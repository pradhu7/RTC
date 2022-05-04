package com.apixio.model.aligner;

import java.io.IOException;
import java.io.Serializable;

import com.apixio.model.event.EventType;
import com.apixio.model.patient.Patient;

public class AlignerType implements Serializable
{
    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = -5363532069169965177L;

    public static final String OBJTYPE = "MAT";

    private String            id; // really an XUUID
    private EventType         eventType;
    private Patient           apo;
    private AlignerConfigInfo alignerConfigInfo;

    private AlignerType()
    {
    }

    /**
     * Instantiates a new AlignerType.
     *
     * @param builder the builder
     */
    private AlignerType(Builder builder)
    {
        this.id                = builder.id;
        this.eventType         = builder.eventType;
        this.apo               = builder.apo;
        this.alignerConfigInfo = builder.alignerConfigInfo;
    }

    public String getId()
    {
        return id;
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public Patient getAPO()
    {
        return apo;
    }

    public AlignerConfigInfo getAlignerConfigInfo()
    {
        return alignerConfigInfo;
    }

    /**
     * Debug only
     */
    public String toString()
    {
        return ("[AlignerTYpe: " +
                "; id=" + id +
                "; eventType=" + eventType +
                "; apo=" + apo +
                "; alignerConfigInfo=" + alignerConfigInfo +
                "]"
        );
    }

    /**
     * The Class Builder.
     */
    public static class Builder
    {
        private static AlignerTypeJSONParser parser = new AlignerTypeJSONParser();

        private String            id;
        private EventType         eventType;
        private Patient           apo;
        private AlignerConfigInfo alignerConfigInfo;

        public Builder setId(String id)
        {
            this.id = id;
            return this;
        }

        public Builder setEventType(EventType eventType)
        {
            this.eventType = eventType;
            return this;
        }

        public Builder setAPO(Patient apo)
        {
            this.apo = apo;
            return this;
        }

        public Builder setAlignerConfigInfo(AlignerConfigInfo alignerConfigInfo)
        {
            this.alignerConfigInfo = alignerConfigInfo;
            return this;
        }

        /**
         * Builds the AlignerType.
         *
         * @return the AlignerType
         */
        public AlignerType build()
        {
            return new AlignerType(this);
        }

        /**
         * Builds the AlignerType.
         *
         * @param json the json
         * @return the metric
         * @throws IOException          Signals that an I/O exception has occurred.
         */
        public AlignerType build(String json)
                throws IOException
        {
            return parser.parseAlignerType(json);
        }
    }
}
