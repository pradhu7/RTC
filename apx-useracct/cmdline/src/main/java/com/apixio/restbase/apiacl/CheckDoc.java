package com.apixio.restbase.apiacl.cmdline;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.yaml.snakeyaml.Yaml;

import com.apixio.restbase.apiacl.ApiAcls;
import com.apixio.restbase.apiacl.RamlReader;

/**
 * Checks that all APIs in generated.raml are in user-account.raml
 */
public class CheckDoc extends RamlReader<Set<String>> {

    protected void processApi(Set<String> allApis, String method, String url)
    {
        allApis.add(method + ":" + url);
    }

    /**
     * 
     */
    public static void main(String[] args) throws Exception
    {
        String       ramlGen = args[0];
        String       ramlDoc = args[1];
        CheckDoc     cd      = new CheckDoc();
        Set<String>  allGen  = new HashSet<String>();
        Set<String>  allDoc  = new HashSet<String>();

        cd.walk(allGen, cd.loadYaml(ramlGen), new Stack<String>());
        cd.walk(allDoc, cd.loadYaml(ramlDoc), new Stack<String>());

        System.out.println("################ APIs documented but no longer (?) in generated .raml file:");
        for (String api : allDoc)
        {
            if (!allGen.contains(api))
                System.out.println(api);
        }

        System.out.println("################ APIs in generated .raml but not documented:");
        for (String api : allGen)
        {
            if (!allDoc.contains(api))
                System.out.println(api);
        }
    }

}
