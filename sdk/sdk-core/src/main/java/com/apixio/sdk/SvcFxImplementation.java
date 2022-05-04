package com.apixio.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.sdk.protos.FxProtos.FxType;
import com.apixio.sdk.util.TypeUtil;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

/**
 * FxImplementation that invokes an external service
 * 
 * Assumes that the service is already running.
 * Calls /setassets and /invokefx
 */

public class SvcFxImplementation implements FxImplementation {
    String postURI = null;
    String fxName = null;

    public SvcFxImplementation(String fxname, String uri) {
        postURI = uri;
        this.fxName = fxname.split("::")[1];
    }

    public void setAssets(Map<String,String> assets) throws Exception {
        Map<String,Object> payload = new HashMap<>();
        payload.put("fxname", fxName);
        payload.put("assetmap", assets);
        post("setassets", TypeUtil.mapToJsonString(payload));
    }

	public Object invoke(List<Object> arglist, FxType outputtype) throws Exception {
        Map<String,Object> payload = new HashMap<>();
        payload.put("fxname", fxName);
        payload.put("outputtype", TypeUtil.protoToBase64String(outputtype));
        payload.put("arguments", arglist);
		return post("invokefx", TypeUtil.mapToJsonString(payload));
	}

    public void setImplementationInfo(ImplementationInfo info) {
    }

    public void setEnvironment(FxEnvironment env) throws Exception {
    }

    protected String post(String endpoint, String jsonpayload) throws UnsupportedEncodingException
    {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(postURI + "/" + endpoint);

        // Request parameters and other properties.
        StringEntity body = new StringEntity(jsonpayload, "UTF-8");
        httppost.setEntity(body);
        httppost.addHeader("content-type", "application/json");
        //Execute and get the response.
        String rv = null;
        HttpResponse response;
        try {
            response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream instream = entity.getContent()) {
                    rv = IOUtils.toString(instream, StandardCharsets.US_ASCII.name());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rv;
    }
}
