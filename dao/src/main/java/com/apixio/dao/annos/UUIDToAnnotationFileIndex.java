package com.apixio.dao.annos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import org.apache.commons.io.IOUtils;

import gnu.trove.map.hash.TShortObjectHashMap;

public class UUIDToAnnotationFileIndex {

    public static final String INDEX_FILENAME_END = ".index.gz";

    private final static int LRU_MAX_SIZE = 10000;

    private IndexerConfig config;
    private TShortObjectHashMap<long[][]> patientgroups;
    private AmazonS3 awss3; // used to pull patient index pieces
    private String patientIndexBucket;
    private String indexKeyRoot;

    public UUIDToAnnotationFileIndex(IndexerConfig config, AmazonS3 awss3client){
        this.config = config;
        this.patientgroups = new TShortObjectHashMap<long[][]>();
        this.awss3 = awss3client;
        this.patientIndexBucket = config.getIndexFilesBucket();
        this.indexKeyRoot = config.getIndexFilesKeyPrefix();
    }
    
    // Pulls the index piece for {groupkey} from S3
    // Because indexes are stored as byte arrays, this function must mirror compileIndex exactly
    long[][] retrieveIndexForGroup(short groupkey) throws IOException
    {
        // Poor man's cache management
        if (patientgroups.size() > LRU_MAX_SIZE) 
        {
            patientgroups = new TShortObjectHashMap<long[][]>();
            System.gc();
        }
        String fullkey = indexKeyRoot + groupkey + INDEX_FILENAME_END;
        try {
            // pull index piece and convert to long[][] (outer is each patient, inner is UUID + filelongs)
            //            System.out.println(String.format("SFM awss3.getObject(%s/%s)", patientIndexBucket, fullkey));
            S3Object s3Object = awss3.getObject(patientIndexBucket, fullkey);
            S3ObjectInputStream is = s3Object.getObjectContent();
            long[][] patientmap = extractLonglist(new GZIPInputStream(is));
            is.close();
            return patientmap;
        } catch (Exception e) {
            //            e.printStackTrace();
            System.out.println("Unable to retrieve index.gz at " + patientIndexBucket + "/" + fullkey);
            // This is expected when the index doesn't exist
            return new long[][]{};
        }
    }

    protected static long[][] extractLonglist(InputStream instr) throws IOException
    {
        ByteBuffer bb = ByteBuffer.wrap(IOUtils.toByteArray(instr));
        int numpatients = bb.getInt();
        long[][] patientmap = new long[numpatients][];
        for (int idx = 0; idx < numpatients; idx++)
        {
            int numfiles = bb.getInt();
            long[] files = new long[numfiles];
            for (int idx2 = 0; idx2 < numfiles; idx2++)
            {
                files[idx2] = bb.getLong();
            }
            patientmap[idx] = files;
        }
        return patientmap;

    }
    // Main function from the perspective of the user. Gets the S3 paths that a patient's annotations
    public String[] getFilesForId(UUID id) throws IOException
    {
        // get the patient group id
        short groupkey = IndexUtil.getGroupKey(id);
        // ensure the index for this patient group is pulled

        if (!patientgroups.containsKey(groupkey))
        {
            patientgroups.put(groupkey, retrieveIndexForGroup(groupkey));
        }

        long[][] patientmap = patientgroups.get(groupkey);
        // find this patient in the index, using brute force TODO binary search
        long[] foundfilelongs = null;
        long[] idlongs = IndexUtil.asLongs(id);

        for (int idx = 0; idx < patientmap.length; idx++)
        {
            // a UUID is two longs; the first two longs of the long array are the UUID
            long[] patient = patientmap[idx];

            if (patient[0] != idlongs[0])
                continue;
            if (patient[1] == idlongs[1])
            {
                foundfilelongs = patient;
                break;
            }                
        }
        if (foundfilelongs == null) {
            return new String[0];
        }
        // Convert the file long representation to full s3 keys
        String[] files = new String[foundfilelongs.length-2];
        for (int idx = 2; idx < foundfilelongs.length; idx++)
        {
            long filelong = foundfilelongs[idx];
            files[idx-2] = IndexUtil.long2filepath(config.getTopicObjectTemplate(), filelong);
        }
        return files;
    }

    /*
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // String path = "/users/bpeintner/Downloads/fistore.oos";
        // UUIDToAnnotationFileIndexer fi = new UUIDToAnnotationFileIndexer(1000);
        // // UUIDToAnnotationFileIndexer fi = UUIDToAnnotationFileIndexer.loadForAppend(new FileInputStream(new File(path)));
        // UUID pid = UUID.fromString("945c8dc7-a123-4e45-9ad7-7e3d46ef080c");
        // fi.addMapping(pid, "prod/topics/annotationscience_prd/year=2020/month=02/day=16/annotationscience_prd+0+0010092424.json.gz");
        // fi.addMapping(UUID.randomUUID(), "prod/topics/annotationscience_prd/year=2020/month=02/day=16/annotationscience_prd+0+0010092424.json.gz");
        // fi.addMapping(pid, "prod/topics/annotationscience_prd/year=2020/month=02/day=16/annotationscience_prd+0+0010092425.json.gz");
        // fi.store(new File(path));
        // UUIDToAnnotationFileIndexer.compileIndex(fi.getPatient2files(), "/Users/bpeintner/Downloads/patientindex/v1/");
        ConfigSet daoConfig = ConfigSet.fromYamlFile(new File("/Users/bpeintner/IdeaProjects/apx-sdk/test/staging.yaml"));
        DaoServices daoServices = DaoServicesSet.createDaoServices(daoConfig);
        S3Ops s3ops = daoServices.getS3Ops();
        AmazonS3 awss3 = s3ops.getS3Connector().getAmazonS3();
        UUIDToAnnotationFileIndex fi2 = new UUIDToAnnotationFileIndex(awss3, new String[]{"apixio-science-bucket", "david/fxannotations", "v1"});
        String[] files = fi2.getFilesForId(UUID.fromString("945c8dc7-a123-4e45-9ad7-7e3d46ef080c"));
        //UUID@41 "945c8dc7-a123-4e45-9ad7-7e3d46ef080c"
    }
    */
}
