package com.apixio.utility;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StringList
{
    /**
     * Flattening a list of strings is a smaller and more efficient serialization as compared to
     * Java's generalized serialization.  This is done for three reasons: it's faster (more than
     * 5x), to conserve space and to standardize the format so that class version changes couldn't
     * affect restoration (yes, it's highly unlikely List<String> will not be restorable...).
     *
     * Syntax of returned String:
     *
     *  [n;chars];...
     *
     * where n is the # of characters following that form the next string.  The ";" as a separate is
     * strictly unnecessary but is useful for human readability.
     */

    /**
     * flattenList flattens a List of Strings to a single string that can be parsed back into a List
     * of Strings via restoreList().
     */
    public static String flattenList(List<String> elements)
    {
        StringBuffer  sb = new StringBuffer();

        for (String s : elements)
        {
            if (s == null)
            {
                sb.append("0;;");
            }
            else
            {
                sb.append(Integer.toString(s.length()));
                sb.append(';');
                sb.append(s);
                sb.append(';');  // this is done purely for debugging help (';' is a nice visual eos indicator)
            }
        }

        return sb.toString();
    }

    public static String flattenList(String... elements)
    {
        StringBuffer  sb = new StringBuffer();

        for (String s : elements)
        {
            if (s == null)
            {
                sb.append("0;;");
            }
            else
            {
                sb.append(Integer.toString(s.length()));
                sb.append(';');
                sb.append(s);
                sb.append(';');  // this is done purely for debugging help (';' is a nice visual eos indicator)
            }
        }

        return sb.toString();
    }

    /**
     * restoreList restores a value returned from flattenList() to its (logically equivalent) List of
     * Strings.
     */
    public static List<String> restoreList(String flattened)
    {
        List<String>  elements = new ArrayList<String>();

        if ((flattened == null) || (flattened.trim().length() == 0))
            return elements;  // allows caller to add to list easily

        int     max       = flattened.length();
        int     cur       = 0;
        boolean useLinked = (max > 500000);  // arbtrary #: we assume it's more efficient overall to use linked list in this case

        if (useLinked)
            elements = new LinkedList<String>();

        while (cur < max)
        {
            int  semi = flattened.indexOf(';', cur);
            int  len;

            if (semi == -1)
                break;

            len = Integer.parseInt(flattened.substring(cur, semi));

            if (len < 0)
            {
                throw new IllegalArgumentException("Invalid flattened string:  " + flattened);
            }
            else if (len == 0)
            {
                elements.add("");
                if (semi + 1 >= max)
                    break;
                cur = semi + 1 + 1;  // +1 to skip over ";" at end of number, +1 to get past element separator
            }
            else if (semi + len > max)
            {
                throw new IllegalArgumentException("Invalid flattened string (not enough chars):  " + flattened);
            }
            else
            {
                int np = semi + 1 + len;

                elements.add(flattened.substring(semi + 1, np));
                if (np + 1 >= max)
                    break;

                cur = np + 1;
            }
        }

        // convert to arraylist so clients have O(1) with random access
        if (useLinked)
            elements = new ArrayList<>(elements);

        return elements;
    }

}
