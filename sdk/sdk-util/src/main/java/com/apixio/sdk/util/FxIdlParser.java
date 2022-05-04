package com.apixio.sdk.util;

import java.util.function.Supplier;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.util.JsonFormat;

import com.apixio.sdk.protos.FxProtos.FxDef;
import com.apixio.sdk.protos.FxProtos.FxType;
import com.apixio.sdk.util.Tokenizer.Token;
import com.apixio.sdk.util.Tokenizer.TokenType;

/**
 * Parses pidgin IDL that declares an f(x) and builds a protobuf FxDef corresponding to it.  This
 * is an intentionally limited parser and is intended to recognize only a useful subset of possible
 * f(x) definitions.
 *
 * The (loose) syntax recognized by this parser is:
 *
 *  fxdef :: type id "(" type "," ... ")"
 *  type  :: "list" "<" fullid ">" |
 *           "int" | "long" | ...
 *  id     :: alpha [alphanum ...]
 *  fullid :: id [ "." id ...]              # for java classnames
 *
 * Examples:
 *
 *  list<com.apixio.ensemble.ifc.Signal> extractFeature(list<com.apixio.ensemble.igf.PageWindow>)
 *  int xton(double,int)
 *
 * This is a hand-crafted parser just because...
 */
public class FxIdlParser
{
    private static JsonFormat.Printer formatter = JsonFormat.printer(); //.omittingInsignificantWhitespace();

    /**
     * All keywords recognized
     */
    private final static String KEY_BOOLEAN = "boolean";
    private final static String KEY_DOUBLE  = "double";
    private final static String KEY_FLOAT   = "float";
    private final static String KEY_INT     = "int";
    private final static String KEY_LIST    = "list";
    private final static String KEY_LONG    = "long";
    private final static String KEY_STRING  = "string";

    /**
     * Method references for cleaner code, easier additions
     */
    private static Map<String, Supplier<FxType>> fxMethods = new HashMap<>();
    static
    {
        fxMethods.put(KEY_BOOLEAN, FxBuilder::makeBooleanType);
        fxMethods.put(KEY_DOUBLE,  FxBuilder::makeDoubleType);
        fxMethods.put(KEY_FLOAT,   FxBuilder::makeFloatType);
        fxMethods.put(KEY_INT,     FxBuilder::makeIntType);
        fxMethods.put(KEY_LONG,    FxBuilder::makeLongType);
        fxMethods.put(KEY_STRING,  FxBuilder::makeStringType);
    }

    /**
     * Top level method to parse and return FxDef.  Syntax is:
     *
     *  {type} {id} ( [ {type} [, {type} ...] ] )
     *
     * and the code does LL(1) parsing of it
     */
    public static FxDef parse(String src)
    {
        Tokenizer    tz     = new Tokenizer(src);
        List<FxType> params = new ArrayList<>();
        Token        tk;
        FxType       fxReturn;
        FxDef        fxDef;
        String       fxName;

        fxReturn = tryType(tz, true);   // we must have a return type

        // we must have a function name
        tk = tz.nextToken();
        if ((tk == null) || (tk.type != TokenType.ALPHANUM))
            tz.produceContextualError("Expected ID but got " + tk);
        fxName = tk.strValue;

        // we must have a param list (which could be empty); param list like: ( type , ... )
        tz.requiredToken(TokenType.LPAREN);
        while (true)
        {
            FxType ptype = tryType(tz, false);

            if (ptype != null)
                params.add(ptype);
            else
                break;

            if (!tz.optionalToken(TokenType.COMMA))
                break;
        }
        tz.requiredToken(TokenType.RPAREN);

        // we have everything to build fxdef
        return FxBuilder.makeFxDef(fxName, fxReturn, params);
    }

    /**
     * Scan next token(s) for a type and create an FxType from it and return it.  A type can
     * be either a primitive (as given in the fxMethods map above) or a "list" of other types,
     * or a well-formed identifier.  A well-formed identifer is like a full Java classname
     * (dots separating alphanumeric identifiers).
     *
     * This supports nested types such as "list<list<int>>" as it properly does recursion
     */
    private static FxType tryType(Tokenizer tz, boolean required)
    {
        Token  tok    = tz.nextToken();
        FxType fxType = null;

        if (tok == null)
            tz.produceContextualError("EOS when expecting type");

        if (tok.type == TokenType.ALPHANUM)
        {
            Supplier<FxType> sup = fxMethods.get(tok.strValue);

            if (sup != null)  // i.e., a primitive
            {
                fxType = sup.get();
            }
            else if (tok.strValue.equals(KEY_LIST))
            {
                tz.requiredToken(TokenType.LANGLE);
                fxType = FxBuilder.makeSequenceType(tryType(tz, true));  // greedy making of seq type
                tz.requiredToken(TokenType.RANGLE);
            }
            else  // assume classname, so we need to push back the token and get a full id
            {
                tz.pushToken(tok);
                fxType = FxBuilder.makeRefStructType(requireFullId(tz));
            }
        }
        else if (!required)
        {
            tz.pushToken(tok);
        }
        else
        {
            tz.produceContextualError("Requiring type but got " + tok);
        }

        return fxType;
    }

    /**
     * Scans next token(s) for a "full identifier" which is really just a well-formed Java
     * classname (dot-separated list of alphanumeric identifiers).
     */
    private static String requireFullId(Tokenizer tz)
    {
        Token         tok = tz.nextToken();
        StringBuilder sb;

        if ((tok == null) || (tok.type != TokenType.ALPHANUM))
            tz.produceContextualError("Expected alphanumeric but got " + tok);

        sb = new StringBuilder();
        sb.append(tok.strValue);

        // look for "." id ...
        while (true)
        {
            if (!tz.optionalToken(TokenType.DOT))
                break;

            sb.append(".");

            tok = tz.nextToken();
            if ((tok == null) || (tok.type != TokenType.ALPHANUM))
                tz.produceContextualError("Expecting alphanumeric during full id but got " + tok);

            sb.append(tok.strValue);
        }

        return sb.toString();
    }

    /**
     * Test only
     */
    public static void main(String... args) throws Exception
    {
        for (String a : args)
        {
            System.out.println(formatter.print(parse(a)));
        }
    }
    
}
