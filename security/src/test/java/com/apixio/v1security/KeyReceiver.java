package com.apixio.v1security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;


class KeyReceiver
{
  private  String keyXml = "";
  private Map<String,String> keyMap = null;

  protected Map<String, String> getKeyFromService(Configuration prop) {
    BufferedReader in = null;
    try {
      URLConnection conn = getConnection(prop);
      in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line;
      while ((line = in.readLine()) != null)
      {
       // String line;
        keyXml += line;
      }
      keyMap = KeyMapper.setKey(keyXml);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("An exception occurred !!");
    } finally
    {
      IOUtils.closeQuietly(in);
    }
    return keyMap;
  }

  private URLConnection getConnection(Configuration prop) throws Exception {
    String token = prop.getString("token");
    token = URLEncoder.encode(token, "UTF-8");
    String primaryDNS = prop.getString("primaryDNS") + "?token=" + token;
    String secondaryDNS = prop.getString("secondaryDNS") + "?token=" + token;
    URL url = null;
    URLConnection conn = null;
    int limit = 6;
    int count = 1;
    boolean connected = false;
    while (count <= limit) {
      if (count % 2 != 0)
        try {
          url = new URL(primaryDNS);
          conn = url.openConnection();
          conn.connect();
          connected = true;
        } catch (Exception e) {
          System.out.println("Connection using primary DNS, " + prop.getString("primaryDNS") + " failed , count = " + (count / 2 + 1));
          System.out.println("Exception is : " + e.getMessage());
        }
      else {
        try {
          url = new URL(secondaryDNS);
          conn = url.openConnection();
          conn.connect();
          connected = true;
        } catch (Exception e) {
          System.out.println("Connection using secondary DNS, " + prop.getString("secondaryDNS") + " failed , count = " + count / 2);
          System.out.println("Exception is : " + e.getMessage());
        }
      }
      if (connected)
        break;
      count++;
    }
    return conn;
  }
}
