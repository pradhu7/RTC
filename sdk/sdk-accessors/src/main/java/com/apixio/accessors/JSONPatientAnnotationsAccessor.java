package com.apixio.accessors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.apixio.ensemble.impl.common.Generator;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.apixio.datasource.s3.S3Ops;
import com.apixio.ensemble.ifc.Signal;
import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxEnvironment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.apixio.signals.AnnotationSignal;

public class JSONPatientAnnotationsAccessor extends BaseAccessor
{
    HashMap<String,List<String>> patientId2AnnotationFile = null;
    AmazonS3 awss3 = null;
    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        super.setEnvironment(env);

        S3Ops s3ops = fxEnv.getDaoServices().getS3Ops();
        awss3 = s3ops.getS3Connector().getAmazonS3();
    }

    public void loadPatientIdMap(String resourcepath) throws IOException 
    {
        S3Object s3Object = awss3.getObject("apixio-science-data", resourcepath);
        S3ObjectInputStream is = s3Object.getObjectContent();
        InputStream gzipStream = new GZIPInputStream(is);
        Reader decoder = new InputStreamReader(gzipStream, "utf-8");
        TypeReference<HashMap<String,List<String>>> typeRef 
            = new TypeReference<HashMap<String,List<String>>>() {};
        patientId2AnnotationFile = new ObjectMapper().readValue(decoder, typeRef);
        System.out.println("here");
    }
    /**
     * Signature:  com.apixio.model.patient.Patient patient(String uuid)
     */
    @Override
    public String getID()
    {
        return "jsonpatientannotations";
    }

    @Override
    public Object eval(AccessorContext req, List<Object> args) throws Exception
    {
        String path = (String) args.get(0);
        if (path.endsWith(".json")) {
            try (Stream<String> stream = Files.lines( Paths.get(path), StandardCharsets.UTF_8)) 
            {
                List<Signal> allSignals = stream.map(s -> {
                    try {
                        return annoJsonToSignal(s);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList());
                return allSignals;
            }
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        } else // This case is where a patientuuid is passed in
        { 
            if (patientId2AnnotationFile == null) {
                loadPatientIdMap(path);
            }
            String keyroot = new File(path).getParent();
            List<Signal> allSignals = new ArrayList<Signal>();
            String patientuuid = (String) args.get(1);
            List<String> s3uris = patientId2AnnotationFile.get(patientuuid);
            if (s3uris == null)
                return allSignals;
            for (String s3uri: s3uris) {
                String fullkey = keyroot + "/" + s3uri + ".json.gz";
                S3Object s3Object = awss3.getObject("apixio-science-data", fullkey);
                S3ObjectInputStream is = s3Object.getObjectContent();
                InputStream gzipStream = new GZIPInputStream(is);
                Reader decoder = new InputStreamReader(gzipStream, "utf-8");
                TypeReference<List<HashMap<String,Object>>> typeRef 
                    = new TypeReference<List<HashMap<String,Object>>>() {};
                List<HashMap<String,Object>> annos = new ObjectMapper().readValue(decoder, typeRef);
                for (HashMap<String,Object> r: annos) {
                    if (!r.get("patientId").equals(patientuuid))
                        continue;
                    if (r.get("sourceName").equals("anno_accept") || r.get("sourceName").equals("anno_reject") )
                    {
                        allSignals.add(new AnnotationSignal(
                            (String) r.get("patientId"), (String) r.get("code"), 
                            (String) r.get("sourceName"), (String) r.get("value"),
                            (String) r.get("modelPath"), (String) r.get("date")));
                    }
                }
                return allSignals;                
            }
        }
 
        return null;
    }
    
    protected Signal annoJsonToSignal(String jsonstr) throws JsonMappingException, JsonProcessingException
    {
        HashMap r = new ObjectMapper().readValue(jsonstr, HashMap.class);
        // example: {"patientId":"0ddd6fe1-7703-43a8-8cc7-392e43296c4a","code":"V24_9","sourceName":"anno_accept","value":"2.16.840.1.113883.6.90:C169","score":1,"modelPath":"s3a://apixio-science-data/science_datasets/wz/labels/fromAnnotationsToLabelsJob/jobName=from*Platform_09_21_2020","date":"2018-12-17"}

        return new AnnotationSignal(
            (String) r.get("patientId"), (String) r.get("code"), 
            (String) r.get("sourceName"), (String) r.get("value"),
            (String) r.get("modelPath"), (String) r.get("date"));
    }
}
