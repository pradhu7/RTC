package com.apixio.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.restbase.util.ConversionUtil;

/**
 * Provides caching of data files whose source is identified by a URI.  The actual fetching of data
 * for the URIs is handled by the UriFetcher implementation so the exact set of URI schemes that
 * this manager supports is determined by that.
 *
 * This code assumes immutable URI objects although it can optionally (TODO!) check that the documentId
 * timestamps and file sizes haven't changed since last cache (this check obviously slows down the
 * caching).
 * 
 * Configuration consists of supplying a tmp directory (as a java.nio.Path object), and flags for
 * runtime behavior for things like when/if to check for stale objects (this would be for testing only
 * as the main working assumption is that documentId objects are immutable)
 * 
 * Actual caching of URI objects is done by MD5-hashing of the URI and then this hash is used as the
 * base filename in the tmp dir.  Once the object is pulled without error from the URI, a {hash}.info
 * file is created and its contents are a JSON object like:
 *
 * { "uri": "...original s3 object path...", "size": nnn, ... }
 *
 * Java synchronization is done at the level of the URI object as it's anticipated that a number of
 * threads will attempt to initialize with the same set of URI objects pretty much all at the same
 * time.
 */
public class UriCacheManager
{

    private static final Logger LOG = LoggerFactory.getLogger(UriCacheManager.class);

    private static final int LOCK_RETRIES = 6;

    private static ObjectMapper mapper = new ObjectMapper();

    private Path       tmpDir;
    private UriFetcher fetcher;

    /**
     * for synchronizing on actual file download
     */
    private Map<String, Object> syncs = new HashMap<>();

    private boolean verbose;

    /**
     * "NONE" only to make client not have to have 2 calls to ctor;
     * future:  CLEAR_CACHE, CHECK_SIZE
     */
    public enum Option { NONE, VERBOSE, NO_MKDIR }

    /**
     * Metadata about a locally cached URI object
     */
    public static class ObjectInfo
    {
        private String originalUri;
        private Path   localPath;
        private long   size;
        private String md5Hash;

        public String getOrignalUri()
        {
            return originalUri;
        }
        public Path getPath()
        {
            return localPath;
        }
        public long getSize()
        {
            return size;
        }
        public String getMd5Hash()
        {
            return md5Hash;
        }

        public ObjectInfo(String originalUri, Path localPath, long size, String md5Hash)
        {
            this.originalUri = originalUri;
            this.localPath   = localPath;
            this.size        = size;
            this.md5Hash     = md5Hash;
        }

        @Override
        public String toString()
        {
            return ("URI Info: originUri=" + originalUri +
                    ", localPath=" + localPath +
                    ", size= " + size +
                    ", hash=" + md5Hash);
        }
    }

    /**
     * Internal only class that holds URI object metadata and that is stored in "info" files
     * that are companion files to the main data file.
     */
    static class ObjectMeta
    {
        public String uri;
        public long   size;
        public String md5;

        // for objectMapper
        public ObjectMeta()
        {
        }

        // convenience only
        public ObjectMeta(String uri, long size, String md5)
        {
            this.uri  = uri;
            this.size = size;
            this.md5  = md5;
        }
    }

    /**
     * Parts of filenames
     */
    private final static String INFO_FILE  = ".info";

    /**
     *
     */
    public UriCacheManager(Path tmpDir, UriFetcher fetcher, Option... options) throws IOException
    {
        boolean exists;

        if (tmpDir == null)
            throw new IllegalArgumentException("tmpDir must be non-null");

        verbose = hasOption(Option.VERBOSE, options);
        exists  = Files.exists(tmpDir);

        if (!exists && !hasOption(Option.NO_MKDIR, options))
        {
            if (verbose)
                LOG.info("Creating local directory for S3 object cache " + tmpDir);

            Files.createDirectories(tmpDir);
        }

        if (!Files.isDirectory(tmpDir) || !Files.isWritable(tmpDir))
            throw new IllegalArgumentException("Local S3 object cache path must be a writeable directory:  " + tmpDir);

        this.fetcher = fetcher;
        this.tmpDir  = tmpDir;
    }

    /**
     * Return a java.nio.file.Path object to a local file that contains the contents of the given
     * URI, caching as necessary.
     */
    public ObjectInfo getLocalObjectInfo(URI uri)
    {
        try
        {
            Object      sync  = getSyncLock(uri);
            InputStream is;
            ObjectMeta  info;
            Path        base;

            synchronized (sync)
            {
                if ((is = readFromCache(uri)) == null)
                    copyToCache(uri);
                else
                    is.close();
            }

            base = getBase(uri);
            info = readInfoFile(getInfoFile(base));

            return new ObjectInfo(info.uri, base, info.size, info.md5);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public InputStream getObject(URI uri)
    {
        try
        {
            Object      sync  = getSyncLock(uri);
            InputStream is;

            synchronized (sync)
            {
                if ((is = readFromCache(uri)) == null)
                {
                    copyToCache(uri);
                    is = readFromCache(uri);
                }

                return is;
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the single Object instance for a given S3 path that's used to synchronize the actual
     * cache file for the path.
     */
    synchronized
    private Object getSyncLock(URI uri)
    {
        String canonical = uri.toString();
        Object lock      = syncs.get(canonical);

        if (lock == null)
        {
            lock = new Object();
            syncs.put(canonical, lock);
        }

        return lock;
    }

    /**
     * Returns an InputStream to the local copy of the S3 object from cache, null if the S3 object
     * has no local file.
     */
    private InputStream readFromCache(URI uri) throws IOException
    {
        Path base = getBase(uri);

        if (Files.exists(getInfoFile(base)))
        {
            if (verbose)
                LOG.info("Using locally cached object at local path " + base + " for URI " + uri);

            return Files.newInputStream(base, StandardOpenOption.READ);
        }
        else
        {
            return null;
        }
    }

    /**
     * Copies the contents of the given S3 object to cache and on success creates the .info
     * companion file
     */
    private void copyToCache(URI uri) throws IOException
    {
        Path    base      = getBase(uri);
        Path    infoFile  = getInfoFile(base);
        boolean succeeded = false;
        int     tries     = 0;

        if (verbose)
            LOG.info("Caching URI object " + uri + " to local path " + base);

        while (!Files.exists(infoFile) && (tries < LOCK_RETRIES))
        {
            try (InputStream          src       = fetcher.fetchUri(uri);
                 Md5DigestInputStream md5Stream = Md5DigestInputStream.wrapInputStream(src))  // luckly .close() isn't required (but is slower...)
            {
                try (FileOutputStream fileOutputStream = new FileOutputStream(base.toFile()))
                {
                    try (FileLock lock = fileOutputStream.getChannel().tryLock())
                    {
                        if (lock != null)
                        {
                            Files.copy(md5Stream, base, StandardCopyOption.REPLACE_EXISTING);
                            createInfoFile(Files.size(base), getInfoFile(base),
                                           uri.toString(), md5Stream.getMd5AsHex());
                            succeeded = true;
                        }
                        else
                        {
                            if (verbose)
                                LOG.info("Failed to lock URI cache key " + uri + " on retry " + tries);

                            tries += 1;
                        }
                    }
                }
            }

            // We sleep outside of the try blocks to release any file handles while waiting.
            if (!succeeded)
                safeSleep(uri, 10000);
        }

        if (!Files.exists(getInfoFile(base)))
            throw new IllegalStateException("Failed to cache URI " + uri + " after " + tries + " retries");
    }

    /**
     * Hash S3 bucket/key to get MD5 hash that's the filename in the tmpdir that will contain cached
     * content.  Note that this is CASE-SENSITIVE for the entire URI (e.g., S3 bucket+key).
     *
     * The file extension of the returned Path MUST be the same as what's in the URI (the "path" part
     * of the URI) as there can be (is currently) code that is sensitive to the file extension.
     */
    Path getBase(URI uri)
    {
        String path = uri.getPath();
        int    dot  = path.lastIndexOf('.');

        if ((dot != -1) && (dot + 1 < path.length()))
            path = "." + path.substring(dot + 1);
        else
            path = "";

        return tmpDir.resolve(md5Hash(uri.toString()) + path);
    }

    /**
     * Create info file that's sibling to cached content file.
     */
    private Path getInfoFile(Path base)
    {
        return base.resolveSibling(base.getFileName() + INFO_FILE);
    }

    private void createInfoFile(long size, Path infoPath, String originalUri, String md5Hash) throws IOException
    {
        try (OutputStream os = Files.newOutputStream(infoPath, StandardOpenOption.CREATE))
        {
            String json = mapper.writeValueAsString(new ObjectMeta(originalUri, size, md5Hash));

            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Read back JSON file to get metadata
     */
    private ObjectMeta readInfoFile(Path infoPath) throws IOException
    {
        String meta = new String(Files.readAllBytes(infoPath), StandardCharsets.UTF_8);

        return mapper.readValue(meta, ObjectMeta.class);
    }

    private String md5Hash(String s)
    {
        MessageDigest md = DigestUtils.getDigest(MessageDigestAlgorithms.MD5);

        md.update(s.getBytes(StandardCharsets.UTF_8));

        return ConversionUtil.toHex(md.digest());
    }

    private boolean hasOption(Option opt, Option[] options)
    {
        // overkill to use streaming
        for (Option option : options)
        {
            if (opt == option)
                return true;
        }

        return false;
    }

    /**
     *
     */
    private static void safeSleep(URI uri, long ms)
    {
        try
        {
            Thread.currentThread().sleep(ms);
        }
        catch (InterruptedException ix)
        {
            LOG.warn("Interrupted while sleeping in UriCacheManager on URI " + uri);
        }
    }

}
