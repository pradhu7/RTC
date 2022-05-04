package com.apixio.restbase.apiacl.cmdline;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.yaml.snakeyaml.Yaml;

import com.apixio.restbase.apiacl.ApiAcls;
import com.apixio.restbase.apiacl.RamlReader;

/**
 * Checks that all APIs in .raml are covered by defs in .json
 *
 * 
 */
public class CheckCoverage extends RamlReader<ApiAcls> {

    protected void processApi(ApiAcls apiacls, String method, String url)
    {
        System.out.println("########### " + method + ":" + url + ": " +
                           ((apiacls.hasMatch(method, url)) ? "covered" : "not covered"));
    }

    /**
     * 
     */
    public static void main(String[] args) throws Exception
    {
        String                 jsonFile = args[0];
        String                 ramlFile = args[1];
        CheckCoverage          cc       = new CheckCoverage();
        Map<String, Object>    raml     = cc.loadYaml(ramlFile);
        ApiAcls                acls     = ApiAcls.fromJsonFile(null, jsonFile);

        cc.walk(acls, raml, new Stack<String>());
    }

}
