package com.apixio.model.trace;

import com.apixio.model.commonparser.ApixioJSONParser;
import com.apixio.model.commonparser.SerDerObjectMapper;

import java.io.IOException;

public class TraceJSONParser
{
    public DocumentEntryJSONParser        entryParser;
    public DocumentTraceJSONParser        traceParser;
    public DocumentSummaryTraceJSONParser summaryParser;

    public TraceJSONParser()
    {
        entryParser   = new DocumentEntryJSONParser();
        traceParser   = new DocumentTraceJSONParser();
        summaryParser = new DocumentSummaryTraceJSONParser();
    }

    public class DocumentEntryJSONParser extends ApixioJSONParser<DocumentEntry>
    {
        public DocumentEntryJSONParser()
        {
            super(SerDerObjectMapper.InitializeType.Compact);
        }

        public DocumentEntry parse(String jsonString)
                throws IOException
        {
            return parse(jsonString, DocumentEntry.class);
        }
    }

    public class DocumentTraceJSONParser extends ApixioJSONParser<DocumentTrace>
    {
        public DocumentTraceJSONParser()
        {
            super(SerDerObjectMapper.InitializeType.Compact);
        }

        public DocumentTrace parse(String jsonString)
                throws IOException
        {
            return parse(jsonString, DocumentTrace.class);
        }
    }

    public class DocumentSummaryTraceJSONParser extends ApixioJSONParser<DocumentSummaryTrace>
    {
        public DocumentSummaryTraceJSONParser()
        {
            super(SerDerObjectMapper.InitializeType.Compact);
        }

        public DocumentSummaryTrace parse(String jsonString)
                throws IOException
        {
            return parse(jsonString, DocumentSummaryTrace.class);
        }
    }
}
