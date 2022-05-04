package com.apixio.restbase.entity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * ParamSet insulates the clients of it from the specifics of how to persist
 * and restore a set of name=value (or name=ParamSet) pairs.  (Basically, I
 * didn't want to expose, for example, Jackson's JsonNode and ObjectNode to
 * all the code that deals with parameter set information).  It also supports
 * persisting to either a Map<String,String> or a JSON string.
 *
 * The functionality itself mirrors a Map and/or JSON structure.  The contents of
 * a ParamSet can be "persisted" to either a Map<String, String> or a String
 * (formatted as JSON).  The nesting of ParamSets within ParamSets is handled
 * differently in each case:  persisting to Map<String, String> will put in
 * namespace-scoped keys (using ":" as scope separators) while persisting
 * to a JSON string will keep the natural nesting.
 *
 * (Both forms of persisting are required because ParamSet can be used by
 * both events and objects kept in Redis (and the Context object is used in
 * both): JSON for events, and Map for objects.  The reason that JSON isn't
 * used for objects is that we don't want to have to read in the full JSON
 * just to update a single object field.  While this certainly isn't strictly
 * necessary, it does allow for direct updates via poking into HASHes in
 * Redis).
 */
public class ParamSet {

    /**
     * Use a single instance of ObjectMapper for optimal efficiency of
     * JSON operations.
     */
    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The char used to delimit levels of nested param sets.
     */
    private static final String SCOPER = ":";

    /**
     * Params are kept internally as a Map<String, Object>, and are converted
     * to/from Map<String, String> and JSON as needed.  Values of Map are
     * either a String or another Map<String, Object> for nested ParamSets.
     */
    private Map<String, Object> params = new HashMap<String, Object>();

    /**
     * Create empty ParamSet
     */
    public ParamSet()
    {
    }

    /**
     * Create a ParamSet from a string that was created via .toJSON().
     */
    public ParamSet(String json)
    {
        try
        {
            JsonNode  root = objectMapper.readTree(json);

            walkJson(this, root);
        }
        catch (IOException ix)
        {
        }
    }

    /**
     * Create a ParamSet from a Map<> produced by .toMap().
     */
    public ParamSet(Map<String, String> map)
    {
        /*
         * Just create child ParamSets while reading flattened keys.  For example, "a:b:c"
         * will create two ParamSets (the first--topmost one) was already created during
         * the creation of the 'this' object.  The findInner creates if not there.
         */

        for (Map.Entry<String, String> entry : map.entrySet())
        {
            String[]  keys = entry.getKey().split(SCOPER);

            // find/create inner ParamSet, then directly set a String (all leaf nodes
            // are Strings).
            findInner(this, keys, 0).params.put(keys[keys.length - 1], entry.getValue());
        }
    }

    /**
     * Return # of field elements.
     */
    public int size()
    {
        return params.size();
    }

    /**
     * Converts the ParamSet into a JSON string that can later be used to reconstruct the
     * equivalent ParamSet.  Fully supports nesting of ParamSets, where the created JSON
     * contains nested objects.
     */
    public String toJSON()
    {
        return convertToJSON(this).toString();
    }

    /**
     * Converts the ParamSet into a simple Map<String, String> where the names of the keys
     * in the returned Map are a colon-separated list of hierarchical key names from the
     * original ParamSet hierarchy.  For example, if the root ParamSet contains a child
     * ParamSet accessible via the key "data", and that child ParamSet contains a key
     * "something", the key name in the returned Map will be "data:something".
     *
     * The returned Map<> can be used to reconstruct an equivalent ParamSet by passing
     * it to the constructor.
     */
    public Map<String, String> toMap()
    {
        Map<String, String> out = new HashMap<String, String>();

        flattenMap(this, "", out);

        return out;
    }

    /**
     * General setting of name=value, where both name and value are strings.
     */
    public void put(String name, String value)
    {
        if (value != null)
            params.put(name, value);
    }

    /**
     * Setting of name=object, where object is another ParamSet.  This allows nesting
     * and scoping of the namespaces, etc.
    public void put(String name, ParamSet value)
    {
        if (value != null)
            params.put(name, value);
    }
     */

    /**
     * Looks up and returns the value of the given key.  If the value stored is actually
     * a ParamSet, or does not exist, then null is returned.
     */ 
    public String get(String name)
    {
        Object v = params.get(name);

        return (v instanceof String) ? ((String) v) : null;
    }

    /**
     * Looks up and returns the sub-ParamSet accessible by the given key.  If the value
     * stored is a String, or does not exist, then null is returned.
     */
    public ParamSet getParamSet(String name)
    {
        Object v = params.get(name);

        return (v instanceof ParamSet) ? ((ParamSet) v) : null;
    }

    /**
     * Finds (creates if not there) the parent ParamSet of the last key in the 'keys' array.
     * If, for example, there is only 1 key, then return the ParamSet passed in.
     */
    private static ParamSet findInner(ParamSet ps, String[] keys, int idx)
    {
        if (idx == (keys.length - 1))
        {
            return ps;
        }
        else
        {
            Object ips = ps.params.get(keys[idx]);

            if (ips == null)
            {
                ips = new ParamSet();
                ps.params.put(keys[idx], (ParamSet) ips);
            }

            return findInner(((ParamSet) ips), keys, idx + 1);
        }
    }

    /**
     * Walks the JsonNode hierarchy (restored from a JSON string via readTree()) and creates
     * corresponding ParamSet nodes, assembles them together, and sets the leaf node
     * values.
     */
    private static void walkJson(ParamSet ps, JsonNode node)
    {
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); )
        {
            Map.Entry<String, JsonNode> entry = it.next();
            String                      key   = entry.getKey();
            JsonNode                    value = entry.getValue();

            if (value.isObject())
            {
                ParamSet child = new ParamSet();

                walkJson(child, entry.getValue());
                ps.params.put(key, child);
            }
            else
            {
                ps.params.put(key, value.asText());
            }
        }
    }

    /**
     * Walks the hierarchy of ParamSets and combines all leaf name=value pairs into a
     * single Map, where the keys of the map are the colon-separated hierarchy of
     * keys.
     */
    private void flattenMap(ParamSet ps, String scope, Map<String, String> out)
    {
        for (Map.Entry<String, Object> entry : ps.params.entrySet())
        {
            String key = scope + entry.getKey();
            Object val = entry.getValue();

            if (val instanceof String)
                out.put(key, (String) val);
            else if (val instanceof ParamSet)
                flattenMap((ParamSet) val, (key + SCOPER), out);
            else
                throw new IllegalStateException("Expected either String or ParamSet as value but got [" + val + "] for key [" + key + "]");
        }
    }

    /**
     * Walks the hierarchy of ParamSets and creates an equivalent JSON hierarchy and returns it.
     */
    private ObjectNode convertToJSON(ParamSet ps)
    {
        ObjectNode  node = objectMapper.createObjectNode();

        for (Map.Entry<String, Object> entry : ps.params.entrySet())
        {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (val instanceof String)
                node.put(key, (String) val);
            else if (val instanceof ParamSet)
                node.set(key, convertToJSON((ParamSet) val));
            else
                throw new IllegalStateException("Expected either String or ParamSet as value but got:  " + val);
        }

        return node;
    }

    /**
     * Debug/testing
     */
    public String toString()
    {
        return params.toString();
    }

    /**
     * test
    public static void main0(String[] args)
    {
        ParamSet ps1 = new ParamSet();
        ParamSet ps2 = new ParamSet();

        ps1.put("top", ps2);
        ps2.put("f1", "hi");
        ps2.put("f2", "bye");

        Map<String, String> flat1 = ps1.toMap();

        System.out.println("Map:   " + flat1);
        ParamSet ps3 = new ParamSet(flat1);
        System.out.println("Map2:  " + ps3.toMap());
    }

    public static void main2(String[] args)
    {
        ParamSet ps1 = new ParamSet();

        ps1.put("f1", "hi");
        ps1.put("f2", "bye");

        Map<String, String> flat1 = ps1.toMap();

        System.out.println("Map:   " + flat1);
        ParamSet ps2 = new ParamSet(flat1);
        System.out.println("Map2:  " + ps2.toMap());
    }

    public static void main(String[] args)
    {
        ParamSet ps1 = new ParamSet();
        ParamSet ps2 = new ParamSet();
        ParamSet ps3 = new ParamSet();

        ps1.put("outer-scalar", "scalar1");
        ps1.put("outer-nested", ps2);

        ps2.put("middle", "scalar2");
        ps2.put("middle-nested", ps3);

        ps3.put("innermost1", "scalar3");
        ps3.put("innermost2", "scalar4");

        Map<String, String> flat1 = ps1.toMap();
        System.out.println("Map:   " + flat1);
        ParamSet ps4 = new ParamSet(flat1);
        System.out.println("reconstituted = " + ps4.params);
        System.out.println("Rcon:  " + ps4.toMap());
        String js1 = ps1.toJSON();
        System.out.println("JSON:  " + js1);
        ParamSet ps5 = new ParamSet(js1);
        System.out.println("rJSON: " + ps5.toJSON());
    }
     */
}
