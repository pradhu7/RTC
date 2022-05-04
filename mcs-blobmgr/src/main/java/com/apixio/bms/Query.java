package com.apixio.bms;

import java.util.Arrays;
import java.util.List;

import com.apixio.datasource.springjdbc.SqlStatement;

/**
 * Abstract generic Query that's database-independent
 */
public abstract class Query
{
    /**
     * All Operators are binary operators (the "not" operation is a separate operator variant)
     * with the exception of the "grouping" ones (and, or).  Note that with only "and" and "or"
     * as logical operators, De Morgan's Law might need to be used for compound negation:
     *
     *  "!(a || b)" can be written as "!a and !b"
     */
    public enum Operator
    {
        // standard binary operators; note that MariaDB does case-insenstive string
        // comparison by default so no operators for that are needed

        EQ("="), NEQ("!="),                     // all datatypes
        LIKE("like"), NLIKE("not like"),        // only strings
        LT("<"), LTE("<="), GT(">"), GTE(">="), // only numeric

        CONTAINS("contains"),                   // special case in code; sqlOp is ignored

        // do these later:  BEFORE, NBEFORE, AFTER, NAFTER

        // grouping operators
        AND("and"), OR("or");

        public String toSql()               { return sqlOp;        }

        private Operator(String sqlOp)      { this.sqlOp = sqlOp;  }
        private Operator()                  { this.sqlOp = null;   }
        private String sqlOp;
    }

    /**
     * Declares the fields in the "main" blob properties (i.e., those that aren't
     * declared as metadata at at higher level, and those that aren't in the extra
     * JSON data) that can be queried.  These enums declare the external name of the
     * field (i.e., the one that bms clients reference).
     *
     * Expected use pattern is for higher (REST) level code to do
     *
     *   QueryBuilder.eq(CoreField.valueOf(param.toUpperCase()), ...)
     *
     * although it should be note that not all combinations of CoreField enum
     * and value type make sense (e.g., CoreField.DELETED shouldn't be compare
     * to a Double).  Also note that it is valid/okay to pass in as the String
     * field name CoreField.toString() as the builder checks the string form
     * of the core field name
     */
    public enum CoreField { CREATOR, DELETED, S3PATH, CREATEDAT }

    /**
     *
     */
    public static class OrderBy
    {
        public OrderBy(String field, boolean ascending)
        {
        }

        public String getByField()
        {
            return null;
        }

        public boolean isAscending()
        {
            return false;
        }
    }
        
    /**
     *
     */
    public static class Limit
    {
        public Limit(int n)
        {
        }

        public int getLimit()
        {
            return 0;
        }
    }

    /**
     * A Term represents a single test of a single metadata field against a value using a particular
     * operator.  Values can only be constants in this model.  Terms can then be combined into a
     * hierarchy of an expreession using "and" and "or".
     */
    public static class Term
    {
        protected String   field;
        protected Operator op;
        protected Object   val;

        /**
         * For constructing leaf-level terms
         */
        Term(String field, Operator op, Object val)
        {
            if (isEmpty(field))
                throw new IllegalArgumentException("Field name can't be empty");
            else if (val == null)
                throw new IllegalArgumentException("Value can't be empty for field " + field);

            this.field = field;
            this.op    = op;
            this.val   = val;
        }

        // special case
        Term()
        {
        }

        /**
         * For constructing inner nodes
         */
        Term(Operator op)
        {
            this.op = op;
        }

        /**
         * debug only
         */
        @Override
        public String toString()
        {
            return "(" + field + " " + op + " " + val + ")";
        }
    }

    // ################################################################
    // ################################################################
    // # (later) Needed concrete classes for each of the datatypes (of values) supported??
    // ################################################################
    // ################################################################

    /**
     * "Expression" isn't really the best word, but means the next level "up" from
     * Terms--basically an atomic Term that includes Terms as units.
     */
    public static class Expression extends Term
    {
        protected List<Term> terms;

        Expression(Operator op, List<Term> terms)
        {
            super(op);

            if (terms.size() == 0)
                throw new IllegalArgumentException("Grouping Operator " + op + " requires a non-empty list of terms");

            for (Term term : terms)
            {
                if (term == null)
                    throw new IllegalArgumentException("Grouping Operator " + op + " requires all terms to be non-null");
            }

            this.terms = terms;
        }

        Expression(Operator op, Term... terms)
        {
            this(op, Arrays.asList(terms));
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("(");

            for (Term term : terms)
            {
                if (sb.length() > 1)
                    sb.append(" " + op + " ");
                sb.append(term.toString());
            }

            sb.append(")");

            return sb.toString();
        }
    }

    public static class All extends Term
    {
        All()
        {
        }
    }

    /**
     * fields
     */
    protected Term    rootTerm;
    protected OrderBy orderBy;
    protected Limit   limit;
    protected String  extra1Prefix;     // if field starts w/ this, then look in extra_1
    protected String  extra2Prefix;     // if field starts w/ this, then look in extra_2

    /**
     *
     */
    protected Query(Term root, OrderBy orderBy, Limit limit, String extra1Prefix, String extra2Prefix)
    {
        this.rootTerm = root;
        this.orderBy  = orderBy;
        this.limit    = limit;

        this.extra1Prefix = extra1Prefix;
        this.extra2Prefix = extra2Prefix;
    }

    /**
     * Note that this method is not public
     */
    abstract SqlStatement generateSql();

    /**
     * Convert from enum string to actual enum, returning null if there's no match
     */
    public static CoreField isCore(String field)
    {
        try
        {
            return Query.CoreField.valueOf(field.toUpperCase());
        }
        catch (Exception x)
        {
            return null;
        }
    }

    /**
     * not sure i want these public...
     */
    public Term getRootTerm()
    {
        return rootTerm;
    }

    public OrderBy getOrderBy()
    {
        return orderBy;
    }

    public Limit getLimit()
    {
        return limit;
    }

    public String getExtra1Prefix()
    {
        return extra1Prefix;
    }

    public String getExtra2Prefix()
    {
        return extra2Prefix;
    }

    /**
     * private implementation
     */
    private static boolean isEmpty(String s)
    {
        return (s == null) || (s.trim().length() == 0);
    }

}
