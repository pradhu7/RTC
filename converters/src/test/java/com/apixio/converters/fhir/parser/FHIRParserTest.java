package com.apixio.converters.fhir.parser;

import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.util.UUID;

/**
 * Created by alarocca on 7/10/20.
 */
public class FHIRParserTest {

    @Ignore
    @Test
    public void testParseFHIRDirectory()
    {
        String directory = "/Users/alarocca/data/clients/stjoes_lac/inbox/8621_2021-04-16/fhir_with_references/Everything/";
        File dir = new File(directory);
        for (File file : dir.listFiles()) {
            try {
                InputStream fhirResources = new FileInputStream(file);
                FHIRParserTest.parseResources(fhirResources);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Ignore
    @Test
    public void compareDuplicateFHIR()
    {
        try {
            String patientTwoFile = "/Users/alarocca/data/clients/stjoes_lac/inbox/8621_2021-04-16/fhir_with_references/Everything/4e560810fa6d.json";
            Patient patientTwo = parseResources(new FileInputStream(patientTwoFile));

            String patientOneFile = "/Users/alarocca/data/clients/stjoes_lac/inbox/8621_2021-04-16/fhir_with_references/Everything/4391d28ca6f2.json";
            Patient patientOne = parseResources(new FileInputStream(patientOneFile));
            System.out.println("Done!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    @Ignore
    @Test
    public void testParseFHIR()
    {
        try {
//            String fileName = "/Users/alarocca/Documents/psjh_test/fhir/Everything/49757894281d.json";
//            String fileName = "/Users/alarocca/Documents/psjh_test/fhir/Everything/209886dc9ac1.json";
            String fileName = "/Users/alarocca/data/clients/stjoes_lac/inbox/8621_2021-04-16/fhir_with_references/Everything/aea2dc85c19c.json";
            // The following two patients have the same ORCA MRN -- investigate
            InputStream fhirResources = new FileInputStream(fileName);
            FHIRParserTest.parseResources(fhirResources);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private static Patient parseResources(InputStream fhirResources) throws Exception{
        FHIRDSTU3Parser parser = new FHIRDSTU3Parser();
        parser.setPrimaryAssignAuthority("ORCA MRN");
        ApxCatalog.CatalogEntry catalogEntry = new ApxCatalog.CatalogEntry();
        catalogEntry.setMimeType("application/fhir+json");
        catalogEntry.setDocumentUUID(UUID.randomUUID().toString());
        ApxCatalog.CatalogEntry.Patient patient = new ApxCatalog.CatalogEntry.Patient();
        ApxCatalog.CatalogEntry.Patient.PatientId patientId = new ApxCatalog.CatalogEntry.Patient.PatientId();
        patientId.setId("1235");
        patientId.setAssignAuthority("PID");
        patient.getPatientId().add(patientId);
        catalogEntry.setPatient(patient);
        catalogEntry.setCreationDate("2020-10-30");
        catalogEntry.setSourceSystem("Epic");
        parser.parse(fhirResources, catalogEntry);
        return parser.getPatient();
//        PatientJSONParser pj = new PatientJSONParser();
//        System.out.println(pj.toJSON(fhirPatient));
    }
}
