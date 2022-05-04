package com.apixio.bizlogic.patient.assembly.documentMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apixio.bizlogic.patient.assembly.PatientAssembler;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.bizlogic.patient.assembly.merge.ClinicalActorMerge;
import com.apixio.bizlogic.patient.assembly.merge.DocumentMetaMerge;
import com.apixio.bizlogic.patient.assembly.merge.EncounterMerge;
import com.apixio.bizlogic.patient.assembly.merge.ParsingDetailMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.merge.SourceMerge;
import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.model.assembly.Assembler.PartEnvelope;
import com.apixio.model.assembly.Part;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Patient;

public class DocumentMetaAssembler extends PatientAssembler
{
    @Override
    @SuppressWarnings("unchecked")
    public List<Part<Patient>> separate(Patient apo)
    {
        List<Part<Patient>> parts = new ArrayList<>();

        List<Document> documents = (List<Document>) apo.getDocuments();
        if ((documents == null || documents.isEmpty()))
            return parts; // empty list

        for (Document d : documents)
        {
            Document separatedDocument = separate(d);
            Patient  separatedApo      = separate(apo, separatedDocument);

            separatedApo.setPatientId(apo.getPatientId());
            AssemblerUtility.addExternalIDs(apo, separatedApo);

            // single document separators
            parts.add(new Part(PatientAssembly.PatientCategory.DOCUMENT_META.getCategory(), getPartID(separatedDocument), separatedApo));
        }

        return parts;
    }

    @Override
    public PartEnvelope<Patient> merge(MergeInfo mergeInfo, PartEnvelope<Patient> p1, PartEnvelope<Patient> p2)
    {
        return new PartEnvelope<Patient>(merge(new PatientSet(p1.part, p2.part)), 0L);
    }

    @Override
    public void prerequisiteMerges(PatientSet patientSet)
    {
        // Need to make sure we do these merges, since the document meta data merge
        // will use merge down metadata that is set by these merges

        ParsingDetailMerge.merge(patientSet);
        SourceMerge.merge(patientSet);
        ClinicalActorMerge.mergeClinicalActors(patientSet);
        EncounterMerge.mergeEncounters(patientSet);
    }

    @Override
    public void actualMerge(PatientSet patientSet)
    {
        DocumentMetaMerge.mergeDocuments(patientSet);
    }

    private String getPartID(Document document)
    {
        return document.getInternalUUID().toString();
    }

    /**
     * Separated Document stripped of content and left with only separated metadata
     */
    private Document separate(Document document)
    {
        Document separatedDocument = new Document();
        separatedDocument.setDocumentTitle(document.getDocumentTitle());
        separatedDocument.setDocumentDate(document.getDocumentDate());
        separatedDocument.setDocumentContents(document.getDocumentContents());

        separatedDocument.setInternalUUID(document.getInternalUUID());
        separatedDocument.setSourceId(document.getSourceId());
        separatedDocument.setOriginalId(document.getOriginalId());

        Map<String, String> metadata = document.getMetadata();
        separatedDocument.setMetadata(metadata);

        separatedDocument.setPrimaryClinicalActorId(document.getPrimaryClinicalActorId());
        separatedDocument.setSupplementaryClinicalActors(document.getSupplementaryClinicalActorIds());
        separatedDocument.setSourceEncounter(document.getSourceEncounter());
        separatedDocument.setParsingDetailsId(document.getParsingDetailsId());

        return separatedDocument;
    }

    /**
     * Separated APO containing only a separated document and its source and parsing details
     * Don't worry about multiple clinical actors, encounters, parsing details, or sources
     * since we have only one document
     */
    private Patient separate(Patient apo, Document document)
    {
        Patient separatedApo = new Patient();

        separatedApo.addDocument(document);

        AssemblerUtility.addClinicalActors(document, apo, separatedApo);
        AssemblerUtility.addEncounters(document, apo, separatedApo);
        AssemblerUtility.addParsingDetails(document, apo, separatedApo);
        AssemblerUtility.addSources(document, apo, separatedApo);

        return separatedApo;
    }
}
