package com.apixio.sdk.util;

import java.util.List;

import com.apixio.sdk.util.FxBuilder;

/**
 * Generally reusable tokenizer that can be used by a parser.  Scanning ignores all whitespace
 * and "alphanum" tokens are terminated by anything non-alphanumeric
 */
public class Tokenizer
{

    /**
     * Strings that are interpreted as Boolean.TRUE and Boolean.FALSE
     */
    private final static String BOOL_TRUE  = "true";
    private final static String BOOL_FALSE = "false";

    /**
     * Really just special characters and alphanumeric sequence
     */
    public enum TokenType
    {
        LPAREN, RPAREN,
        LANGLE, RANGLE,
        COMMA,
        DOT,
        ALPHANUM,
        BOOLEAN, STRING, INTEGER, FLOATING
    }

    /**
     * Tokens are a type (above) and an optional String value (for ALPHANUM)
     */
    public static class Token
    {
        TokenType type;
        Boolean   boolValue;
        Double    floatingValue;
        Long      integerValue;
        String    strValue;  // non-null only for TokenType.ALPHANUM

        public Token(TokenType type)
        {
            this.type = type;
        }

        public Token(TokenType type, Boolean value)
        {
            this.type      = type;
            this.boolValue = value;
        }

        public Token(TokenType type, Double value)
        {
            this.type          = type;
            this.floatingValue = value;
        }

        public Token(TokenType type, Long value)
        {
            this.type         = type;
            this.integerValue = value;
        }

        public Token(TokenType type, String value)
        {
            this.type     = type;
            this.strValue = value;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("(");
            sb.append(type.toString());
            sb.append(":");

            if (boolValue != null)
                sb.append(boolValue.toString());
            else if (floatingValue != null)
                sb.append(floatingValue.toString());
            else if (integerValue != null)
                sb.append(integerValue.toString());
            else if (strValue != null)
                sb.append(strValue);

            sb.append(")");
                      
            return sb.toString();
        }
    }

    /**
     * Scan state keeps track of where we are in the source string and allows for pushing
     * back characters by decrementing the offset.
     */
    private static class ScanState
    {
        private String source;
        private int    pos;
        private int    max;

        ScanState(String src)
        {
            this.source = src;
            this.max    = src.length();
        }

        /**
         * return -1 at end of string, otherwise can cast to char
         */
        int nextChar()
        {
            if (pos >= max)
                return -1;
            else
                return (int) source.charAt(pos++);
        }

        int getPos()
        {
            return pos;
        }

        void backup()
        {
            pos = Math.max(--pos, 0);
        }
    }


    /**
     * lastToken supports pushing back a single scanned token (for LL(1) support, really)
     */
    private ScanState scan;
    private Token     lastToken;

    /**
     * Create a tokenizer that will scan the given source string
     */
    public Tokenizer(String src)
    {
        if (src == null)
            throw new IllegalArgumentException("Can't tokenize a null string");

        scan = new ScanState(src);
    }

    /**
     * Throws an exception with the given message with info about where in the actual source string
     * the error occurred
     */
    public void produceContextualError(String msg)
    {
        String source = scan.source.replaceAll("\t", " ");

        throw new IllegalStateException(msg + " in\n    " + source + "\nat: " + dashes(scan.pos - 2) + "^");
    }

    private static String dashes(int n)
    {
        StringBuilder sb = new StringBuilder();

        // stupid is good enough
        for (int i = 0; i < n; i++)
            sb.append("-");

        return sb.toString();
    }

    /**
     * Get next token and return true if the scanned type is the same as the required type; if
     * types don't match, push it back
     */
    public boolean optionalToken(TokenType req)
    {
        Token tok = nextToken();

        if (tok != null)
        {
            if (tok.type != req)
            {
                pushToken(tok);
                tok = null;
            }
        }

        return (tok != null);
    }

    /**
     * Get next token and if the type of it doesn't match what is required, then throw an error.
     */
    public Token requiredToken(TokenType req)
    {
        Token tok = nextToken();

        if ((tok == null) || (tok.type != req))
            produceContextualError("Expected " + req + " but got " + tok);

        return tok;
    }

    /**
     * Push back a token received via nextToken().  Only 1 pushback is allowed.
     */
    public void pushToken(Token token)
    {
        if (lastToken != null)
            produceContextualError("Attempt to push a token when a pushed token already exists");

        lastToken = token;
    }

    /**
     * Tokens and what starts them, etc.
     *
     *  * LPAREN:   (
     *  * RPAREN:   )
     *  * LANGLE:   <
     *  * RANGLE:   >
     *  * COMMA:    ,
     *  * DOT:      .
     *  * ALPHANUM: [a-zA-Z]        # boolean true/false are special case
     *  * BOOLEAN:  true false
     *  * STRING:   "               # no escaped/embedded quotes
     *  * INTEGER:  [0-9-+]         # + - only in leading position
     *  * FLOATING: integer and .   # like INTEGER but allow .INTEGER (w/o +-)
     *
     * Note that INTEGER/FLOATING must start with + or - or a digit
     */
    public Token nextToken()
    {
        Token         next = lastToken;
        StringBuilder sb;
        int           ch;

        if (next != null)
        {
            lastToken = null;
        }
        else
        {
            ch = nextRealChar();

            if (ch == -1)
                next = null;
            else if (ch == '(')
                next = new Token(TokenType.LPAREN);
            else if (ch == ')')
                next = new Token(TokenType.RPAREN);
            else if (ch == '<')
                next = new Token(TokenType.LANGLE);
            else if (ch == '>')
                next = new Token(TokenType.RANGLE);
            else if (ch == '.')
                next = new Token(TokenType.DOT);
            else if (ch == ',')
                next = new Token(TokenType.COMMA);
            else if ((ch == '-') || (ch == '+') || isDigit((char) ch))   // starting with . is not supported
                next = scanNumeric(ch);
            else if ((ch == '"') || (ch == '\''))
                next = scanString(ch);
            else if (!isAlpha(((char) ch)))
                produceContextualError("Unexpected character '" + ((char) ch) + "' at position " + scan.getPos());
            else  // must be identifier-like (which includes true/false for boolean...sigh)
                next = scanIdBoolean(ch);
        }

        return next;
    }

    /**
     * Return the first non-whitespace character
     */
    private int nextRealChar()
    {
        int ch;

        // skip whitespace
        while (((ch = scan.nextChar()) != -1) && Character.isWhitespace((char) ch))
            ;

        return ch;
    }

    /**
     * Scan the rest of what's presumed to be either an integer or a floating point number.  Numeric tokens
     * MUST start with a plus or minus character or a digit--a leading decimal point is not supported.
     */
    private Token scanNumeric(int ch)
    {
        boolean       dot = false;
        StringBuilder sb  = new StringBuilder();
        Token         next;

        // integer or floating
        // scan until "." or non-digit; if "." then scan until non-digit

        sb.append((char) ch);

        while ((ch = scan.nextChar()) != -1)
        {
            if (ch == '.')
            {
                if (dot)
                {
                    scan.backup();
                    break;
                }

                dot = true;
                sb.append('.');
            }
            else if (isDigit((char) ch))
            {
                sb.append((char) ch);
            }
            else
            {
                scan.backup();
                break;
            }
        }

        if (dot)
            next = new Token(TokenType.FLOATING, Double.parseDouble(sb.toString()));
        else
            next = new Token(TokenType.INTEGER, Long.parseLong(sb.toString()));

        return next;
    }

    /**
     * Scan to the closing double quote of a string and return the string
     */
    private Token scanString(int closeCh)
    {
        StringBuilder sb    = new StringBuilder();
        int           start = scan.pos;
        boolean       close = false;
        Token         next  = null;
        int           ch;

        sb = new StringBuilder();

        // scan to closing "; no support for escaping anything
        while ((ch = scan.nextChar()) != -1)
        {
            if (ch == closeCh)
            {
                next  = new Token(TokenType.STRING, sb.toString());
                close = true;
                break;
            }
            else
            {
                sb.append((char) ch);
            }
        }

        if (!close)
        {
            scan.pos = start;
            produceContextualError("Unexpected end of source while scanning for closing quote");
        }

        return next;
    }

    /**
     * Scan until non-alphanumeric character is found.  Check if the scanned string is a boolean true
     * or false, returning the appropriate value there, and if not boolean then return an ALPHANUM
     * token.
     */
    private Token scanIdBoolean(int ch)
    {
        StringBuilder sb    = new StringBuilder();
        Token         next  = null;
        String        alnum;

        sb = new StringBuilder();
        sb.append((char) ch);

        while ((ch = scan.nextChar()) != -1)
        {
            char cch = (char) ch;

            if (!(isAlpha(cch) || isDigit(cch)))
            {
                scan.backup();
                break;
            }
            else
            {
                sb.append(cch);
            }
        }

        alnum = sb.toString();

        // deal with true/false for boolean.  yuck
        if (BOOL_TRUE.equals(alnum))
            next = new Token(TokenType.BOOLEAN, Boolean.TRUE);
        else if (BOOL_FALSE.equals(alnum))
            next = new Token(TokenType.BOOLEAN, Boolean.FALSE);
        else
            next = new Token(TokenType.ALPHANUM, sb.toString());

        return next;
    }

    /**
     * Restrict things to UTF8/ASCII
     */
    private static boolean isAlpha(char ch)
    {
        return ( ((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z')) );
    }
    
    private static boolean isDigit(char ch)
    {
        return ((ch >= '0') && (ch <= '9'));
    }

    /**
     * Testing onliy
     */
    public static void main(String... args)
    {
        for (String a : args)
        {
            Tokenizer tz = new Tokenizer(a);
            Token     tk;

            while ((tk = tz.nextToken()) != null)
                System.out.println(tk.toString());
        }
    }
    
    
}
