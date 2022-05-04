package com.apixio.restbase.apiacl.perm.exts;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.Extractor;

public class JsonPathExtractor implements Extractor {

    private ObjectMapper objectMapper = new ObjectMapper();
    private String       path;

    /**
     * Initializes the extractor with the given configuration.  Interpretation of config
     * is entirely up to the type of extractor.
     */
    @Override
    public void init(InitInfo initInfo, ApiDef api, String config)
    {
        this.path = config;
    }

    /**
     * Returns true if the only way for the extractor to return information is by examining
     * the HTTP entity body.
     */
    @Override
    public boolean requiresHttpEntity()
    {
        return true;
    }

    @Override
    public Object extract(CheckContext ctx, MatchResults match, HttpRequestInfo info, Object prevInChain, Object httpEntity)
    {
        Object field;

        if ((httpEntity instanceof String) && MediaType.APPLICATION_JSON.equals(info.getContentType()))
        {
            // since we know the supplied entity is declared to be JSON but all we have is a String,
            // let's parse here to see if we can extract the requested value
            try
            {
                //                System.out.println("SFM: JsonPathExtractor.extract entity is string but want to extract " + path + " so converting to JsonNode tree");
                httpEntity = objectMapper.readTree((String) httpEntity);
            }
            catch (Exception x)
            {
                x.printStackTrace();
            }
        }
            
        if (httpEntity instanceof JsonNode)
            field = jacksonExtract(((JsonNode) httpEntity), path);
        else
            field = PojoWalker.getObjectField(httpEntity, path);

        ctx.recordDetail("JsonPathExtractor.extract({}) returning {}", path, field);

        return field;
    }

    private static Object jacksonExtract(JsonNode obj, String dottedPath)
    {
        //        System.out.println("SFM:  JsonPathExtractor.jacksonExtract(path=" + dottedPath + ", obj=" + obj + ")");

        if (obj == null)
            throw new IllegalArgumentException("Root object is null");
        else if ((dottedPath == null) || ((dottedPath = dottedPath.trim()).length() == 0))
            throw new IllegalArgumentException("Dotted path is null or empty");

        String origDottedPath = dottedPath;

        while (true)
        {
            int  dot = dottedPath.indexOf('.');

            if (dot == -1)
            {
                JsonNode field = obj.findValue(dottedPath);

                if (field == null)
                    return null;
                else
                    return field.asText();
            }
            else if ((dot > 0) && (dot + 1 < dottedPath.length()))
            {
                String   ele   = dottedPath.substring(0, dot);

                obj = obj.findValue(ele);

                if (obj == null)
                    throw new IllegalArgumentException("JsonNode chain had interior null field value.  dottedPath=\"" + origDottedPath +
                                                           "\"; null field value at \"" + ele + "\"");

                dottedPath = dottedPath.substring(dot + 1);
            }
            else
            {
                throw new IllegalArgumentException("Invalid dotted path:  leading '.' or final '.' or '..' encountered:  " + origDottedPath);
            }
        }
    }

    /**
     * Debug only
    private static final String JSON = "{\"attributes\": {\"HCC_APP_VERSION\": \"5.4.0\", \"SOURCE_TYPE\": \"USER_ANNOTATION\", \"$patientUUID\": \"5117932d-80de-4317-8934-99d44094c513\", \"$documentUUID\": \"cc11cd5d-7021-406f-8893-21956b385c6c\", \"sourceType\": \"USER_ANNOTATION\"}, \"subject\": {\"type\": \"patient\", \"uri\": \"5117932d-80de-4317-8934-99d44094c513\"}, \"source\": {\"type\": \"document\", \"uri\": \"cc11cd5d-7021-406f-8893-21956b385c6c\"}, \"fact\": {\"code\": {\"codeSystemVersion\": null, \"code\": \"5846\", \"displayName\": \"Acute kidney failure with lesion of renal cortical necrosis\", \"codeSystem\": \"2.16.840.1.113883.6.103\"}, \"values\": {\"rejectReason\": \"\", \"result\": \"accept\"}, \"time\": {\"endTime\": \"2014-04-04T00:00:00+0000\", \"startTime\": \"2014-04-04T00:00:00+0000\"}}, \"evidence\": {\"source\": {\"type\": \"user\", \"uri\": \"protest03@apixio.net\"}, \"inferred\": false, \"attributes\": {\"comment\": \"Grinder Flag for Review\", \"presentedHcc\": \"135\", \"code\": \"5846\", \"sweep\": \"finalReconciliation\", \"hccDescription\": \"Acute Renal Failure\", \"presentedHccModelPaymentYear\": \"2015\", \"presentedTag\": \"\", \"dateOfService\": \"04/04/2014\", \"encounterType\": \"Hospital Inpatient Setting: Other Diagnosis\", \"modelPaymentYear\": \"2015\", \"physicianFirstName\": \"Dr. Grinder\", \"codingOrganization\": \"UO_cae34081-757b-4855-ac0b-0c8001fc1f41\", \"codeSystem\": \"\", \"hcc\": \"135\", \"pageNumber\": \"\", \"presentedHccSweep\": \"finalReconciliation\", \"hccPaymentYear\": \"2015\", \"project\": \"PRHCC_6362dec2-70c5-4740-9d06-a5b257ad8ca9\", \"physicianLastName\": \"\", \"timestamp\": \"2015-11-25T00:49:32.871994Z\", \"presentedState\": \"routable\", \"presentedHccDescription\": \"Acute Renal Failure\", \"presentedHccLabelSetVersion\": \"V22\", \"presentedHccMappingVersion\": \"2015 finalReconciliation\", \"codeSystemVersion\": null, \"NPI\": \"\", \"physicianMiddleName\": \"\", \"suggestedPages\": \"1\", \"reviewFlag\": \"true\", \"organization\": \"495\", \"transactionId\": \"c6737cd6-b5f3-4522-9928-3a999a5121d8\"}}}";
    public static void main(String[] args) throws Exception
    {
        com.fasterxml.jackson.databind.ObjectMapper mapper     = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode     httpEntity = mapper.readTree(JSON);

        if (httpEntity instanceof JsonNode)
        {
            for (String field : args)
                System.out.println(jacksonExtract(((JsonNode) httpEntity), field));
        }
        else
        {
            for (String field : args)
                System.out.println(PojoWalker.getObjectField(httpEntity, field));
        }
    }
     */

}
