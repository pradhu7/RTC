package com.apixio.sdk.util;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Paths;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.util.UriCacheManager.Option;
import com.apixio.util.UriCacheManager;
import com.apixio.util.UriFetcher;

/**
 * Centralized utility for URI caching setup
 */
public class CacheUtil
{
    /**
     * "Standard" config points via ConfigSet via .yaml.  Expected structure of yaml is
     *
     *  cache:
     *    directory:  ""        # required local file system directory path
     *    options:    ""        # optional; csv list of UriCacheManager.Option enum names
     *
     */
    public static final String CACHE_DIRECTORY  = "cache.directory";
    public static final String CACHE_OPTIONS    = "cache.options";
    
    /**
     * Cache setup info
     */
    public static class CacheInfo
    {
        public String   directory;
        public Option[] options;

        /**
         * Create from non-standard config
         */
        public CacheInfo(String directory, Option... options)
        {
            this.directory = directory;
            this.options   = options;
        }

        /**
         * Create from standard config with optional override for server for convenience.  The "root" of the
         * config set must be at the parent of the "cache" key so that a config.getConfigString("cache") would
         * return a non-null ConfigSet
         */
        public CacheInfo(ConfigSet config, String directoryOverride)
        {
            this(config.getString(CACHE_DIRECTORY, directoryOverride),
                 makeOptions(config.getString(CACHE_OPTIONS, null))
                );
        }

        private static Option[] makeOptions(String csv)
        {
            List<Option> options = new ArrayList<>();

            if (csv != null)
            {
                for (String opt : csv.split(","))
                {
                    if ((opt = opt.trim()).length() > 0)
                        options.add(Option.valueOf(opt.toUpperCase()));
                }
            }

            return options.toArray(new Option[options.size()]);
        }
    }

    /**
     * Make a UriCacheManager with the given cache info and URI fetcher
     */
    public static UriCacheManager makeCacheManager(CacheInfo cacheInfo, UriFetcher fetcher) throws Exception
    {
        return new UriCacheManager(Paths.get(cacheInfo.directory), fetcher, cacheInfo.options);
    }

}
