package com.apixio.sdk.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import java.net.URI;
import com.apixio.sdk.Converter;
import com.apixio.sdk.protos.FxProtos.FxTag;
import com.apixio.sdk.protos.FxProtos.FxType;
import com.apixio.sdk.test.FxTest.TestRequest;
import com.apixio.sdk.util.TypeUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.apixio.sdk.util.FxIdlParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;



public class FxTestServer {

    FxTest fxtest = null;
    public FxTestServer(String daoConfig, String logConfig) throws Exception {
        fxtest = new FxTest(daoConfig, logConfig);
    }
    
    public static void main(String[] args) throws Exception {
        FxTestServer fxtester = new FxTestServer(args[0], null);
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/accessor", new AccessorHandler(fxtester.fxtest));
        server.createContext("/persist", new PersistHandler(fxtester.fxtest));
        server.setExecutor(null); // creates a default executor
        System.out.println("Starting HTTP server on port 8000");
        server.start();
    }
    
    static class PersistHandler implements HttpHandler {

        private FxTest fxTest;
        public PersistHandler(FxTest fxtest) {
            this.fxTest = fxtest;
        }
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream body = t.getRequestBody();
            // List<String> lines = IOUtils.readLines(body);
            HashMap<String,Object> bodymap = new ObjectMapper().readValue(body, typeRef);
            Object dataObj = bodymap.get("data");
            String dataUri = (String) bodymap.get("dataUri");
            String response = "empty";
            try {
                String dataType = (String) bodymap.get("type");
                if (dataType.equals("string")) {
                    URI finalDataUri = fxTest.persist(TypeUtil.stringType, new URI(dataUri), dataObj);
                    response = "success";
                } else {
                    FxType type = TypeUtil.protoStringToFxType(dataType);
                    if (type.getTag() == FxTag.SEQUENCE) {
                        List<String> dataList = (List<String>) dataObj;
                        String signalPrettyName = type.getSequenceInfo().getOfType().getContainerInfo().getName();
    
                        Converter converter = fxTest.getConverter(type);
                        Object data = TypeUtil.convertStringListToInterfaces(converter, dataList);
                        URI finalDataUri = fxTest.persist(type, new URI(dataUri), data);
                        response = "success";
                    }
                }
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }   
    static class AccessorHandler implements HttpHandler {

        private FxTest fxTest;
        public AccessorHandler(FxTest fxtest) {
            this.fxTest = fxtest;
        }
        TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String,String>>() {};
        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream body = t.getRequestBody();
            HashMap<String,String> bodymap = new ObjectMapper().readValue(body, typeRef);
            String accessorString = bodymap.get("accessorString");
            bodymap.remove("accessorString");
            String response = "empty";
            try {
                Object result = fxTest.runAccessor(accessorString, new TestRequest(bodymap));
                Map<String, Object> argdetails = new HashMap<>();
                if (accessorString.startsWith("patientannotations")) {
                    FxType listsignalType = FxIdlParser.parse("list<apixio.Signal> fake()").getReturns();
                    List<String> argProtoStrs = ((List<Object>) result).stream()
                    .map(k -> TypeUtil.interfaceToProtoString(fxTest.getConverter(listsignalType), k))
                    .collect(Collectors.toList());
                    argdetails.put("type", TypeUtil.protoToBase64String(listsignalType));
                    argdetails.put("data", argProtoStrs);
                } else if (result instanceof List) {
                    List<String> argProtoStrs = ((List<Object>) result).stream()
                    .map(k -> TypeUtil.protoToBase64String((Message) k))
                    .collect(Collectors.toList());
                    argdetails.put("type", "list<" + ((List<Object>)result).get(0).getClass().getName().replace("$",".") + ">"); // TODO make robust
                    argdetails.put("data", argProtoStrs);
                } else {
                    response = result.toString();
                }
                response = TypeUtil.mapToJsonString(argdetails);                    
            } catch (Exception e) {
                response = "{\"ERROR\":\"" + e.getMessage() + "\"}";
                e.printStackTrace();
            }
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}
