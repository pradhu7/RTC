package com.apixio.sdk.util;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.util.JsonFormat;

import com.apixio.sdk.protos.EvalProtos;
import com.apixio.sdk.util.Tokenizer.Token;
import com.apixio.sdk.util.Tokenizer.TokenType;

/**
 * Parses with a pidgin syntax that describes how a generic ECC is configured to evaluate an f(x).
 * The syntax is very much just a functional expression, like so:
 *
 *  pageWindows(signalPartialPatient("uuid", true))
 *
 * where there are the following syntax elements:
 *
 *  * function identifiers; e.g., pageWindows, signalPartialPatient
 *  * parens, commas
 *  * constants, for each supported type:  numeric, boolean, string
 *
 * Note that the top level construct is a comma separated list of arguments, as it's supposed to
 * represent the parameters to the f(x).
 *
 * This is a hand-crafted parser just because...
 */
public class FxEvalParser
{
    private static JsonFormat.Printer formatter = JsonFormat.printer(); //.omittingInsignificantWhitespace();

    /**
     *
     */
    public static EvalProtos.ArgList parse(String src)
    {
        return parseCsvArgs(new Tokenizer(src));
    }

    /**
     * Assumes current position in scan is immediately before first arg in a possibly csv-separated list
     * of args.
     */
    private static EvalProtos.ArgList parseCsvArgs(Tokenizer tz)
    {
        List<EvalProtos.Arg> arglist = new ArrayList<>();
        Token                next    = tz.nextToken();

        if (next == null)
            tz.produceContextualError("Hit EOS while scanning arglist");

        if (next.type == TokenType.RPAREN)  // empty list
        {
            tz.pushToken(next);
        }
        else
        {
            EvalProtos.Arg  arg;

            tz.pushToken(next);

            arglist.add(parseOneArg(tz));

            while (true)
            {
                next = tz.nextToken();

                if (next == null)
                    break;

                if (next.type == TokenType.RPAREN)
                {
                    tz.pushToken(next);  // let parseFunction pick it up
                    break;
                }
                else if (next.type == TokenType.COMMA)
                {
                    arg = parseOneArg(tz);  // null if end of list of args

                    if (arg != null)
                        arglist.add(arg);
                }
                else
                {
                    tz.produceContextualError("Expected comma or ) but got " + next);
                }
            }
        }

        return ArgBuilder.makeArgList(arglist);
    }

    /**
     * Parses:  alnum "(" [ (function | const) [, ...] ] ")"
     *
     * where
     *
     *   const:     integer | floating | string | boolean
     */
    private static EvalProtos.Arg parseFunction(Tokenizer tz)
    {
        Token              tok = tz.requiredToken(TokenType.ALPHANUM);
        EvalProtos.ArgList arglist;

        tz.requiredToken(TokenType.LPAREN);
        arglist = parseCsvArgs(tz);
        tz.requiredToken(TokenType.RPAREN);

        return ArgBuilder.makeAccessorCall(tok.strValue, arglist);
    }

    /**
     * Parses:  const | function
     */
    private static EvalProtos.Arg parseOneArg(Tokenizer tz)
    {
        Token          tok = tz.nextToken();
        EvalProtos.Arg arg = null;

        if (tok.type == TokenType.ALPHANUM)
        {
            tz.pushToken(tok);
            return parseFunction(tz);
        }
        else if ((arg = makeConst(tok)) != null)
        {
            return arg;
        }
        else if (tok.type == TokenType.COMMA)
        {
            tz.produceContextualError("Unexpected comma");
            return null;
        }
        else
        {
            tz.pushToken(tok);
            return null;
        }
    }

    /**
     * Returns non-null if the tokenizer returns a constant type
     */
    private static EvalProtos.Arg makeConst(Token tok)
    {
        if (tok != null)
        {
            TokenType type = tok.type;

            if (type == TokenType.BOOLEAN)
                return ArgBuilder.makeBooleanValue(tok.boolValue);
            else if (type == TokenType.STRING)
                return ArgBuilder.makeStringValue(tok.strValue);
            else if (type == TokenType.INTEGER)
                return ArgBuilder.makeIntValue(tok.integerValue.intValue()); //!! hack due to mismatch of tokenizer assuming long
            else if (type == TokenType.FLOATING)
                return ArgBuilder.makeDoubleValue(tok.floatingValue);
        }

        return null;
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
