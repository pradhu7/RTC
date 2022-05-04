package com.apixio.dao.annos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.datasource.s3.S3Ops;
import com.apixio.datasource.s3.S3OpsException;
import com.apixio.restbase.config.ConfigSet;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TLongArrayList;
/**
 * This file is the main class for managing the pragmatic [(c) 2021 David Yee] storage of annotations in S3.
 * 
 * Background:
 *      - CAPV needs annotations on a per-patient basis. Therefore, we need a way to query all annos for a patient.
 *      - Annotations are stored in S3 at arn:aws:s3:::apixio-backups-kafka-topics/prod/topics/annotationscience_prd/
 *          - Organized as groups of json.gz files, one folder per day, e.g., prod/topics/annotationscience_prd/year=2020/month=01/
 *          - Each line of each json.gz is a json representation that is close to EventType/PredictionBin
 *          - Each file contains more than one patient. Typical patient has annos spread over 5 files, but high numbers are possible
 *          - Approximately 10M patients, 1.1M files (and growing), and 500-1000 annos per file, so ~1B annotations
 *      - For the short term, using S3 as annotation storage/retieval mechanism is justified because pushing them to Cassandra would push us past the deadline
 * 
 * Implmentation: Patient Index
 *      - To allow a query of annotations by patient, we need to create a map from patient id to the list of files that contain her annos.
 *      - This index needs to be updated daily and its users need to have access to the latest index
 *          - therefore, storing inside jars, e.g., does not work; has to be pulled from storage or accessed via api
 *      - Initial plan is store index in pieces so that (a) can be pulled incrementally and (b) memory can be limited
 *      - The index is one block per patient: each block is a list of longs, where the first two longs are the patient id and each of the remaining longs point to a single anno file
 *      - To see how a long points to a file on s3, see filepath2long and long2filepath
 *      - We break the index into 512 pieces each labeled by a groupkey
 *      - The groupkey is the last 9 bits of the patient id
 *  
 * Implmentation: Using the index
 *      - To use the index and query for annos, instantiate UUIDToAnnotationFileIndex
 *      - UUIDToAnnotationFileIndex.getFilesForId accepts a patient id and returns a list of s3 paths
 *      - UUIDToAnnotationFileIndex starts with an empty index. For each new patient id, it retrieves the index piece associated with that patient's group id
 *      - Each index piece is kept in memory until the cache mechanism kicks it out. So, later patients with same group id will not need to pull index piece from S3
 * 
 * Implementation: Building/updating the index
 *      - Here's the flow for updating an index for a single day (addS3AnnotationFolderToS3Index)
 *          - Download all new annotation files in the folder for that day
 *          - Process all files and build an uncompiled index (HashMap<UUID, TLongArrayList>)
 *          - Compile the index into small files using 'compileIndex' (saves on disk)
 *          - Merge this new, small index into the full index using `mergeIndexToS3`
 *      - For building initial index
 *          - Building the full index using the one-day flow is too slow, since it compiles and merges unnecessarily
 *          - If you have the folder downloaded, you can use `addFolderToPatientIndex` to add a folder to an instantiated `UUIDToAnnotationFileIndexer`
 *          - If you want to build multiple day by pulling from s3, use multiple calls to `addS3AnnotationFolderToIndexer`
 *          - Not recommeneded to build the full dataset at once: 
 *              - best to do one month at a time and compileIndex to a local folder for each
 *              - then, use `mergeIndexToS3`to merge each one at a time.
 * 
 * Notes
 *      - `mergeIndexToS3` is idempotent
 *      - `addS3AnnotationFolderToIndexer` accepts a pointer to a csv logfile.
 *          - When it's done, it adds an entry showing the day added and the number of files
 *          - For the 'run in production forever' mode, this will tell you if it missed any days
 *      
 */
public class UUIDToAnnotationFileIndexer {
    static ObjectMapper objectMapper = new ObjectMapper();
    static TypeReference<Map<String,Map<String, Object>>> typeRef 
        = new TypeReference<Map<String,Map<String,Object>>>() {};

    static
    {
        // total hack to reduce the spewage from httpclient wire logging
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http.wire")).setLevel(ch.qos.logback.classic.Level.WARN);
    }

    private Map<UUID, TLongArrayList> patient2files; // maps from patient id to a list of longs that represent files
    private IndexerConfig config;
    private AmazonS3 awss3;

    public UUIDToAnnotationFileIndexer(IndexerConfig config, AmazonS3 awss3)
    {
        this.config        = config;
        this.awss3         = awss3;
        this.patient2files = new HashMap<UUID, TLongArrayList>();
    }

    private Map<UUID, TLongArrayList> getPatient2files() {
        return patient2files;
    }

    // Used at 'build index time'
    private void addMapping(UUID id, String filepath)
    {
        System.out.println("addMapping for PatientUUID " + id + " to path " + filepath + " groupkey=" + IndexUtil.getGroupKey(id));

        // get a long representation of the filename
        long filelong = IndexUtil.filepath2long(filepath);
        // ensure that there is an arraylist associated with this patient
        if (!patient2files.containsKey(id))
        {
            patient2files.put(id, new TLongArrayList(2));
        }
        TLongArrayList files = patient2files.get(id);
        // add this file to this patient
        files.add(filelong);
    }

    // static function that saves patient2files in an efficient bytearray, organized by 'patient groups' (i.e., groupkey)
    private static void compileIndex(Map<UUID, TLongArrayList> patient2files, String root) throws FileNotFoundException, IOException
    {
        Map<Short,List<byte[]>> groups = new HashMap<Short,List<byte[]>>();
        for (Entry<UUID, TLongArrayList> e: patient2files.entrySet())
        {
            // every patient is represented as a sequence of longs. First two are UUID; remaining are files
            TLongArrayList files = e.getValue();
            // remove duplicates
            Set<Long> set = new HashSet<Long>();
            TLongIterator fit = files.iterator();
            while (fit.hasNext()) {
                set.add(fit.next());
            }
            files.clear();
            files.addAll(set);
            long[] patientvec = new long[files.size()+2];
            long[] idlongs = IndexUtil.asLongs(e.getKey());
            // Add UUID to front
            System.arraycopy(idlongs, 0, patientvec, 0, 2);
            TLongIterator it = files.iterator();
            int idx = 2;
            while(it.hasNext()) {
                patientvec[idx++] = it.next();
            }
            ByteBuffer bb = ByteBuffer.allocate(patientvec.length * 8);

            for (long el: patientvec) 
                bb.putLong(el);

            // gather all patients in the same group together
            short groupkey = IndexUtil.getGroupKey(e.getKey());
            List<byte[]> grouplist = groups.get(groupkey);
            if (grouplist == null)
            {
                grouplist = new ArrayList<byte[]>();
                groups.put(groupkey, grouplist);
            }
            grouplist.add(bb.array());
        }
        // write out each patient group in its own file. For now, save to file system and 
        // assume an external process will push to s3.
        for (Entry<Short, List<byte[]>> e: groups.entrySet())
        {
            List<byte[]> filebytes = e.getValue();
            writeGroup(filebytes, root, e.getKey());
        }
    }

    // actual layout of bytes in file:
    //  1.  number of items in List<>, as an int
    //  2.  loop on items in List<>:
    //      a) number of longs in byte[], as an int
    //      b) byte[] contents
    private static void writeGroup(List<byte[]> filebytes, String root, short groupkey) throws IOException
    {
        int capacity = 4;                 // 4=sizeof(int); for 1. above
        for (byte[] file: filebytes)
        {
            capacity += file.length + 4;  // sizeof(int); for 2.a above
        }
        ByteBuffer groupbuffer = ByteBuffer.allocate(capacity);
        groupbuffer.putInt(filebytes.size());
        for (byte[] file: filebytes)
        {
            groupbuffer.putInt((file.length)/8); // /8 to get #longs
            groupbuffer.put(file);
        }
        String filename = groupkey + UUIDToAnnotationFileIndex.INDEX_FILENAME_END;
        File filepath = Paths.get(root, filename).toFile();
        FileOutputStream fos = new FileOutputStream(filepath);
        GZIPOutputStream gos = new GZIPOutputStream(fos);
        IOUtils.write(groupbuffer.array(), gos);
        gos.close();
        fos.close();
    }
    // load this index-building version of the data. This is saved as java objects and is different than the compiled index,
    // which is loaded incrementally during runtime
    /*
    private static UUIDToAnnotationFileIndexer loadForAppend(InputStream path) throws FileNotFoundException, IOException, ClassNotFoundException
    {
        UUIDToAnnotationFileIndexer  fi = new UUIDToAnnotationFileIndexer();
        ObjectInputStream ois = new ObjectInputStream(path);
        fi.patient2files = (Map<UUID, TLongArrayList>) ois.readObject();
        ois.close();
        System.gc();
        return fi;
    }
    */

    // Store the appendable version of the data in java objects. This is different than the compiled index, which is generated 
    // using compileIndex
    /*
    private void store(File path) throws IOException
    {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
        oos.writeObject(patient2files);
        oos.close();
     }
    */

    private void addMappingsForFile(String jsonline, String fileuri) 
    {
        try {
            Map<String, Map<String, Object>> jsonobj = objectMapper.readValue(jsonline, typeRef);
            String pid = (String) jsonobj.get("subject").get("uri");
            if (!jsonline.contains("PRHCC"))
                return;
            if (jsonline.contains("_coder@apixio.com") || jsonline.contains("_qa@apixio.com")|| jsonline.contains("lucy_franklin"))
                return;
            Map<String, Object> fact = jsonobj.get("fact");
            String status = ((Map<String, String>) fact.get("values")).get("result");
            if (!status.equals("accept") && !status.equals("reject"))
                return;

            addMapping(UUID.fromString(pid), fileuri);

        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
    /*
    private void addFolderToPatientIndex(String root)
    {
        int count = 0;
        int maxfiles = 0;
        for(String onedir: new File(root).list())   
        {
            if (onedir.charAt(0) != 'd')
                continue;
            Path dirPath = Paths.get(root, onedir);
            for(String onefile: new File(dirPath.toString()).list())   
            {
                Path filePath = Paths.get(dirPath.toString(), onefile);
                String key = filePath.toString().split("onemonthprod/")[1];
                for (String jsonline: IndexUtil.readLinesFromGzStream(filePath.toString()))
                {
                    addMappingsForFile(jsonline, key);
                }
            }   
        }
        System.out.println(count + " " + maxfiles);
    }
    */

    // Pulls an existing index from s3, merges it with new info, then re-stores it in s3
    // ALSO OVERWRITES EXISTING INDEX ON LOCAL DISK
    private void mergeIndexToS3(File indexRoot) throws IOException
    {
        UUIDToAnnotationFileIndex indx = new UUIDToAnnotationFileIndex(config, awss3);
        for (File fle: indexRoot.listFiles((dir, filename) -> filename.endsWith(UUIDToAnnotationFileIndex.INDEX_FILENAME_END))) {
            String key = config.getIndexFilesKeyPrefix() + fle.getName();
            short groupkey = Short.parseShort(fle.getName().split("\\.")[0]);
            long[][] stored = indx.retrieveIndexForGroup(groupkey);
            long[][] newidx = UUIDToAnnotationFileIndex.extractLonglist(new GZIPInputStream(new FileInputStream(fle)));
            long[][] merged = merge2Indexes(stored, newidx);
            List<byte[]> filebytes = new ArrayList<byte[]>();

            for(long[] patientvec: merged) {
                ByteBuffer bb = ByteBuffer.allocate(patientvec.length * 8);
                for (long el: patientvec) 
                    bb.putLong(el);
                filebytes.add(bb.array());
            }

            writeGroup(filebytes, indexRoot.toString(), groupkey);

            //            System.out.println(String.format("SFM awss3.putObject(%s/%s)", config.getIndexFilesBucket(), key));
            awss3.putObject(config.getIndexFilesBucket(), key, fle);
        }
    }

    private long[][] merge2Indexes(long[][] one, long[][] two)
    {
        Map<UUID, long[]> all = new HashMap<>();

        for (long[] pat : one)
            all.put(new UUID(pat[0], pat[1]), pat);  // note that we're still keeping patID in [0],[1]

        for (long[] pat : two)
        {
            UUID uuid = new UUID(pat[0], pat[1]);
            long[] onepat = all.get(uuid);

            if (onepat != null)
            {
                Set<Long> set = new HashSet<>();
                long[]    newpat;

                // add both lists of filelongs to Set<> (skip patID that's in [0],[1] in both lists)

                for (int idx = 2; idx < onepat.length; idx++)
                    set.add(onepat[idx]);

                for (int idx = 2; idx < pat.length; idx++)
                    set.add(pat[idx]);

                newpat = new long[set.size() + 2];  // +2 for patID

                // copy patID
                newpat[0] = pat[0];
                newpat[1] = pat[1];

                int sidx = 2;

                for (long el : set)
                    newpat[sidx++] = el;

                pat = newpat;
            }

            all.put(uuid, pat);
        }

        long[][] merged = new long[all.size()][];
        int idx = 0;

        for(long[] fnl : all.values())
        {
            merged[idx] = fnl;
            idx++;
        }

        return merged;
    }

    private void mergeIndexFolders(File indexRoot, File outputLoc) throws IOException
    {
        int numIndexFiles = (1 << IndexUtil.keybits);
        // UUIDToAnnotationFileIndex indx = new UUIDToAnnotationFileIndex(config, awss3);
        for (int groupId = 0; groupId < numIndexFiles; groupId++) {
            long[][] merged = new long[][]{};
            for (File fle: indexRoot.listFiles()) {
                File idxFile = new File(fle, groupId + UUIDToAnnotationFileIndex.INDEX_FILENAME_END);
                if (idxFile.exists()) {
                    long[][] newidx = UUIDToAnnotationFileIndex.extractLonglist(new GZIPInputStream(new FileInputStream(idxFile)));
                    merged = merge2Indexes(merged, newidx);
                }
            }
            List<byte[]> filebytes = new ArrayList<byte[]>();
            for(long[] patientvec: merged) {
                ByteBuffer bb = ByteBuffer.allocate(patientvec.length * 8);
                for (long el: patientvec) 
                    bb.putLong(el);
                filebytes.add(bb.array());
            }
            writeGroup(filebytes, outputLoc.toString(), (short) groupId);

        }
    }


    private int addS3AnnotationFolderToIndexer(LocalDate dateToAdd, File logfile) throws IOException
    {
        // Construct folder path
        String day = String.format("%02d", dateToAdd.getDayOfMonth());
        String month = String.format("%02d", dateToAdd.getMonthValue());
        String year = String.format("%d", dateToAdd.getYear());
        String folder = String.format(config.getTopicFolderTemplate(), year, month, day);

        // Iterate through all files and process them
        int filesadded = 0;
        //        System.out.println(String.format("SFM awss3.listObjects(%s/%s)", config.getKafkaTopicsBucket(), folder));
        ObjectListing objs = awss3.listObjects(config.getKafkaTopicsBucket(), folder);
        List<S3ObjectSummary> keyList = new ArrayList<S3ObjectSummary>();
        keyList.addAll(objs.getObjectSummaries());

        while (objs.isTruncated()) {
            objs = awss3.listNextBatchOfObjects(objs);
            keyList.addAll(objs.getObjectSummaries());
        }
        for (S3ObjectSummary os: keyList)
        {
            filesadded = filesadded + 1;
            try {
                //                System.out.println(String.format("SFM awss3.getObject(%s/%s)", os.getBucketName(), os.getKey()));
                S3Object s3Object = awss3.getObject(os.getBucketName(), os.getKey());
                S3ObjectInputStream is = s3Object.getObjectContent();

                for (String jsonline : IndexUtil.readLinesFromGzStream(is))
                    addMappingsForFile(jsonline, os.getKey());

                is.close();
            } catch (S3OpsException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } 
        }

        // Keep a log at {pathToIndexer}.log
        List<String> loglines = logfile.exists() ? IOUtils.readLines(new FileInputStream(logfile), StandardCharsets.UTF_8) : new ArrayList<String>();
        loglines.add(String.format("Add,%s,%s,%s,%s,%d", year, month, day, new SimpleDateFormat("yyyy-MM-dd").format(new Date()), filesadded));
        FileOutputStream lfos = new FileOutputStream(logfile);
        IOUtils.writeLines(loglines, "\n", lfos, StandardCharsets.UTF_8);
        lfos.close();
        
        return filesadded;
    }


    /**
     * Methods that are the highest level of semantic operations on the indexer
     */
    public int addS3AnnotationFolderToS3Index(LocalDate dateToAdd, File logFile) throws FileNotFoundException, ClassNotFoundException, IOException
    {
        // logFile   File to hold a record of which folders have been processed
        // s3bucket  originally apx-science-data
        // s3Prefix  should be the folder that contains 'patientindex', no end slash, eg. david/fxannotations
        // version   initially v1

        //        System.out.println("SFMSFM indexing... local index dir is " + config.getLocalIndexDirectory());

        int filesadded = addS3AnnotationFolderToIndexer(dateToAdd, logFile);

        // Save the index pieces to the output folder
        String tmpfolder = config.getLocalIndexDirectory();
        File tmpfile = new File(tmpfolder);
        tmpfile.mkdirs();
        compileIndex(getPatient2files(), tmpfolder);

        // Merge index pieces to S3
        mergeIndexToS3(tmpfile);
        return filesadded;
    }

    // month is 1-based
    public String createIndexForMonth(File root, int year, int month, File logfile) throws IOException {
        LocalDate date = LocalDate.of(year, month, 1);

        for (int day = 1, lastDay = date.lengthOfMonth(); day <= lastDay; day++) {
            addS3AnnotationFolderToIndexer(LocalDate.of(year, month, day), logfile);
        }

        String indexPath = Paths.get(root.toString(), String.format("%d-%d_index", year, (month - 1))).toString();
        new File(indexPath).mkdirs();
        compileIndex(getPatient2files(), indexPath);

        return indexPath;
    }

    public static void dumpIndexFile(String topicObjectTemplate, File path) throws Exception
    {
        try (FileInputStream fis = new FileInputStream(path);
             GZIPInputStream gis = new GZIPInputStream(fis))
        {
            long[][] index = UUIDToAnnotationFileIndex.extractLonglist(gis);

            for (long[] patientFilelongs : index)
            {
                System.out.println("For patientUUID " + (new UUID(patientFilelongs[0], patientFilelongs[1])));

                for (int idx = 2; idx < patientFilelongs.length; idx++)
                    System.out.println("  " + IndexUtil.long2filepath(topicObjectTemplate, patientFilelongs[idx]));
            }
        }
    }

    private String dumpPatFiles(UUID pat, long[] files)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(pat.toString());
        sb.append("; files=");

        for (int idx = 2; idx < files.length; idx++)
            sb.append(IndexUtil.long2filepath(config.getTopicObjectTemplate(), files[idx]) + "; ");

        return sb.toString();
    }

    // This main is only for testing at this point
    public static void main(String[] args) throws Exception {

        IndexerConfig config = new IndexerConfig("apixio-backups-kafka-topics",
                                                 "prod/topics/annotationscience_prd/year=%s/month=%s/day=%s",   // must be prefix of next param:
                                                 "prod/topics/annotationscience_prd/year=%s/month=%s/day=%s/annotationscience_prd+%s+%010d.json.gz",
                                                 "apixio-science-data", "david/fxannotations/patientindex/v1",
                                                 "/tmp/patientindex/v1", "/tmp/PatientIndex.log.csv");

        if (args.length < 1)
            throw new IllegalArgumentException("Usage:  UUIDToAnnotationFileIndexer {config.yaml}");

        ConfigSet daoConfig = ConfigSet.fromYamlFile(new File(args[0]));
        File logFile = new File("/tmp/PatientIndex.log.csv");
        DaoServices daoServices = DaoServicesSet.createDaoServices(daoConfig);
        S3Ops s3ops = daoServices.getS3Ops();
        UUIDToAnnotationFileIndexer indexer;

        switch (2)
        {
            case 1:
            {
                AnnotationsDAO annosDAO = daoServices.getPatientAnnotationsDAO();

                annosDAO.getAnnotationsForPatient(UUID.fromString("0007bda6-aee2-48fa-802e-bb68b95af2fb"));

                break;
            }
            case 2:
            {
                AnnotationsDAO annosDAO = daoServices.getPatientAnnotationsDAO();

                annosDAO.addS3KafkaTopicBackupToIndex(LocalDate.of(2020, 12, 15));
                break;
            }
            case 3:
            {
                indexer = new UUIDToAnnotationFileIndexer(config, s3ops.getS3Connector().getAmazonS3());

                // Test:  this is a daily update for 2020-12-13
                indexer.addS3AnnotationFolderToS3Index(LocalDate.of(2020,12,13), logFile);
                break;
            }
            case 4:
            {
                indexer = new UUIDToAnnotationFileIndexer(config, s3ops.getS3Connector().getAmazonS3());
                
                File localwriteable = new File("/tmp/bulkindexes");
                // Test:  this creates index for entire month
                long start = System.currentTimeMillis();
                indexer.createIndexForMonth(localwriteable, Integer.parseInt(args[1]), Integer.parseInt(args[2]), logFile);
                long end = System.currentTimeMillis();
                System.out.println(String.format("\ntotal %d", end - start));
                break;
            }
            case 5:
            {
                indexer = new UUIDToAnnotationFileIndexer(config, s3ops.getS3Connector().getAmazonS3());
                indexer.mergeIndexFolders(new File("/tmp/bulkindexes"), new File("/tmp/fullindex"));
            }
        }

        System.exit(0);

    }
}
