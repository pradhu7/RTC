package com.apixio.restbase.apiacl.cmdline;

import java.util.Set;
import java.util.Stack;

import org.yaml.snakeyaml.Yaml;

import com.apixio.restbase.apiacl.RamlReader;

public class Raml2Json extends RamlReader<Void> {

    protected void processApi(Void context, String method, String url)
    {
        /*
		{ "api-id":    "userorg-create",
		  "api-name":  "Create UserOrg",
		  "url":       "POST:/uorgs",
		  "permission-config":
		  { "subject": "token",
			"operation": "ManageUserOrgs",
			"object":  null
			}
		  },
         */

        System.out.println("{ \"api-id\": \"" + url + "\",");
        System.out.println("  \"api-name\": \"" + url + "\",");
        System.out.println("  \"url\": \"" + method + ":" + url + "\",");
        System.out.println("  \"permission-config\": {");
        System.out.println("    \"subject\": \"token\",");
        System.out.println("    \"operation\": \"fillIn\",");
        System.out.println("    \"object\": null");
        System.out.println("  }");
        System.out.println("},\n");
    }

    public static void main(String[] args) throws Exception
    {
        Raml2Json  r2j = new Raml2Json();

        r2j.walk(null, r2j.loadYaml(args[0]), new Stack<String>());
    }

}
