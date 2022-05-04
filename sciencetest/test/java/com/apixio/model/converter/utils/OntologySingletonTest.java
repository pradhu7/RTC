package com.apixio.model.converter.utils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by jctoledo on 4/21/16.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OntologySingletonTest {
    private static Logger logger = Logger.getLogger(OntologySingletonTest.class);

    private static OntologySingleton ontoSingleton;

    @BeforeClass
    public static void setupOnce(){
        ontoSingleton = OntologySingleton.getInstance();
    }

    @Test
    public void testA(){
        if(ontoSingleton == null){
            String msg ="Null Ontology Singleton Found! ";
            logger.error(msg);
            fail(msg);
        }
    }

    @Test
    public void testB(){
        String q = "SELECT ?c WHERE {?c rdfs:subClassOf* <http://apixio.com/ontology/alo#000113>.}";
        ResultSet rs = SPARQLUtils.executeSPARQLSelect(ontoSingleton.getOntModel(), q);
        if(rs == null){
            String msg ="Unable to create result set from query: "+q;
            fail(msg);
        }else{
            List<Resource> subClasses = new ArrayList<>();
            int c = 0;
            while(rs.hasNext()){
                QuerySolution sol = rs.next();
                if(sol != null){
                    c ++;
                    subClasses.add(sol.getResource("c"));
                }
            }
            if(c < 9){
                String msg = "Empty ResultSet created from query : "+q;
                logger.error(msg);
                fail(msg);
            }
        }
    }

    @Test
    public void testC(){
        com.hp.hpl.jena.util.iterator.ExtendedIterator<OntClass> c = ontoSingleton.getOntModel().listClasses();
        int cc = 0;
        while(c.hasNext()){
            OntClass oc = c.next();
            cc++;
        }
        if(cc == 0){
            String msg = "could not retrive classes from ontology document !";
            logger.error(msg);
            fail(msg);
        }
    }

    @Test
    public void testD(){
        OntClass oc = this.ontoSingleton.getOntModel().getOntClass("http://apixio.com/ontology/alo#000050");
        ExtendedIterator<OntClass> itr = oc.listSubClasses();
        int cc = 0;
        while(itr.hasNext()){
            OntClass c = itr.next();
            cc ++;
        }
        if(cc == 0){
            String msg = "could not retrive subclasses of Entity from ontology document !";
            logger.error(msg);
            fail(msg);
        }
    }



}