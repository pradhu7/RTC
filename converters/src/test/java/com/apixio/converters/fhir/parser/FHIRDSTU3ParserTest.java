package com.apixio.converters.fhir.parser;

import com.apixio.model.patient.Problem;
import org.joda.time.LocalDate;
import org.junit.Test;

import java.io.InputStream;


public class FHIRDSTU3ParserTest {
    @Test
    public void testConditionEndDate() throws Exception {
        InputStream conditionDocumentInputStream = this.getClass().getResourceAsStream("/com/apixio/converters/fhir/parser/condition.json");
        LocalDate endDate = new LocalDate("2006-10-22"); // Onset "end" is 7/7, abatement is 10/22

        FHIRDSTU3Parser fhirParser = new FHIRDSTU3Parser();
        fhirParser.parse(conditionDocumentInputStream);
        Iterable<Problem> problems = fhirParser.getPatient().getProblems();
        Problem fhirProblem = problems.iterator().next();

        assert (fhirProblem.getEndDate().toLocalDate().equals(endDate));
    }
}