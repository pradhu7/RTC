package com.apixio.model.converter.utils;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.util.FileManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.AutoIRIMapper;

import java.io.File;

/**
 * Created by jctoledo on 4/19/16.
 */
public class SPARQLUtilsTest {
    private static File ontologyFile;
    private static File inputDir;

    @BeforeClass
    public static void setupOnce(){
        // load the owl documents from resources
        ClassLoader cl = SPARQLUtilsTest.class.getClassLoader();
        ontologyFile = new File(cl.getResource("ontology/alo.owl").getFile());
        inputDir = new File(cl.getResource("ontology").getFile());
    }

    /*@Test
    public void testOne() throws OWLOntologyCreationException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String reasonerFactoryName = "org.semanticweb.HermiT.Reasoner$ReasonerFactory";
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();


        AutoIRIMapper mapper = new AutoIRIMapper(inputDir, false);
        manager.getIRIMappers().add(mapper);
        OWLOntology ilo = null;

        for (IRI i : mapper.getOntologyIRIs()){
            OWLOntology o = manager.loadOntology(i);
            String s = o.getOntologyID().getOntologyIRI().toString();
            if(s.contains("ilo")){
                ilo = o;
            }
            int p=0;
        }

        OWLReasonerFactory rfact = (OWLReasonerFactory) Class.forName(reasonerFactoryName).newInstance();
       //OWLIndividual ind = rfact.(URI.create(ontology.getURI()+"JAVA"));
        OWLReasoner or = rfact.createReasoner(ilo);


        boolean b = or.isConsistent();

        int q =1;
    }

    @Test
    public void testTwo(){
        Model schema = FileManager.get().loadModel("file:"+inputDir+"/ilo.owl");
        Model data = FileManager.get().loadModel("file:data/owlDemoData.ttl");
        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        reasoner = reasoner.bindSchema(schema);

        InfModel infmodel = ModelFactory.createInfModel(reasoner, data);
        int p = 2;
    }

    @Test
    public void testGetCodedEntityClassMap() throws Exception {
        //create manager
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = null;
        AutoIRIMapper mapper = new AutoIRIMapper(ontologyFile.getParentFile(), true);
        manager.getIRIMappers().add(mapper);
        IRI alo = null;
        for(IRI i: mapper.getOntologyIRIs()){
            if(i.toString().contains("alo")){
                alo = i;
            }
            manager.loadOntology(i);
        }
        ontology = manager.getOntology(alo);

    }*/
}