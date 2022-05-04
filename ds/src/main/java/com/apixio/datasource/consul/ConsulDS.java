package com.apixio.datasource.consul;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.orbitz.consul.KeyValueClient;

import com.apixio.datasource.consul.consultype.ConsulTypesUtils;
import com.orbitz.consul.option.ImmutablePutOptions;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.PutOptions;
import com.orbitz.consul.option.QueryOptions;

import static com.apixio.datasource.consul.consultype.ConsulTypes.TypeAndValue;
import static com.apixio.datasource.consul.ConsulConfig.RootAndToken;

/**
 * The main purpose of this class is to read the consul configuration and return it as a map.
 * It also writes to consul. This is intended for migration purposes from yaml to consul.
 *
 * The class is thread safe.
 *
 * Note: Once the configuration is read from Consul, it is saved in memory and never updated!!!
 */
public class ConsulDS
{
    public static class RootAndKV implements Comparable<RootAndKV>
    {
        private String root;
        private String key;
        private Object value;

        public RootAndKV(String root, String key, Object value)
        {
            this.root  = root;
            this.key   = key;
            this.value = value;
        }

        @Override
        public int compareTo(RootAndKV rootAndKV)
        {
            int c = root.compareTo(rootAndKV.root);
            if (c != 0) return c;

            return key.compareTo(rootAndKV.key);
        }

        public String getRoot()
        {
            return root;
        }

        public String getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        // Important: We use only "key" field for equals and hashCode. That is important!!!

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
                return true;

            if (other == null || getClass() != other.getClass())
                return false;

            RootAndKV rootAndKV = (RootAndKV) other;

            return key.equals(rootAndKV.key);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(key);
        }

        @Override
        public String toString()
        {
            return "RootAndKV{" +
                    "root=" + root +
                    ", key=" + key +
                    ", objectValue=" + value +
                    ", consulValue=" + ConsulTypesUtils.convertObjectToConsulValue(value) +
                    "}";
        }
    }

    private static ConsulDS            instance;
    private static ConsulConfig        consulConfig;
    private static KeyValueClient      kvClient;
    private static Map<String, Object> config;
    private static Set<RootAndKV>      kvs;

    private ConsulDS()
    {
        consulConfig = new ConsulConfig();

        // debug info
        consulConfig.printConfig();

        kvClient = new ConsulConnector(consulConfig).getClient();
    }

    public static Map<String, Object> getConfig(char configSeparator)
    {
        if (instance == null)
        {
            synchronized (ConsulDS.class)
            {
                if (instance == null)
                {
                    instance = new ConsulDS();
                }
            }
        }

        if (config == null || config.isEmpty())
        {
            synchronized (ConsulDS.class)
            {
                if (config == null || config.isEmpty())
                {
                    config = getConfigForAllRoots(configSeparator);
                }
            }
        }

        return config;
    }

    public static Set<RootAndKV> getKvs(char configSeparator)
    {
        if (instance == null)
        {
            synchronized (ConsulDS.class)
            {
                if (instance == null)
                {
                    instance = new ConsulDS();
                }
            }
        }

        if (kvs == null || kvs.isEmpty())
        {
            synchronized (ConsulDS.class)
            {
                if (kvs == null || kvs.isEmpty())
                {
                    kvs = getKVsForAllRoots(configSeparator);
                }
            }
        }

        return kvs;
    }

    public static void writeKV(String key, Object value, String consulRoot, char configSeparator)
    {
        if (instance == null)
        {
            synchronized (ConsulDS.class)
            {
                if (instance == null)
                {
                    instance = new ConsulDS();
                }
            }
        }

        TypeAndValue tv = ConsulTypesUtils.convertObjectToConsulValue(value);

        if (tv != null)
        {
            Optional<RootAndToken> rt = consulConfig.getRootAndToken(consulRoot);
            PutOptions putOptions = rt.isPresent() ? ImmutablePutOptions.builder().token(rt.get().getToken()).build() : PutOptions.BLANK;
            String     newKey     = changeSeparator(decorateWithConsulRoot(key, consulRoot, consulConfig.getSeparator()), configSeparator, consulConfig.getSeparator());

            System.out.println("newKey=" + newKey + "; value= " + tv);

            kvClient.putValue(newKey, tv.getValue(), tv.getConsulType().getFlag(), putOptions);
        }
    }

    private static Map<String, Object> getConfigForAllRoots(char configSeparator)
    {
        List<RootAndToken> rootAndTokens = consulConfig.getRootAndTokens();

        return rootAndTokens.stream()
                .map(rootAndToken -> getKVsForEachRoot(rootAndToken.getRoot(), rootAndToken.getToken(), configSeparator))
                .flatMap(Collection::stream)
                .map(rootAndKV -> new AbstractMap.SimpleEntry<>(rootAndKV.key, rootAndKV.value))
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
    }

    private static Set<RootAndKV> getKVsForAllRoots(char configSeparator)
    {
        List<RootAndToken> rootAndTokens = consulConfig.getRootAndTokens();

        return rootAndTokens.stream()
                .map(rootAndToken -> getKVsForEachRoot(rootAndToken.getRoot(), rootAndToken.getToken(), configSeparator))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private static List<RootAndKV> getKVsForEachRoot(String consulRoot, String token, char configSeparator)
    {
        try
        {
            char consulSeparator =  consulConfig.getSeparator();

            QueryOptions queryOptions = (token != null && !token.isEmpty()) ? ImmutableQueryOptions.builder().token(token).build() : QueryOptions.BLANK;
            List<String> keys         = kvClient.getKeys(consulRoot, queryOptions);

            return keys.stream()
                    .map(key ->
                    {
                        String newKey = changeSeparator(stripConsulRoot(key, consulRoot, consulSeparator), consulSeparator, configSeparator);
                        Object obj    = ConsulTypesUtils.convertConsulValueToObject(kvClient.getValue(key, queryOptions));
                        return obj    != null ? new RootAndKV(consulRoot, newKey, obj) : null;
                    })
                    .filter(e -> e != null)
                    .collect(Collectors.toList());
        }
        catch (Exception e)
        {
            if (!consulConfig.isLenient())
                throw new IllegalStateException("No access to consulRoot=" + consulRoot);
            else
                return new ArrayList<>();
        }
    }

    private static String stripConsulRoot(String key, String consulRoot, char consulSeparator)
    {
        if (consulRoot == null || consulRoot.trim().length() == 0)
            return key;

        return key.replaceFirst(consulRoot + consulSeparator, "");
    }

    private static String decorateWithConsulRoot(String key, String consulRoot, char consulSeparator)
    {
        if (consulRoot == null || consulRoot.trim().length() == 0)
            return key;

        if (key.startsWith(consulRoot + consulSeparator))
            return key;
        else
            return consulRoot + consulSeparator + key;
    }

    private static String changeSeparator(String key, char fromSeparator, char toSeparator)
    {
        return key.replace(fromSeparator, toSeparator);
    }
}
