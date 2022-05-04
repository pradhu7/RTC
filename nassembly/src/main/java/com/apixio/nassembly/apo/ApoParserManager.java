package com.apixio.nassembly.apo;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.*;
import com.apixio.nassembly.documentmeta.DocMetadataKeys;

import com.apixio.converters.apx.parser.APXParser;
import com.apixio.converters.axm.parser.AXMParser;
import com.apixio.converters.base.Parser;
import com.apixio.converters.ccda.parser.CCDAParser;
import com.apixio.converters.ccr.parser.CCRParser;
import com.apixio.converters.ecw.parser.ECWParser;
import com.apixio.converters.fhir.parser.FHIRDSTU3Parser;
import com.apixio.converters.json.parser.PatientObjectParser;
import com.apixio.converters.text.extractor.TextExtractionUtils;
import com.apixio.converters.xml.creator.XMLUtils;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.model.file.ApxPackagedStream;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import com.apixio.model.nassembly.AssemblyContext;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Patient;
import com.apixio.nassembly.patient.ParserUtil;
import org.apache.commons.io.IOUtils;

public class ApoParserManager implements Serializable
{

    public static Patient parsePackagedStream(ApxPackagedStream inputFile, String sourceFileName, Object customerPropertiesObj, List<String> fileTypesText, AssemblyContext ac) throws Exception {
        CustomerProperties cp = (CustomerProperties) customerPropertiesObj;
        String primaryAssignAuthority = cp.getPrimaryAssignAuthority(ac.pdsId());
        if (primaryAssignAuthority == null)
            throw new Exception("primaryAssignAuthority cannot be null");

        if (inputFile == null)
            throw new Exception("Input given is null for parser");

        ApxCatalog apxCatalog = inputFile.getCatalog();

        if (apxCatalog == null)
            throw new Exception("Catalog object cannot be null");

        String fileType = apxCatalog.getCatalogEntry().getFileFormat();

        byte[] fileContentBytes = IOUtils.toByteArray(inputFile.getFileContent());

        if (fileType == null)
            throw new Exception("File type not known. Cannot parse");

        Parser parser;
        Patient patient;

        if (fileType.equalsIgnoreCase("AXM"))
        {
            parser = new AXMParser();
        }else if (fileType.equalsIgnoreCase("CCR"))
        {
            parser = new CCRParser();
        }
        // TODO: Eventually type sould be detected from the payload itself
        // Backwards compatible for Loader setting type to CCD
        else if (fileType.equalsIgnoreCase("CCD") || fileType.equalsIgnoreCase("CCDA"))
        {
            parser = new CCDAParser();
        }else if (fileType.equalsIgnoreCase("ECW"))
        {
            parser = new ECWParser();
        }else if (fileType.equalsIgnoreCase("APO"))
        {
            parser = new PatientObjectParser();
        }else if (fileType.equalsIgnoreCase("FHIR"))
        {
            parser = new FHIRDSTU3Parser();
        }
        else
        {
            parser = new APXParser();
            APXParser p = (APXParser) parser;

            p.setDocuments(APXParser.keyConstantFileContent, fileContentBytes);
            p.setDocuments(APXParser.keyConstantFileName, inputFile.getFileName());
        }

        // Init Parser
        parser.setPrimaryAssignAuthority(primaryAssignAuthority);
        parser.setSourceDocumentName(sourceFileName);

        parser.parse(new ByteArrayInputStream(fileContentBytes), apxCatalog.getCatalogEntry());
        patient = parser.getPatient();

        Iterator <Document> docIterator = patient.getDocuments().iterator();
        if (docIterator.hasNext())
        {
            Document doc = docIterator.next();
            doc.setMetaTag("pipeline.version", ac.codeVersion());
        }

        ApxCatalog.CatalogEntry  catalogEntry = inputFile.getCatalog().getCatalogEntry();
        String computedFileType = ParserUtil.getFileTypeFromDocumentOrCatalog(catalogEntry, patient);

        // If the computed filed type is different than what we entered with, update the fileFormat and content in the apxPackage for downstream processes
        if (!computedFileType.equals(fileType)) {
            fileContentBytes = ParserUtil.getDocumentContent(patient).get();
            catalogEntry.setFileFormat(computedFileType);
        }


        if (ParserUtil.isGivenType(computedFileType, fileTypesText))
        {
            //Extract Text
            Iterable<Document> documents = patient.getDocuments();
            if (documents != null)
            {
                Iterator<Document> it = documents.iterator();
                if (it.hasNext())
                {
                    // There should be only one file to process. So getting the 0th element.
                    Document document = it.next();

                    Map<String, String> metadata = document.getMetadata();
                    if (metadata == null)
                        metadata = new HashMap<>();

                    //Aspose text extraction..
                    String xmlContent = XMLUtils.getXMLRepresentation(new ByteArrayInputStream(fileContentBytes), computedFileType);
                    if (xmlContent != null && !xmlContent.trim().equals(""))
                    {
                        document.setStringContent((document.getStringContent() == null ? "" : document.getStringContent()) + xmlContent);
                        // TODO: Hack for metadata
                        metadata.put(DocMetadataKeys.STRING_CONTENT_TS().toString(), Long.toString(new Date().getTime()));
                        metadata.put(DocMetadataKeys.DOC_CACHE_FORMAT().toString(), "protobuf_base64");
                        metadata.put(DocMetadataKeys.DOC_CACHE_LEVEL().toString(), "doc");
                        metadata.put(DocMetadataKeys.DOC_CACHE_TS().toString(), Long.toString(new Date().getTime()));
                    }


                    String extractedBytes = TextExtractionUtils.extractText(new ByteArrayInputStream(fileContentBytes), computedFileType);
                    metadata.put(DocMetadataKeys.TEXT_EXTRACTED().toString(), extractedBytes);
                    if (!extractedBytes.isEmpty())
                    {
                        metadata.put(DocMetadataKeys.TEXT_EXTRACTED_TS().toString(), Long.toString(new Date().getTime()));
                    }
                    document.setMetadata(metadata);

                    // TODO: do we want "totalPages" for text documents? right now we consider them always as 1 page
                }
            }
        }

        // Because we are working with a non-resettable stream and because file content may have changed, reset the file content
        inputFile.setFileContent(new ByteArrayInputStream(fileContentBytes));

        return patient;
    }
}