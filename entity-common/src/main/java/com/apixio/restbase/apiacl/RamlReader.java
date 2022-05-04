package com.apixio.restbase.apiacl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.yaml.snakeyaml.Yaml;

/**
 * Generalized RamlReader that allows operation-specific code to read in
 * and walk the RAML tree of APIs, where this class constructs the full
 * String URL path during the walk.
 */
public abstract class RamlReader<C> {   // "C" for Context

    /**
     * by spec, each node that begins with '/' marks a resource.
     * under a resource node we need to look for nodes with the names
     *  [post, get, delete, put, ...].  If a subnode begins with '/'
     * then it is a subresource.
     *
     * if a resource url begins with "/{" then it is a templated element
     */

    private static Set<String> METHODS = new HashSet<String>();
    static
    {
        METHODS.addAll(Arrays.asList(new String[] { "get", "post", "put", "delete" }));
    }

    /**
     * Load configuration from .yaml file into generic Map.
     */
    protected Map<String, Object> loadYaml(String filepath) throws Exception
    {
        InputStream input = new FileInputStream(new File(filepath));
        Yaml        yaml  = new Yaml();
        Object      data = yaml.load(input);

        input.close();

        return (Map<String, Object>) data;
    }

    /**
     * Subclasses should call this with the value returned from loadYaml to walk the
     * tree.  Each API node results in a call to processApi.
     */
    protected void walk(C context, Map<String, Object> node, Stack<String> path)
    {
        for (Map.Entry<String, Object> elem : node.entrySet())
        {
            String key = elem.getKey();
            Object val = elem.getValue();

            if (key.startsWith("/"))
            {
                String newp = (path.empty()) ? key : path.peek() + key;

                path.push(newp);

                walk(context, (Map<String, Object>) val, path);

                path.pop();
            }
            else if (METHODS.contains(key.toLowerCase()))
            {
                processApi(context, key.toUpperCase(), path.peek());
            }
        }
    }

    /**
     * Called for each RESTful API encountered in the raml file.  Called during walk().
     */
    protected abstract void processApi(C context, String method, String url);

}
