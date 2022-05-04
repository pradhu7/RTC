package com.apixio.restbase.apiacl.cmdline;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.yaml.snakeyaml.Yaml;

import com.apixio.restbase.apiacl.ApiAcls;
import com.apixio.restbase.apiacl.RamlReader;
import com.apixio.restbase.apiacl.perm.RestPermissionEnforcer;

/**
 * Checks that all APIs in .raml are covered by defs in .json
 */
public class ListAllApis extends RamlReader<Void> {

    protected void processApi(Void context, String method, String url)
    {
        System.out.println(method + ":" + url);
    }

    /**
     * 
     */
    public static void main(String[] args) throws Exception
    {
        String                 ramlFile = args[0];
        ListAllApis            laa      = new ListAllApis();
        Map<String, Object>    raml     = laa.loadYaml(ramlFile);

        laa.walk(null, raml, new Stack<String>());
    }

}
