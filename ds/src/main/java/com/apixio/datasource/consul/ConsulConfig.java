package com.apixio.datasource.consul;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConsulConfig
{
    public static class RootAndToken implements Comparable<RootAndToken>
    {
        private String root;
        private String token;

        public RootAndToken(String root, String token)
        {
            this.root  = root;
            this.token = token;
        }

        @Override
        public int compareTo(RootAndToken rootAndToken)
        {
            int c = root.compareTo(rootAndToken.root);
            if (c != 0) return c;

            return token.compareTo(rootAndToken.token);
        }

        public String getRoot()
        {
            return root;
        }

        public String getToken()
        {
            return token;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
                return true;

            if (other == null || getClass() != other.getClass())
                return false;

            RootAndToken rootAndToken = (RootAndToken) other;

            return root.equals(rootAndToken.root) && token.equals(rootAndToken.token);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(root, token);
        }

        @Override
        public String toString()
        {
            return "RootAndToken{" +
                    "root=" + root +
                    ", token=" + token +
                    "}";
        }
    }

    private final static Pattern rootKeysPattern    = Pattern.compile("::");
    private final static Pattern rootKeyItemPattern = Pattern.compile(":");

    private final String CONSUL_URL          = "consulUrl";
    private final String CONSUL_TOKEN        = "consulToken";
    private final String CONSUL_CONN_TIMEOUT = "consulConnTimeout";
    private final String CONSUL_READ_TIMEOUT = "consulReadTimeout";
    private final String CONSUL_DC           = "consulDataCenter";
    private final String CONSUL_ROOTS        = "consulRoots";
    private final String CONSUL_SEPARATOR    = "consulSeparator";
    private final String CONSUL_LENIENT      = "consulLenient";

    private final String              url;
    private final String              token;
    private final long                connTimeoutMs;
    private final long                readTimeoutMs;
    private final String              dataCenter;
    private final List<RootAndToken>  rootAndTokens;
    private final char                separator;
    private final boolean             lenient;

    public ConsulConfig()
    {
        url            = getConfig(CONSUL_URL, "http://127.0.0.1:8500");
        token          = getConfig(CONSUL_TOKEN, "");
        connTimeoutMs  = Long.valueOf(getConfig(CONSUL_CONN_TIMEOUT, "100"));
        readTimeoutMs  = Long.valueOf(getConfig(CONSUL_READ_TIMEOUT, "100"));
        dataCenter     = getConfig(CONSUL_DC, "");
        rootAndTokens  = readConsulRoots();
        separator      = getConfig(CONSUL_SEPARATOR, "/").charAt(0);
        lenient        = Boolean.valueOf(getConfig(CONSUL_LENIENT, "false"));

        if (rootAndTokens.isEmpty())
            throw new IllegalStateException("You need at least one root");
    }

    public void printConfig()
    {
        System.out.println("url = " + url);
        System.out.println("token = " + token);
        System.out.println("connTimeoutMs = " + connTimeoutMs);
        System.out.println("readTimeoutMs = " + readTimeoutMs);
        System.out.println("dataCenter = " + dataCenter);
        System.out.println("rootAndTokens = " + rootAndTokens);
        System.out.println("separator = " + separator);
        System.out.println("lenient = " + lenient);
    }

    // consulRoot is in the form of root1:token1::root2::token2 (tokens can be empty)
    // If token is empty, it inherits the agent token
    private List<RootAndToken> readConsulRoots()
    {
            return rootKeysPattern.splitAsStream(getConfig(CONSUL_ROOTS, ""))
                    .map(s -> rootKeyItemPattern.split(s))
                    .map(a ->
                    {
                        String root = a[0].trim();
                        String tok  = (a.length == 1) ? token : a[1].trim();
                        return new RootAndToken(root, tok);
                    })
                    .collect(Collectors.toList());
    }

    public String getUrl()
    {
        return url;
    }

    public String getToken()
    {
        return token;
    }

    public long getConnTimeout()
    {
        return connTimeoutMs;
    }

    public long getReadTimeout()
    {
        return readTimeoutMs;
    }

    public String getDataCenter()
    {
        return dataCenter;
    }

    public List<RootAndToken> getRootAndTokens()
    {
        return rootAndTokens;
    }

    public Optional<RootAndToken> getRootAndToken(String root)
    {

        return rootAndTokens.stream().filter(rt -> rt.root.equals(root)).findFirst();
    }

    public char getSeparator()
    {
        return separator;
    }

    public boolean isLenient()
    {
        return lenient;
    }

    private String getConfig(String cf, String defaultCf)
    {
        String value = getConfig(cf);

        return value != null ? value : defaultCf;
    }

    private String getConfig(String cf)
    {
        String value = System.getProperty(cf);
        if (value == null)
            value = System.getenv(cf);

        return value;
    }
}
