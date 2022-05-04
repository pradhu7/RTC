package com.apixio.model.converter.utils;

import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.converter.vocabulary.OWLVocabulary;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.*;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jctoledo on 4/19/16.
 */
public class SPARQLUtils {

    private static Logger logger = Logger.getLogger(SPARQLUtils.class);


    /**
     * Get the value of a query solution's variable as a String
     * @param sol a query solution object
     * @param variableName a variable binding for which we want the value
     * @return the string representation of a solution value
     */
    public static String getQuerySolutionVariableStringValue(QuerySolution sol, String variableName){
        if (sol.contains(variableName)) {
            if(sol.getLiteral(variableName) != null){
                return sol.getLiteral(variableName).getLexicalForm();
            }
        }
        logger.debug("Could not find "+variableName+" in solution object!");
        return "";
    }


    public static boolean getQuerySolutionVariableBooleanValue(QuerySolution sol, String varName){
        if (sol.contains(varName)) {
            if(sol.getLiteral(varName) != null){
                return sol.getLiteral(varName).getBoolean();
            }
        }
        logger.debug("Could not find "+varName+" in solution object!");
        return false;
    }

    /**
     * Get the value of a query solution's variable as a resource
     * @param sol a query solution object
     * @param variableName a variable binding for which we want the value
     * @return the Resource representation of a solution value
     */
    public static Resource getQuerySolutionVariableResource(QuerySolution sol, String variableName){
        if(sol.contains(variableName)){
            return sol.getResource(variableName);
        }
        logger.debug("could not find "+variableName+ "in solution object!" );
        return null;
    }

    public static boolean executeSPARQLAsk(RDFModel model, String queryStr){
        QueryExecution qe = makeExecution(model, queryStr);
        return qe.execAsk();
    }

    public static Model executeSPARQLDescribe(RDFModel model, String queryStr){
        QueryExecution qe = makeExecution(model, queryStr);
        Model results = qe.execDescribe();
        return results;
    }

    public static Model executeSPARQLDescribe(OntModel model, String queryStr){
        QueryExecution qe = makeExecution(model, queryStr);
        Model results = qe.execDescribe();
        return results;
    }

    public static ResultSet executeSPARQLSelect (InfModel model, String queryStr){
        QueryExecution qe = makeExecution(model, queryStr);
        ResultSet results = qe.execSelect();
        return results;
    }

    public static ResultSet executeSPARQLSelect(RDFModel model, String queryStr){
        QueryExecution qe = makeExecution(model, queryStr);
        ResultSet results = qe.execSelect();
        return results;
    }


    public static ResultSet executeSPARQLSelect(OntModel model, String queryStr){
        QueryExecution qe = makeExecution(model, queryStr);
        ResultSet results = qe.execSelect();
        return results;
    }

    public static ResultSet executeSPARQLSelect(Model model, String queryStr){
        QueryExecution qe = makeExecution(model, queryStr);
        ResultSet results = qe.execSelect();
        return results;
    }

    public static boolean askForDemographics(RDFModel model){
        String qs = "ASK { "+
                " ?p <"+RDF.type+"> <"+OWLVocabulary.OWLClass.Patient+">. "+
                " ?p <"+OWLVocabulary.OWLObjectProperty.HasDemographics+"> ?d. "+
                " ?d <"+RDF.type+"> <"+ OWLVocabulary.OWLClass.Demographics+">. "+
                " }";
        return SPARQLUtils.executeSPARQLAsk(model, qs);
    }


    public static boolean askForInsuranceClaim(RDFModel model){
        String qs = "ASK { "+
                " ?p <"+RDF.type+"> <"+OWLVocabulary.OWLClass.Patient+">. "+
                " ?p <"+OWLVocabulary.OWLObjectProperty.HasInsuranceClaim+"> ?ic. "+
                " ?ic <"+RDF.type+"> ?t . "+
                " ?t <"+RDFS.subClassOf+"> <"+ OWLVocabulary.OWLClass.InsuranceClaim+">. "+
                " }";
        return SPARQLUtils.executeSPARQLAsk(model, qs);
    }

    public static int getCountsByType(RDFModel model, URI c){
        String q = "select (count(distinct ?x) as ?total) where {?x rdf:type <"+c.toString()+">}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect( model, q);
        while(rs.hasNext()){
            QuerySolution sol = rs.next();
            return sol.get("total").asLiteral().getInt();
        }
        return -1;

    }


    public static int getCountsByType(Model model, URI c){
        String q = "select (count(distinct ?x) as ?total) where {?x rdf:type <"+c.toString()+">}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect( model, q);
        while(rs.hasNext()){
            QuerySolution sol = rs.next();
            return sol.get("total").asLiteral().getInt();
        }
        return -1;

    }

    public static boolean askForRAClaim(RDFModel model){
        String qs = " ASK{ "+
                " ?p <"+RDF.type+"> <"+OWLVocabulary.OWLClass.Patient+">. "+
                " ?p <"+ OWLVocabulary.OWLObjectProperty.HasRiskAdjustmentClaim+">  ?rac ."+
                " ?rac <"+RDF.type+"> <"+OWLVocabulary.OWLClass.RiskAdjustmentInsuranceClaim+">. "+
                " };";
        return SPARQLUtils.executeSPARQLAsk(model, qs);
    }

    public static Map<String, String> makeTypeToLabelMap(RDFModel model){
        Map<String, String> rm = new HashMap<>();
        String queryString =
                "SELECT distinct ?type ?lbl " +
                "WHERE {" +
                "      ?s <"+ RDF.type +"> ?type . " +
                "      OPTIONAL { ?type <"+ RDFS.label +"> ?lbl .} " +
                "}";
        ResultSet results = SPARQLUtils.executeSPARQLSelect(model, queryString);
        while(results.hasNext()){
            QuerySolution qs = results.next();
            RDFNode type = qs.get("type");
            Literal lblL = qs.getLiteral("lbl");
            if(lblL != null) {
                rm.put(type.asResource().toString(), lblL.toString());
            }else {
                rm.put(type.asResource().toString(), "");
            }
        }
        return rm;
    }

    /**
     * Given an APO model and a resource return a list of all rdf:types associated with the param
     * @param m an APO model
     * @param r a resource to test
     * @return a list of resource corresponding to all types
     */
    public static List<Resource> getRDFTypes(RDFModel m, Resource r){
        List<Resource> rm = new ArrayList<>();
        String q = "select distinct ?t where {<"+r.getURI()+"> rdf:type ?t}";
        ResultSet results = SPARQLUtils.executeSPARQLSelect(m, q);
        while(results.hasNext()){
            QuerySolution qs = results.next();
            try{
                Resource atype = qs.getResource("t");
                if(!rm.contains(atype)){
                    rm.add(atype);
                }
                qs.getResource("t");
            }catch (NullPointerException e) {
                return rm;
            }
        }
        return rm;
    }

    public static Map<URI, String> getCodedEntityClassMap(OntModel onto){
        Map<URI, String> rm = new HashMap<>();
        String qs = "select ?class as ?class_uri STR(?lbl) as ?label"+
                " where { "+
                " ?class <"+RDFS.subClassOf+"> <"+OWLVocabulary.OWLClass.CodedEntity.uri()+"> ."+
                " ?class <"+RDFS.label+"> ?lbl. " +
                "}";
        String prefixes = getDefaultPrefixes();
        qs = prefixes + qs;
        Query q = QueryFactory.create(qs);
        QueryExecution qe = QueryExecutionFactory.create(q, onto);
        ResultSet rs = qe.execSelect();
        while(rs.hasNext()){
            QuerySolution sol = rs.next();
            try {
                rm.put(new URI(sol.getResource("class_uri").getURI()), sol.getLiteral("label").toString());
            } catch (URISyntaxException e){
                logger.error ("could not create uri from query result!\n"+e);
                continue;
            }
        }
        return rm;
    }

    private  static QueryExecution makeExecution(InfModel model, String query){
        String prefixes = getDefaultPrefixes();
        query = prefixes + query;
        Query q = QueryFactory.create(query);
        QueryExecution qe = QueryExecutionFactory.create(q, model);
        return qe;
    }

    private static QueryExecution makeExecution(OntModel model, String query){
        String prefixes = getDefaultPrefixes();
        query = prefixes + query;
        Query q = QueryFactory.create(query);
        QueryExecution qe = QueryExecutionFactory.create(q, model);
        return qe;
    }

    private static QueryExecution makeExecution(RDFModel model, String query){
        String prefixes = getDefaultPrefixes();
        query = prefixes + query;
        Query q = QueryFactory.create(query);
        QueryExecution qe = QueryExecutionFactory.create(q, model);
        return qe;
    }

    private static QueryExecution makeExecution(Model model, String query){
        String prefixes = getDefaultPrefixes();
        query = prefixes + query;
        Query q = QueryFactory.create(query);
        QueryExecution qe = QueryExecutionFactory.create(q, model);
        return qe;
    }

    /**
     * Create a default prefix map to place at the header of every query string
     *
     */
    private static String getDefaultPrefixes() {
        String rm = "";
        Map<String, String> rmMap = new HashMap<>();
        rmMap.put("rdf", RDF.getURI());
        rmMap.put("rdfs", RDFS.getURI());
        rmMap.put("owl", OWL.getURI());
        rmMap.put("xsd", XSD.getURI());
        rmMap.put("dc", DC.getURI());
        rmMap.put("base", "http://apixio.com/");
        rmMap.put("alo", rmMap.get("base")+"ontology/alo#");
        for (Map.Entry<String, String> entry : rmMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            rm += "PREFIX "+key+": <"+value+"> \n";
        }
        return rm;
    }

    public static ParameterizedSparqlString addDefaultPrefixes(ParameterizedSparqlString q){
        q.setNsPrefix("rdf", RDF.getURI());
        q.setNsPrefix("rdfs", RDFS.getURI());
        q.setNsPrefix("xsd", XSD.getURI());
        q.setNsPrefix("dc", DC.getURI());
        q.setNsPrefix("base", "http://apixio.com/");
        q.setNsPrefix("alo", q.getNsPrefixURI("base")+"ontology/alo#");
        return q;
    }

}
