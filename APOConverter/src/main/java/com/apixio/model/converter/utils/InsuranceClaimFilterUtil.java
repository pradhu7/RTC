package com.apixio.model.converter.utils;

import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.converter.vocabulary.OWLVocabulary;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by jctoledo on 6/8/16.
 */
public class InsuranceClaimFilterUtil {
    private static RDFModel filterModel = new RDFModel();
    private static Logger logger = Logger.getLogger(InsuranceClaimFilterUtil.class);

    /**
     * Read in the default RA claim filter into an rdf model and return it
     *
     * @param is An input stream with the csv file that dic
     * @return an RDF model with the default filtering resources
     */
    public static RDFModel getRAClaimFilterModel(String is) {
        filterModel = createFilterModel(is);
        return filterModel;
    }

    /**
     * This method instanciates a jena model and populates it with an RDF representation of
     * the desired filters
     *
     * @param ais an inputstream
     * @return a jena model containing an RDF representation of the CSV filter file
     */
    private static RDFModel createFilterModel(String ais) {
        RDFModel rm = new RDFModel();
        if (ais != null && ais == ""){
            logger.warn("Found an empty filter file!");
            return null;
        }

        if (ais != null && isValidInsuranceClaimFilterFile(ais)) {
            List<String> lines = Arrays.asList(ais.split("\\n"));
            int linecount = 0;
            for (String line : lines) {
                if (linecount != 0) {
                    String[] lineArr = line.trim().split("\\s*,\\s*");
                    String errorCodeVal = lineArr[0].replaceAll("[^A-Za-z0-9 ]", "");
                    String description = lineArr[1];
                    String route = lineArr[2];
                    String showOnOut = lineArr[3];
                    boolean showOutputBool = parseStringToValue(showOnOut);
                    boolean routebool = parseStringToValue(route);
                    //now I am ready to create some RDF
                    //create a resource of type InsuranceClaimErrorCodeFilter
                    Resource res = rm.createResource(OWLVocabulary.BASE_NAMESPACE + "InsuranceClaimFilter/" + errorCodeVal);
                    res.addProperty(RDF.type, OWLVocabulary.OWLClass.InsuranceClaimErrorCodeFilter.resource());
                    res.addProperty(DCTerms.description, description);
                    res.addLiteral(OWLVocabulary.OWLDataProperty.InOutputReport.property(), showOutputBool);
                    res.addLiteral(OWLVocabulary.OWLDataProperty.RouteToCoder.property(), routebool);
                    res.addLiteral(OWLVocabulary.OWLDataProperty.HasErrorCodeValue.property(), errorCodeVal);
                }
                linecount++;
            }
        } else {
            logger.debug("Found invalid csv file");
            return null;
        }
        return rm;
    }

    private static boolean isValidInsuranceClaimFilterFile(String csvString) {

        List<String> lines = Arrays.asList(csvString.split("\\n"));
        int linecount = 0;
        w:
        for (String line : lines) {
            String[] lineArr = line.trim().split("\\s*,\\s*");
            if (lineArr.length != 4) {
                return false;
            }
            if (linecount == 0) {
                if (!lineArr[0].equals("ERROR CODE")) {
                    return false;
                }
                if (!lineArr[1].equals("ERROR DESCRIPTION")) {
                    return false;
                }
                if (!lineArr[2].equals("ROUTE")) {
                    return false;
                }
                if (!lineArr[3].equals("SHOW ON OUTPUT")) {
                    return false;
                }
                linecount++;
                continue w;
            }
            if (lineArr[0].trim().length() != 3) {
                return false;
            }
            if (lineArr[1].trim().length() == 0) {
                return false;
            }
            if (!lineArr[2].toLowerCase().contains("yes")) {
                if (!lineArr[2].toLowerCase().contains("no")) {
                    return false;
                }
            }
            if (!lineArr[3].toLowerCase().contains("yes")) {
                if (!lineArr[3].toLowerCase().contains("no")) {
                    return false;
                }
            }
            linecount++;
        }
        return true;
    }

    private static boolean parseStringToValue(String s) {
        if (s.toLowerCase().contains("yes")) {
            return true;
        } else {
            return false;
        }

    }
}
