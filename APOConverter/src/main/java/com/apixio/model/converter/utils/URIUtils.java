package com.apixio.model.converter.utils;

import com.apixio.model.converter.implementations.NewSource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jctoledo on 4/8/16.
 */
public class URIUtils {
    static final Logger logger = Logger.getLogger(URIUtils.class);
    /**
     * Create a url safe encoding of a string
     *
     * @param inputString a string to url encode
     * @return an urlencoded string
     */
    public static String urlEncode(String inputString) {
        try {
            return URLEncoder.encode(inputString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            assert false : "This should never happen";
            return null;
        }
    }

    /**
     * Create an MD5 hash of the provided string
     *
     * @param message a string to encode
     * @return an md5 hash representation of a string
     */
    public static String hash(String message) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(message.getBytes());
            BigInteger hash = new BigInteger(1, digest.digest());
            return hash.toString(16);
        } catch (NoSuchAlgorithmException e) {
            assert false : e.getMessage();
            return null;
        }
    }


}
