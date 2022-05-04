package com.apixio.useracct.cmdline;

import java.io.FileReader;
import java.io.BufferedReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple parser to separate a String into space-separated tokens.
 */
class LineParser {

    /**
     * Break a single String line into space-separated arguments, similar to
     * what the bash shell does (double-quotes can be used to protect a
     * substring that has spaces in it).  This class is NOT intended to be
     * called on strings passed directly from bash.
     *
     * When a double-quote character is encountered an immediate scan for
     * the closing double-quote is done and all characters between the quotes
     * are included in the current arg.  Double-quotes can appear anywhere
     * in the line, not just at semantically meaningful points (e.g., after
     * an equals sign).  For example, the following string:
     *
     *    th"is"="a protected stri"g numma" two"
     *
     * will end up as a two arguments:
     *
     *    this=a protected strig
     *    numma two
     */
    static List<String> parseLine(String line)
    {
        /*
         * 0:  skipping whitespace (whitespace characters are arg separators)
         * 1:  in arg, looking for whitespace/eos (quoted strings taken care of here)
         */
        List<String>   elements = new ArrayList<String>();
        int            len      = line.length();
        int            pos      = 0;
        int            state    = 0;
        StringBuilder  argSb    = null;

        while (pos < len)
        {
            char ch = line.charAt(pos);

            if (state == 0)
            {
                if (Character.isWhitespace(ch))
                {
                    pos++;
                }
                else
                {
                    argSb = null;
                    state = 1;
                }
            }
            else if (state == 1)
            {
                if (argSb == null)
                    argSb = new StringBuilder();

                if (ch == '"')
                {
                    int closeQ = line.indexOf('"', pos + 1);

                    if (closeQ == -1)
                        throw new IllegalArgumentException("Missing close quote; scan started at position " + pos + " in string [" + line + "]");

                    argSb.append(line.substring(pos + 1, closeQ));

                    pos = closeQ + 1;
                }
                else if (!Character.isWhitespace(ch))
                {
                    argSb.append(ch);
                    pos++;
                }
                else
                {
                    elements.add(argSb.toString());
                    argSb = null;
                    state = 0;
                    pos++;
                }
            }
        }

        // close/error out as necessary
        if (argSb != null)
            elements.add(argSb.toString());

        return elements;
    }


    /**
     * Test via reading from file
     */
    public static void main(String[] args) throws Exception
    {
        for (String fname : args)
        {
            BufferedReader br = new BufferedReader(new FileReader(fname));
            String         line;

            while ((line = br.readLine()) != null)
            {
                List<String> largs = parseLine(line);

                System.out.println("[" + line + "] => " + largs);
            }

            br.close();
        }
    }
}
