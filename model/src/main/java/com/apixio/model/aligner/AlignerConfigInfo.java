package com.apixio.model.aligner;

import java.io.IOException;
import java.io.Serializable;

import org.joda.time.DateTime;

public class AlignerConfigInfo implements Serializable
{
    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = -5363532069169965177L;

    private String    name;
    private String    expression;
    private String    classNameUri;
    private DateTime  createdAt;
    private String    batch;

    private AlignerConfigInfo()
    {
    }

    public String getName()
    {
        return name;
    }

    public String getExpression()
    {
        return expression;
    }

    public String getClassNameUri()
    {
        return classNameUri;
    }

    public DateTime getCreatedAt()
    {
        return createdAt;
    }

    public String getBatch()
    {
        return batch;
    }

    /**
     * Instantiates a new AlignerConfigInfo.
     *
     * @param builder the builder
     */
    private AlignerConfigInfo(Builder builder)
    {
        this.name         = builder.name;
        this.expression   = builder.expression;
        this.classNameUri = builder.classNameUri;
        this.createdAt    = builder.createdAt;
        this.batch        = builder.batch;
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("AlignerConfigInfo: " +
                "; name=" + name +
                "; expression=" + expression +
                "; classNameUri=" + classNameUri +
                "; createdAt=" + createdAt +
                "; batch=" + batch +
                "]"
        );
    }

    /**
     * The Class Builder.
     */
    public static class Builder
    {
        private static AlignerConfigInfoJSONParser parser = new AlignerConfigInfoJSONParser();

        private String     name;
        private String     expression;
        private String     classNameUri;
        private DateTime   createdAt;
        private String     batch;

        public Builder setName(String name)
        {
            this.name = name;
            return this;
        }

        public Builder setExpression(String expression)
        {
            this.expression = expression;
            return this;
        }

        public Builder setClassNameUri(String classNameUri)
        {
            this.classNameUri = classNameUri;
            return this;
        }

        public Builder setCreatedAt(DateTime createdAt)
        {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setBatch(String batch)
        {
            this.batch = batch;
            return this;
        }

        public AlignerConfigInfo build()
        {
            return new AlignerConfigInfo(this);
        }

        /**
         * Builds the AlignerConfigInfo.
         *
         * @param json the json
         * @return the metric
         * @throws IOException          Signals that an I/O exception has occurred.
         */
        public AlignerConfigInfo build(String json)
                throws IOException
        {
            return parser.parseAlignerConfigInfo(json);
        }
    }
}
