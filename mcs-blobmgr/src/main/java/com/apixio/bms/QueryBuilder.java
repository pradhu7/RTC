package com.apixio.bms;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.apixio.bms.Query.All;
import com.apixio.bms.Query.Expression;
import com.apixio.bms.Query.Limit;
import com.apixio.bms.Query.Operator;
import com.apixio.bms.Query.OrderBy;
import com.apixio.bms.Query.Term;
import com.apixio.restbase.util.DateUtil;

/**
 * QueryBuilder helps construct a Query that represents a parsed/built query on
 * metadata (including JSON in the 'extra' field) that will be converted to a
 * valid MariaDb SQL query to select Blobs.
 *
 * Queries are built using by using methods in this class to build up the tree
 * of operators and arguments (a "term").
 *
 * Use model for building queries:
 *
 *  QueryBuilder qb = new QueryBuilder();
 *  qb.rootTerm(qb.and(qb.eq(...), qb.neq(...)));
 *  qb.limit(1);
 *  qb.orderBy("???");
 *  SqlStatement sql = qb.build().generateSql();
 *
 * (Yes, that model is a little awkward...)
 *
 * Note that this builder class and the model around it are a bit odd in that
 * we know we're need to create SQL which will only be used by MariaDbStorage
 * but QueryBuilder is exposed publicly so the design tries to separate to some
 * degree the client-facing needs and the implementation needs.  The orderBy()
 * method is also blurring the line between client and impl as client needs to
 * know /something/ about the details as it passed in field name (this whole
 * "order by" is a bit odd as a client that wants to order by something would
 * have to know that the query would produce multiple blobs and the only
 * meaningful "order by" would be by version or date-created...let's ignore
 * this for now but keep the concept as a placeholder for future feature).
 */
public class QueryBuilder
{
    /**
     * Declaration of valid oper/type for use by byGeneric() creation of Terms.  Keep in sync
     * with compile-time list valid combination methods below.
     */
    private static Set<String> validOpsTypes = new HashSet<>();
    static
    {
        addValidOp(Operator.EQ, Integer.class);
        addValidOp(Operator.EQ, Double.class);
        addValidOp(Operator.EQ, String.class);
        addValidOp(Operator.EQ, Boolean.class);

        addValidOp(Operator.NEQ, Integer.class);
        addValidOp(Operator.NEQ, Double.class);
        addValidOp(Operator.NEQ, String.class);
        addValidOp(Operator.NEQ, Boolean.class);

        addValidOp(Operator.LIKE, String.class);
        addValidOp(Operator.NLIKE, String.class);

        addValidOp(Operator.LT, Integer.class);
        addValidOp(Operator.LT, Double.class);
        addValidOp(Operator.LT, Date.class);
        addValidOp(Operator.LTE, Integer.class);
        addValidOp(Operator.LTE, Double.class);

        addValidOp(Operator.GT, Integer.class);
        addValidOp(Operator.GT, Double.class);
        addValidOp(Operator.GT, Date.class);
        addValidOp(Operator.GTE, Integer.class);
        addValidOp(Operator.GTE, Double.class);

        // allow only string right now until REST code can interpret query params
        // as something besides a string...
        addValidOp(Operator.CONTAINS, String.class);
    }

    private static void addValidOp(Operator op, Class<?> clz)
    {
        validOpsTypes.add(opClassToString(op, clz));
    }

    private static boolean isValidOp(Operator op, Class<?> clz)
    {
        return validOpsTypes.contains(opClassToString(op, clz));
    }

    private static String opClassToString(Operator op, Class<?> clz)
    {
        return op.toString() + "_" + clz.getName();
    }

    /**
     * fields
     */
    private Term    rootTerm;
    private OrderBy orderBy;
    private Limit   limit;
    private String  extra1Prefix;
    private String  extra2Prefix;

    /**
     * Only valid type/operator combinations are exposed here.  This must be kept in sync
     * with what's added to validOpsTypes
     */
    public static Term eq(String field, Integer val)    {   return new Term(field, Operator.EQ, val);    }
    public static Term eq(String field, Double val)     {   return new Term(field, Operator.EQ, val);    }
    public static Term eq(String field, String val)     {   return new Term(field, Operator.EQ, val);    }
    public static Term eq(String field, Boolean val)    {   return new Term(field, Operator.EQ, val);    }

    public static Term neq(String field, Integer val)   {   return new Term(field, Operator.NEQ, val);   }
    public static Term neq(String field, Double val)    {   return new Term(field, Operator.NEQ, val);   }
    public static Term neq(String field, String val)    {   return new Term(field, Operator.NEQ, val);   }
    public static Term neq(String field, Boolean val)   {   return new Term(field, Operator.NEQ, val);   }

    public static Term like(String field, String val)   {   return new Term(field, Operator.LIKE, val);  }
    public static Term nlike(String field, String val)  {   return new Term(field, Operator.NLIKE, val); }

    public static Term lt(String field, Integer val)    {   return new Term(field, Operator.LT, val);    }
    public static Term lt(String field, Double val)     {   return new Term(field, Operator.LT, val);    }
    public static Term lt(String field, Date val)       {   return new Term(field, Operator.LT, DateUtil.dateToIso8601(val)); }
    public static Term lte(String field, Integer val)   {   return new Term(field, Operator.LTE, val);   }
    public static Term lte(String field, Double val)    {   return new Term(field, Operator.LTE, val);   }

    public static Term gt(String field, Integer val)    {   return new Term(field, Operator.GT, val);    }
    public static Term gt(String field, Double val)     {   return new Term(field, Operator.GT, val);    }
    public static Term gt(String field, Date val)       {   return new Term(field, Operator.GT, DateUtil.dateToIso8601(val)); }
    public static Term gte(String field, Integer val)   {   return new Term(field, Operator.GTE, val);   }
    public static Term gte(String field, Double val)    {   return new Term(field, Operator.GTE, val);   }

    public static Term and(Term... terms)               {   return new Expression(Operator.AND, terms);  }
    public static Term and(List<Term> terms)            {   return new Expression(Operator.AND, terms);  }
    public static Term or(Term... terms)                {   return new Expression(Operator.OR, terms);   }
    public static Term or(List<Term> terms)             {   return new Expression(Operator.OR, terms);   }

    public static Term all()                            {   return new All();                            }

    /**
     * Do the above generically--checking done at runtime rather than compile-time
     */
    public static Term byGeneric(String field, Operator op, Object val)
    {
        if (!isValidOp(op, val.getClass()))
            throw new IllegalArgumentException("Unsupported combination of op and argtype:  [" + op + ", " + val.getClass().getName() + "]");

        if ((op == Operator.AND) || (op == Operator.OR))
            throw new IllegalArgumentException("Can't use byGeneric for AND or OR operators");

        return new Term(field, op, val);
    }

    /**
     * Set the root term of the query.  This MUST be done by the client before a build()
     */
    public QueryBuilder term(Term root)
    {
        rootTerm = root;

        return this;
    }

    public QueryBuilder withPrefixes(String extra1Prefix, String extra2Prefix)
    {
        this.extra1Prefix = extra1Prefix;
        this.extra2Prefix = extra2Prefix;

        return this;
    }

    public Query build()
    {
        if (rootTerm == null)
            throw new IllegalStateException("Attempt to build Query with no terms");

        return MariaDbQuery.build(rootTerm, orderBy, limit, extra1Prefix, extra2Prefix);
    }

}
