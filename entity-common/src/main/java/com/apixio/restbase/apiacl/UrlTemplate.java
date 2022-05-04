package com.apixio.restbase.apiacl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.restbase.apiacl.model.HttpMethod;

/**
 * A UrlTemplate represents a URL definition of the form METHODS:URL where
 * METHODS is a csv-list of the HTTP method (GET, PUT, ..., and "*") and URL can
 * include {} placeholders and can include a trailing wildcard.
 *
 * The full template is parsed into the method and URL components and can match
 * a real request URL against all these elements to determine match as well as
 * the placeholder values.
 *
 * This is (intentionally) a subset of functionality available within JAX-RS's
 * template system.  One big difference is that this class doesn't handle
 * regular expressions within the placeholders.  Another (intentional)
 * limitation is that placeholders are not allowed as the first component.
 */
public class UrlTemplate {

    private static final String WILDCARD = "*";

    /**
     * One piece of the URL (between "/" characters).
     */
    private static class Component {
        String  name;
        boolean isPlaceholder;

        Component(String name, boolean isPlaceholder)
        {
            this.name          = name;
            this.isPlaceholder = isPlaceholder;
        }

        boolean isWild()
        {
            return WILDCARD.equals(name);
        }

        @Override
        public String toString()
        {
            return "(n=" + name + ", ph=" + isPlaceholder + ")";
        }
    }

    /**
     * Unparsed URI
     */
    private String uri;

    /**
     * The parsed method.
     */
    private EnumSet<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);

    /**
     * The parsed URL components.
     */
    private Component[] components;

    /**
     * Parse a "full" URL template; this means, specifically, that it includes the method
     * and the URL, separated by a colon character.
     */
    public UrlTemplate(String url)
    {
        int co = url.indexOf(':');

        if ((co == -1) || (co + 1 >= url.length()))
            throw new IllegalArgumentException("Invalid UrlTemplate syntax (must be METHOD:URI):  " + url);

        for (String method : url.substring(0, co).split(","))
        {
            if (!method.isEmpty())
                methods.add(HttpMethod.fromString(method));
        }

        parseUri(url.substring(co + 1));
    }

    /**
     * Create a UrlTemplate from an already-separated method and URL components.
     */
    public UrlTemplate(String method, String uri)
    {
        this(HttpMethod.fromString(method), uri);
    }

    /**
     * Create a UrlTemplate from an already-separated (parsed) method and URL components.
     */
    public UrlTemplate(HttpMethod method, String uri)
    {
        this.methods = EnumSet.of(method);
        parseUri(uri);
    }

    private void parseUri(String raw)
    {
        uri = validateUri(raw);  // for safekeeping

        List<Component> comps   = new ArrayList<Component>();
        int             len     = uri.length();
        int             coStart = -1;
        int             idStart = -1;
        int             pos     = 0;
        int             state   = 0;   // 0=look for /; 1=deciding on comp or id; 2=in comp; 3=in id (look for '}')

        while (pos < len)
        {
            char ch = uri.charAt(pos);

            if (state == 0)    // we're expecting a '/'; anything else is illegal
            {
                if (ch != '/')
                    throw new IllegalArgumentException("Invalid syntax in URL [" + uri + "]");

                state = 1;
            }
            else if (state == 1)  // hit "/"; if we get "{" then we're in an ID otherwise we look for "/" to end static component
            {
                if (ch == '{')
                {
                    idStart = pos + 1;
                    state = 3;
                }
                else
                {
                    coStart = pos;
                    state = 2;
                }
            }
            else if (state == 2)     // in a static component
            {
                if (ch == '{') // or '}'
                    throw new IllegalArgumentException("Encountered '{' not immediately after '/' in URL [" + uri + "]");

                if (ch == '/')
                {
                    Component comp = new Component(uri.substring(coStart, pos), false);

                    comps.add(comp);

                    coStart = -1;
                    state = 0;
                    pos--;

                    if (comp.isWild())
                        break;
                }
            }
            else if (state == 3)      // in a placeholder
            {
                if (ch == '}')
                {
                    comps.add(new Component(uri.substring(idStart, pos), true));
                    idStart = -1;
                    state = 0;
                }
            }

            pos++;
        }

        if (idStart != -1)
            throw new IllegalArgumentException("In URL [" + uri + "]:  encountered an unclosed identifier");

        if (coStart != -1)
        {
            String comp = uri.substring(coStart);

            if (comp.length() > 0)
                comps.add(new Component(comp, false));
        }

        if ((comps.size() == 0) || (comps.get(0).isPlaceholder))
            throw new IllegalArgumentException("In URL [" + uri + "]:  either empty URL or first component is a placeholder (which is not allowed)");

        components = comps.toArray(new Component[comps.size()]);
    }

    public EnumSet<HttpMethod> getMethods()
    {
        return methods;
    }

    /**
     * Returns the count of URL components, both static and placeholders.
     */
    public int getComponentCount()
    {
        return components.length;
    }

    /**
     * Returns null if the match fails.  Returns empty map if no path params, otherwise
     * returns map of templateName=actualValue.
     *
     * This is finite-state-machine code to increase performance as much as possible since
     * this will be called on each and every request, across all APIs.
     */
    public Map<String, String> match(String method, String url)
    {
        if (!compatibleMethod(method))
            return null;

        url = validateUri(url);  // this is an expensive operation in the end...

        Map<String, String> params  = new HashMap<String, String>();  // must start out non-null (set to null on failure to match)
        int                 len     = url.length();
        int                 coLen   = components.length;
        int                 coIdx   = 0;   // current component index to attempt to match against
        int                 pos     = 0;
        int                 state   = 0;   // 0=look for /; 1=in component name
        int                 coStart = -1;

        // loop assumes leading '/'
        while (pos < len)
        {
            char ch = url.charAt(pos);

            if (state == 0)       // initialization of coStart (component start position)
            {
                coStart = pos + 1;
                state   = 1;
            }
            else // (state == 1)
            {
                if (ch == '/')  // final char cannot be '/' (due to validateUri)
                {
                    Component curCo = components[coIdx++];
                    String    comp;

                    if (curCo.isWild())
                    {
                        break;
                    }
                    else if (coIdx >= coLen)  // tested URL is longer than this template
                    {
                        params = null;
                        break;
                    }

                    comp = url.substring(coStart, pos);

                    if (curCo.isPlaceholder)
                    {
                        params.put(curCo.name, comp);
                    }
                    else if (!curCo.name.equals(comp)) // test URL component doesn't match non-placeholder element
                    {
                        params = null;
                        break;
                    }

                    state = 0;
                    pos--;
                }
            }

            pos++;
        }

        // params non-null either because we ran off end of URL (it won't have final '/') or
        // last component is a wildcard
        if (params != null)
        {
            if (coIdx < coLen)  // i.e., last component is not a wildcard (coIdx is >= coLen IFF last component is wildcard)
            {
                Component curCo = components[coIdx++];

                if (!curCo.isWild())
                {
                    String    comp    = url.substring(coStart, len);

                    if (coIdx < coLen) // not enough components
                        params = null;
                    else if (curCo.isPlaceholder)
                        params.put(curCo.name, comp);
                    else if (!comp.equals(curCo.name))
                        params = null;
                }
            }
        }

        return params;
    }

    /**
     *
     */
    public boolean compatibleMethod(String method)
    {
        return compatibleMethod(HttpMethod.fromString(method));
    }
    public boolean compatibleMethod(HttpMethod method)
    {
        return methods.contains(HttpMethod.ANY) || methods.contains(method);
    }

    /**
     * Make sure certain constraints are met, adjusting the URL where possible to meet
     * those constraints (e.g, remove trailing "/" chars).
     */
    private static String validateUri(String url)
    {
        if ((url == null) || (url.length() == 0))
            throw new IllegalArgumentException("Url cannot be empty");
        else if (!url.startsWith("/"))
            throw new IllegalArgumentException("Url [" + url + "] must start with '/'");

        int pos = url.indexOf('?');
        int end;
        int len;

        if (pos != -1)
            url = url.substring(0, pos);

        pos = 0;
        end = len = url.length();

        // make sure it starts with exactly 1 '/'
        if (!url.startsWith("/") || url.startsWith("//"))
            throw new IllegalArgumentException("Url [" + url + "] must start with a single '/'");

        // remove all trailing '/'s
        while ((end > 0) && (url.charAt(end - 1) == '/'))
            end--;

        if ((pos == 0) && (end == len))
            return url;
        else if (pos < end)
            return url.substring(pos, end);
        else
            throw new IllegalArgumentException("Url [" + url + "] reduces to empty string");
    }

    /**
     *
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("API=");
        sb.append(methods.toString());
        sb.append(":");

        for (Component c : components)
        {
            sb.append('/');
            sb.append(c.name);
        }

        return sb.toString();
    }

}
