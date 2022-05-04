package com.apixio.model.external;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AxmUtilsTest {

    @Test
    public void testNoPHIInException() {
        try {
          AxmPackage p = AxmUtils.fromJson("THIS IS PHI NOT CORRECT JSON");
        } catch(Throwable e) {
          //System.out.println(e.getMessage());
          assertFalse(e.getMessage().contains("THIS IS PHI"));

          //System.out.println(ExceptionUtils.getStackTrace(e));
          assertFalse(ExceptionUtils.getStackTrace(e).contains("THIS IS PHI"));
        }
    }

    @Test
    public void testToJson() throws Exception {
        String expected = "{\"version\":\"0.1.0\",\"patient\":{\"demographics\":{\"givenNames\":[\"Jose\"],\"familyNames\":[\"Garcia\"],\"prefixes\":[\"Dr\"],\"suffixes\":[\"Jr\"],\"gender\":\"MALE\",\"dateOfBirth\":\"1990-01-01\",\"metaData\":{\"HealthPlan\":\"Kaiser\"}},\"externalIds\":[{\"id\":\"123\",\"assignAuthority\":\"PAT_ID\"},{\"id\":\"234\",\"assignAuthority\":\"HICN\"}],\"problems\":[{\"originalId\":{\"id\":\"123\",\"assignAuthority\":\"PROBLEM_AA\"},\"providerInRoles\":[{\"providerId\":{\"id\":\"454\",\"assignAuthority\":\"PROVIDER_AA\"},\"actorRole\":\"AUTHORING_PROVIDER\"}],\"name\":\"Diabetes\",\"resolution\":\"ACTIVE\",\"diagnosisDate\":\"2016-01-10\",\"temporalStatus\":\"Acute\",\"startDate\":\"2016-01-01\",\"endDate\":\"2016-02-01\",\"codeOrName\":{\"name\":\"Diabetes\"}}],\"encounters\":[{\"originalId\":{\"id\":\"454\",\"assignAuthority\":\"ENCOUNTER_AA\"},\"startDate\":\"2016-01-01\",\"endDate\":\"2016-02-01\",\"codeOrName\":{\"name\":\"Diabetes\"}}],\"providers\":[{\"originalId\":{\"id\":\"454\",\"assignAuthority\":\"PROVIDER_AA\"}}],\"coverages\":[{}]}}";

        AxmPatient patient = new AxmPatient();
        AxmPackage aPackage = new AxmPackage();
        aPackage.setVersion("0.1.0");
        aPackage.setPatient(patient);

        AxmExternalId id1 = new AxmExternalId();
        id1.setId("123");
        id1.setAssignAuthority("PAT_ID");
        patient.addExternalId(id1);

        AxmExternalId id2 = new AxmExternalId();
        id2.setId("234");
        id2.setAssignAuthority("HICN");
        patient.addExternalId(id2);

        AxmDemographics demo = new AxmDemographics();
        patient.setDemographics(demo);
        demo.addGivenName("Jose");
        demo.addFamilyName("Garcia");
        demo.addPrefix("Dr");
        demo.addSuffix("Jr");
        demo.setGender(AxmGender.MALE);
        demo.setDateOfBirth(LocalDate.parse("1990-01-01"));
        demo.addMetaTag("HealthPlan", "Kaiser");

        AxmProblem problem = new AxmProblem();
        patient.addProblem(problem);

        AxmExternalId problemId = new AxmExternalId();
        problemId.setId("123");
        problemId.setAssignAuthority("PROBLEM_AA");
        problem.setOriginalId(problemId);

        AxmProviderInRole providerInRole = new AxmProviderInRole();
        AxmExternalId problemProviderId = new AxmExternalId();
        problemProviderId.setId("454");
        problemProviderId.setAssignAuthority("PROVIDER_AA");
        providerInRole.setProviderId(problemProviderId);
        providerInRole.setActorRole(AxmActorRole.AUTHORING_PROVIDER);
        problem.addProviderInRole(providerInRole);

        problem.setName("Diabetes");
        problem.setResolution(AxmResolution.ACTIVE);
        problem.setDiagnosisDate(LocalDate.parse("2016-01-10"));
        problem.setStartDate(LocalDate.parse("2016-01-01"));
        problem.setEndDate(LocalDate.parse("2016-02-01"));
        problem.setTemporalStatus("Acute");

        AxmCodeOrName codeOrName = new AxmCodeOrName("Diabetes");
        problem.setCodeOrName(codeOrName);

        AxmEncounter encounter = new AxmEncounter();
        patient.addEncounter(encounter);
        AxmExternalId encounterId = new AxmExternalId();
        encounterId.setId("454");
        encounterId.setAssignAuthority("ENCOUNTER_AA");
        encounter.setOriginalId(encounterId);
        encounter.setStartDate(LocalDate.parse("2016-01-01"));
        encounter.setEndDate(LocalDate.parse("2016-02-01"));
        encounter.setCodeOrName(codeOrName);

        AxmProvider provider = new AxmProvider();
        patient.addProvider(provider);
        AxmExternalId providerId = new AxmExternalId();
        providerId.setId("454");
        providerId.setAssignAuthority("PROVIDER_AA");
        provider.setOriginalId(providerId);

        AxmCoverage coverage = new AxmCoverage();
        patient.addCoverage(coverage);

        String actual = AxmUtils.toJson(aPackage);

        assertEquals(expected, actual);

        assertEquals(expected, AxmUtils.toJson(AxmUtils.fromJson(expected)));


        AxmPatient testEmptyInit = AxmUtils.fromJson(expected).getPatient();
        assertNotNull(testEmptyInit.getProcedures()); // just a type of data not in above json
        assertTrue(testEmptyInit.getProcedures().size() == 0);
    }
}
