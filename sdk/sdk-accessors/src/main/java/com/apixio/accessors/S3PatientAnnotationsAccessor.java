package com.apixio.accessors;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import com.apixio.ensemble.ifc.Signal;
import com.apixio.model.event.AttributeType;
import com.apixio.model.event.EventType;
import com.apixio.model.event.FactType;
import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxEnvironment;
import com.apixio.signals.AnnotationSignal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.apixio.dao.annos.AnnotationsDAO;

public class S3PatientAnnotationsAccessor extends BaseAccessor
{
    AnnotationsDAO annoDao;
    HashMap<String,String> rejectReasonMap;
    HashMap<String,String[]> apxcatmap;
    ObjectMapper objectMapper = new ObjectMapper();
    TypeReference<HashMap<String,HashMap<String, Object>>> typeRef 
        = new TypeReference<HashMap<String,HashMap<String,Object>>>() {};

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        super.setEnvironment(env);

        annoDao = fxEnv.getDaoServices().getPatientAnnotationsDAO();
        
        // Load reject reason mapping
        rejectReasonMap = new HashMap<>();
        InputStream rrStream = this.getClass().getResourceAsStream("/Reject_reasons_20210610.csv");
        CsvMapper csvmapper = new CsvMapper();
        csvmapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        MappingIterator<String[]> it = csvmapper.readerFor(String[].class).readValues(rrStream);
        it.next(); // throw away header
        while (it.hasNext()) {
            String[] row = it.next();
            rejectReasonMap.put(row[1], row[0]);
        }

        // load apxcat codes
        //  TODO instead of loading from jar, we should pull from https://mapping-stg.apixio.com:8443/mapping/v0/code
        InputStream apxcatStream = this.getClass().getResourceAsStream("/mappingcode.json.gz");
        TypeReference<List<HashMap<String,Object>>> listhmRef = new TypeReference<List<HashMap<String,Object>>>() {};
        List<HashMap<String,Object>> apxcatmapraw = objectMapper.readValue(new GZIPInputStream(apxcatStream), listhmRef);
        apxcatmap = new HashMap<>();
        for (HashMap<String,Object> el: apxcatmapraw) {
            // [ {"f":{"c":"0031","s":"2.16.840.1.113883.6.103"},
            //    "t":{"c":"2","s":"HCCCRV1"},
            //    "v":"2016+17-icd-hcccrv1","u":false},...,
            String rawcode = (String) ((HashMap)el.get("f")).get("c");
            String rawcodeSys = (String) ((HashMap)el.get("f")).get("s");
            String apxcatcode = (String) ((HashMap)el.get("t")).get("c");
            String apxcatcodeSys = (String) ((HashMap)el.get("t")).get("s");
            if (!apxcatcodeSys.contains("V24") || apxcatcode.equals("999")) {
                continue;
            }
            String date = (String) el.get("end");
            String raw = rawcode+":"+rawcodeSys;
            String[] existing = apxcatmap.get(raw);
            if (existing == null || (existing[2].compareTo(date) < 0)) {
                apxcatmap.put(raw, new String[]{apxcatcode, apxcatcodeSys, date});
            }
            /* START block of TESTING CODE */
            apxcatmap.put("FAKE:FAKE", new String[]{"111", "HCCV24", "2038-01-01"});
            /* END block of TESTING CODE */
            
        }
    }

    /**
     * Signature:  com.apixio.model.patient.Patient patient(String uuid)
     */
    @Override
    public String getID()
    {
        return "patientannotations";
    }

    static ArrayList<String> ACCEPTED_REJECT_CATS = new ArrayList<>(Arrays.asList(new String[]{"PRO", "INS", "SIG"}));

    @Override
    public Object eval(AccessorContext req, List<Object> args) throws Exception
    {
        List<Signal> allSignals = new ArrayList<Signal>();
        String patientuuid = (String) args.get(0);
        List<EventType> annos = annoDao.getAnnotationsForPatient(UUID.fromString(patientuuid));
        for (EventType anno: annos) {
            AnnotationSignal signal = eventToSignal(anno);
            if (signal != null)
                allSignals.add(signal);

        }
        return allSignals;                
     }


    // This contains all of the conversion nuances between tha kafka annotations and the Signal
    // Emulates and pulls from (a) AnnotationsToLabels.java, (b) https://zeppelin.apixio.com:9443/#/notebook/2FSVVWSH6, and 
    // (c) https://zeppelin.apixio.com:9443/#/notebook/2FTACG1Q6
    private AnnotationSignal eventToSignal(EventType anno) throws JsonMappingException, JsonProcessingException
    {
        String pid = anno.getSubject().getUri();
        String date = null;
        String acceptedCode = null;
        String acceptedCodeSystem = null;
        String code = null;
        for (AttributeType attr: anno.getEvidence().getAttributes().getAttribute()) {
            // if (attr.getName().equals("project")) {
            //     if (!attr.getValue().startsWith("PRHCC"))
            //         return null;
            if (attr.getName().equals("timestamp")) {
                date = attr.getValue().substring(0, 10);
            } else if (attr.getName().equals("acceptedCode")) {
                acceptedCode = attr.getValue();
            } else if (attr.getName().equals("acceptedCodeSystem")) {
                acceptedCodeSystem = attr.getValue();
            } else if (attr.getName().equals("code")) {
                code = attr.getValue();
            }
        }
        // ReferenceType src = anno.getEvidence().getSource();
        // String uri = src.getUri();
        // if (uri.contains("_coder@apixio.com") || uri.contains("_qa@apixio.com") || uri.startsWith("lucy_franklin"))
        //     return null;
        FactType fact = anno.getFact();
        String finalcode = acceptedCode;
        String codeSystem = acceptedCodeSystem;
        if (finalcode == null || finalcode.length() == 0) {
            finalcode = code;
        }
        if (finalcode == null || finalcode.length() == 0) {
            finalcode = fact.getCode().getCode();
            codeSystem = fact.getCode().getCodeSystem();
        }

        List<AttributeType> factattr = fact.getValues().getAttribute();
        String status = "none";
        String rejectReason = null;
        for (AttributeType fa: factattr) {
            if (fa.getName().equals("result"))
                status = fa.getValue();
            else if (fa.getName().equals("rejectReason"))
                rejectReason = fa.getValue();
        }
        if (!status.equals("accept") && !status.equals("reject"))
            return null;
        String sourceName = "anno_" + status;

        String value = codeSystem + ":" + finalcode;
        if (status.equals("reject")) {
            String rejectCategory = rejectReasonMap.get(rejectReason);
            if (rejectCategory == null)
                rejectCategory = "UNK";
            value = rejectCategory + ":" + rejectReason;
            if (!ACCEPTED_REJECT_CATS.contains(rejectCategory))
                return null;
        }
        // value = value + ":" + date;
        String[] apxcatinfo = apxcatmap.get(finalcode + ":" + codeSystem);
        if (apxcatinfo == null) {
            return null;
        }
        String apxcat = apxcatinfo[1].substring(3) + "_" + apxcatinfo[0];
        String preshccmodel = "s3://apixio-science-data/science_datasets/wz/care_analytics/prescription_HCC/cleaned_mapping/prescription_HCC_v3_10-14-2020.csv/part-00000-db79bdab-d95f-496f-8902-389e202b7f80-c000.csv";
        return new AnnotationSignal(pid, apxcat, sourceName, value, preshccmodel, date);

    }
}
