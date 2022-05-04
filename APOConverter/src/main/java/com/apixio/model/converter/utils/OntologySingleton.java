package com.apixio.model.converter.utils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;
import org.apache.log4j.Logger;

import java.io.InputStream;

/**
 * Singleton for accessing an OntModel with Apixio's OWL ontologies
 * Created by jctoledo on 4/21/16.
 */
public final class OntologySingleton {
    private static Logger logger = Logger.getLogger(OntologySingleton.class);
    private static final String aloPath = "/ontology/alo.owl";
    private static final String iloPath = "/ontology/ilo.owl";
    private static OntologySingleton instance = null;
    private OntModel ontModel = null;

    protected OntologySingleton () {
        initialize();
    }

    /**
     * Initialize the ont model
     */
    private void initialize() {
        OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
        try {
            InputStream in = this.getClass().getResourceAsStream(aloPath);
            InputStream iloIn = this.getClass().getResourceAsStream(iloPath);
            try {
                ontoModel.read(in, null);
                ontoModel.read(iloIn, null);
                this.ontModel = ontoModel;
                in.close();
                iloIn.close();
            } catch (org.apache.jena.atlas.web.HttpException e){
              logger.debug(e.getMessage());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            logger.info("Ontology at " + aloPath + " loaded.");
        } catch (JenaException je) {
            System.err.println("ERROR" + je.getMessage());
            logger.error(je);
            System.exit(0);
        }
    }

    /**
     * Create an instance of this singleton
     * @return an ontology singleton
     */
    public static OntologySingleton getInstance(){
        if(instance == null){
            instance = new OntologySingleton();
        }
        return instance;
    }

    /**
     * Return an OntModel representation of Apixio's ontology model
     * @return a Jena OntModel
     */
    public OntModel getOntModel(){
        return this.ontModel;
    }

}
