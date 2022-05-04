package com.apixio.model.converter.lib;

import com.apixio.XUUID;
import com.apixio.model.converter.utils.OntologySingleton;
import com.apixio.model.converter.utils.SPARQLUtils;
import com.apixio.model.converter.vocabulary.OWLVocabulary;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.shared.Command;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jctoledo on 4/8/16.
 */
public class RDFModel implements Model {
    private static Logger logger = Logger.getLogger(RDFModel.class);
    private Model model;
    private XUUID xuuid;
    private String date;
    private OntModel onto;
    private Resource patientResource;
    private Reasoner reasoner;
    private RDFModel inferences;

    /**
     * Instantiates a new New apo model.
     *
     * @param rdfModel the rdf model
     */
    public RDFModel(Model rdfModel) {
        this();
        model = rdfModel;
    }

    /**
     * Instantiates a new New apo model.
     */
    public RDFModel() {
        model = ModelFactory.createDefaultModel();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        Date d = new Date();
        date = dateFormat.format(d);
        addNameSpaces();
        loadOntology();
    }

    private void addNameSpaces() {
        model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        model.setNsPrefix("alo", "http://apixio.com/ontology/alo#");
        model.setNsPrefix("ilo", "http://apixio.com/ontology/ilo#");
    }

    private void loadOntology() {
        this.onto = OntologySingleton.getInstance().getOntModel();
        this.add(onto);
    }

    private Reasoner getReasoner() {
        return this.reasoner;
    }

    public void setReasoner(Reasoner r) {
        this.reasoner = r;
        if (this.reasoner != null) {
            InfModel infmodel = ModelFactory.createInfModel(this.reasoner, this.model);
            this.inferences = new RDFModel(infmodel.getDeductionsModel());
            logger.debug(inferences.size() + " inferences made");
            this.model.add(this.inferences);
        }
    }

    public String getDate() {
        return this.date;
    }

    public Resource getPatientResource() {
        if (patientResource != null) {
            return patientResource;
        } else {
            String q = "select distinct ?p " +
                    " where {?p rdf:type <" + OWLVocabulary.OWLClass.Patient.uri() + "> .} limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this, q);
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                this.patientResource = sol.getResource("p");
            }
        }
        return this.patientResource;
    }

    public XUUID getPatientXUUID() {
        if (this.xuuid != null) {
            return this.xuuid;
        } else {
            String q = "select ?xuuid " +
                    "where {?p rdf:type <" + OWLVocabulary.OWLClass.Patient.uri() + ">." +
                    "?p <" + OWLVocabulary.OWLDataProperty.HasXUUID.property() + "> ?xuuid } limit 1";
            ResultSet rs = SPARQLUtils.executeSPARQLSelect(this, q);
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                if (sol.contains("xuuid")) {
                    XUUID rm = XUUID.fromString(sol.getLiteral("xuuid").getLexicalForm());
                    return rm;
                }
            }
        }
        return this.xuuid;
    }

    public void setPatientXUUID(XUUID anId) {
        this.xuuid = anId;
    }

    public String getRDFSLabel(Resource aUri) {
        if (this.contains(aUri, RDFS.label)) {
            return this.getProperty(aUri, RDFS.label).getLiteral().getLexicalForm();
        }
        return "";
    }

    public OntModel getOntology() {
        return this.onto;
    }

    @Override
    public Resource getResource(String s, ResourceF resourceF) {
        return this.model.getResource(s, resourceF);
    }

    @Override
    public Property getProperty(String s) {
        return this.model.getProperty(s);
    }

    @Override
    public Bag getBag(String s) {
        return this.model.getBag(s);
    }

    @Override
    public Bag getBag(Resource resource) {
        return this.model.getBag(resource);
    }

    @Override
    public Alt getAlt(String s) {
        return this.model.getAlt(s);
    }

    @Override
    public Alt getAlt(Resource resource) {
        return this.model.getAlt(resource);
    }

    @Override
    public Seq getSeq(String s) {
        return this.model.getSeq(s);
    }

    @Override
    public Seq getSeq(Resource resource) {
        return this.model.getSeq(resource);
    }

    @Override
    public Resource createResource(Resource resource) {
        return this.model.createResource(resource);
    }

    @Override
    public RDFNode getRDFNode(Node node) {
        return this.model.getRDFNode(node);
    }

    @Override
    public Resource createResource(String s, Resource resource) {
        return this.model.createResource(s, resource);
    }

    @Override
    public Resource createResource(ResourceF resourceF) {
        return this.model.createResource(resourceF);
    }

    @Override
    public Resource createResource(String s, ResourceF resourceF) {
        return this.model.createResource(s, resourceF);
    }

    @Override
    public Property createProperty(String s) {
        return this.model.createProperty(s);
    }

    @Override
    public Literal createLiteral(String s) {
        return this.model.createLiteral(s);
    }

    @Override
    public Literal createTypedLiteral(boolean b) {
        return this.model.createTypedLiteral(b);
    }

    @Override
    public Literal createTypedLiteral(int i) {
        return this.model.createTypedLiteral(i);
    }

    @Override
    public Literal createTypedLiteral(long l) {
        return this.model.createTypedLiteral(l);
    }

    @Override
    public Literal createTypedLiteral(Calendar calendar) {
        return this.model.createTypedLiteral(calendar);
    }

    @Override
    public Literal createTypedLiteral(char c) {
        return this.model.createTypedLiteral(c);
    }

    @Override
    public Literal createTypedLiteral(float v) {
        return this.model.createTypedLiteral(v);
    }

    @Override
    public Literal createTypedLiteral(double v) {
        return this.model.createTypedLiteral(v);
    }

    @Override
    public Literal createTypedLiteral(String s) {
        return this.model.createTypedLiteral(s);
    }

    @Override
    public Literal createTypedLiteral(String s, String s1) {
        return this.model.createTypedLiteral(s, s1);
    }

    @Override
    public Literal createTypedLiteral(Object o, String s) {
        return this.model.createTypedLiteral(o, s);

    }

    @Override
    public Statement createLiteralStatement(Resource resource, Property property, boolean b) {
        return this.model.createLiteralStatement(resource, property, b);
    }

    @Override
    public Statement createLiteralStatement(Resource resource, Property property, float v) {
        return this.model.createLiteralStatement(resource, property, v);
    }

    @Override
    public Statement createLiteralStatement(Resource resource, Property property, double v) {
        return this.model.createLiteralStatement(resource, property, v);
    }

    @Override
    public Statement createLiteralStatement(Resource resource, Property property, long l) {
        return this.model.createLiteralStatement(resource, property, l);
    }

    @Override
    public Statement createLiteralStatement(Resource resource, Property property, int i) {
        return this.model.createLiteralStatement(resource, property, i);
    }

    @Override
    public Statement createLiteralStatement(Resource resource, Property property, char c) {
        return this.model.createLiteralStatement(resource, property, c);
    }

    @Override
    public Statement createLiteralStatement(Resource resource, Property property, Object o) {
        return this.model.createLiteralStatement(resource, property, o);
    }

    @Override
    public Statement createStatement(Resource resource, Property property, String s) {
        return this.model.createStatement(resource, property, s);
    }

    @Override
    public Statement createStatement(Resource resource, Property property, String s, String s1) {
        return this.model.createStatement(resource, property, s, s1);
    }

    @Override
    public Statement createStatement(Resource resource, Property property, String s, boolean b) {
        return this.model.createStatement(resource, property, s, b);
    }

    @Override
    public Statement createStatement(Resource resource, Property property, String s, String s1, boolean b) {
        return this.model.createStatement(resource, property, s, s1, b);
    }

    @Override
    public Bag createBag() {
        return this.model.createBag();
    }

    @Override
    public Bag createBag(String s) {
        return this.model.createBag(s);
    }

    @Override
    public Alt createAlt() {
        return this.model.createAlt();
    }

    @Override
    public Alt createAlt(String s) {
        return this.model.createAlt(s);
    }

    @Override
    public Seq createSeq() {
        return this.model.createSeq();
    }

    @Override
    public Seq createSeq(String s) {
        return this.model.createSeq(s);
    }

    @Override
    public Model add(Resource resource, Property property, RDFNode rdfNode) {
        return this.model.add(resource, property, rdfNode);
    }

    @Override
    public Model addLiteral(Resource resource, Property property, boolean b) {
        return this.model.addLiteral(resource, property, b);
    }

    @Override
    public Model addLiteral(Resource resource, Property property, long l) {
        return this.model.addLiteral(resource, property, l);
    }

    @Override
    public Model addLiteral(Resource resource, Property property, int i) {
        return this.model.addLiteral(resource, property, i);
    }

    @Override
    public Model addLiteral(Resource resource, Property property, char c) {
        return this.model.addLiteral(resource, property, c);
    }

    @Override
    public Model addLiteral(Resource resource, Property property, float v) {
        return this.model.addLiteral(resource, property, v);
    }

    @Override
    public Model addLiteral(Resource resource, Property property, double v) {
        return this.model.addLiteral(resource, property, v);
    }

    @Override
    public Model addLiteral(Resource resource, Property property, Object o) {
        return this.model.addLiteral(resource, property, o);
    }

    @Override
    public Model addLiteral(Resource resource, Property property, Literal literal) {
        return this.model.addLiteral(resource, property, literal);
    }

    @Override
    public Model add(Resource resource, Property property, String s) {
        return this.model.add(resource, property, s);
    }

    @Override
    public Model add(Resource resource, Property property, String s, RDFDatatype rdfDatatype) {
        return this.model.add(resource, property, s, rdfDatatype);
    }

    @Override
    public Model add(Resource resource, Property property, String s, boolean b) {
        return this.model.add(resource, property, s, b);
    }

    @Override
    public Model add(Resource resource, Property property, String s, String s1) {
        return this.model.add(resource, property, s, s1);
    }

    @Override
    public Model remove(Resource resource, Property property, RDFNode rdfNode) {
        return this.model.remove(resource, property, rdfNode);
    }

    @Override
    public Model remove(StmtIterator stmtIterator) {
        return this.model.remove(stmtIterator);
    }

    @Override
    public Model remove(Model model) {
        return this.model.remove(model);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource resource, Property property, boolean b) {
        return this.model.listLiteralStatements(resource, property, b);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource resource, Property property, char c) {
        return this.listLiteralStatements(resource, property, c);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource resource, Property property, long l) {
        return this.model.listLiteralStatements(resource, property, l);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource resource, Property property, float v) {
        return this.model.listLiteralStatements(resource, property, v);
    }

    @Override
    public StmtIterator listLiteralStatements(Resource resource, Property property, double v) {
        return this.model.listLiteralStatements(resource, property, v);
    }

    @Override
    public StmtIterator listStatements(Resource resource, Property property, String s) {
        return this.model.listStatements(resource, property, s);
    }

    @Override
    public StmtIterator listStatements(Resource resource, Property property, String s, String s1) {
        return this.model.listStatements(resource, property, s, s1);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property property, boolean b) {
        return this.model.listResourcesWithProperty(property, b);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property property, long l) {
        return this.model.listResourcesWithProperty(property, l);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property property, char c) {
        return this.model.listResourcesWithProperty(property, c);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property property, float v) {
        return this.model.listResourcesWithProperty(property, v);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property property, double v) {
        return this.model.listResourcesWithProperty(property, v);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property property, Object o) {
        return this.model.listResourcesWithProperty(property, o);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property property, String s) {
        return this.model.listSubjectsWithProperty(property, s);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property property, String s, String s1) {
        return this.model.listSubjectsWithProperty(property, s, s1);
    }

    @Override
    public boolean containsLiteral(Resource resource, Property property, boolean b) {
        return this.model.containsLiteral(resource, property, b);
    }

    @Override
    public boolean containsLiteral(Resource resource, Property property, long l) {
        return this.model.containsLiteral(resource, property, l);
    }

    @Override
    public boolean containsLiteral(Resource resource, Property property, int i) {
        return this.model.containsLiteral(resource, property, i);
    }

    @Override
    public boolean containsLiteral(Resource resource, Property property, char c) {
        return this.model.containsLiteral(resource, property, c);
    }

    @Override
    public boolean containsLiteral(Resource resource, Property property, float v) {
        return this.model.containsLiteral(resource, property, v);
    }

    @Override
    public boolean containsLiteral(Resource resource, Property property, double v) {
        return this.model.containsLiteral(resource, property, v);
    }

    @Override
    public boolean containsLiteral(Resource resource, Property property, Object o) {
        return this.model.containsLiteral(resource, property, o);
    }

    @Override
    public boolean contains(Resource resource, Property property, String s) {
        return this.model.contains(resource, property, s);
    }

    @Override
    public boolean contains(Resource resource, Property property, String s, String s1) {
        return this.model.contains(resource, property, s, s1);
    }

    @Override
    public void enterCriticalSection(boolean b) {
        this.model.enterCriticalSection(b);
    }

    @Override
    public void leaveCriticalSection() {
        this.model.leaveCriticalSection();
    }

    @Override
    public PrefixMapping setNsPrefix(String s, String s1) {
        return this.model.setNsPrefix(s, s1);
    }

    @Override
    public PrefixMapping removeNsPrefix(String s) {
        return this.model.removeNsPrefix(s);
    }

    @Override
    public PrefixMapping setNsPrefixes(PrefixMapping prefixMapping) {
        return this.model.setNsPrefixes(prefixMapping);
    }

    @Override
    public PrefixMapping setNsPrefixes(Map<String, String> map) {
        return this.model.setNsPrefixes(map);
    }

    @Override
    public PrefixMapping withDefaultMappings(PrefixMapping prefixMapping) {
        return this.model.withDefaultMappings(prefixMapping);
    }

    @Override
    public String getNsPrefixURI(String s) {
        return this.model.getNsPrefixURI(s);
    }

    @Override
    public String getNsURIPrefix(String s) {
        return this.model.getNsURIPrefix(s);
    }

    @Override
    public Map<String, String> getNsPrefixMap() {
        return this.model.getNsPrefixMap();
    }

    @Override
    public String expandPrefix(String s) {
        return this.model.expandPrefix(s);
    }

    @Override
    public String shortForm(String s) {
        return this.model.shortForm(s);
    }

    @Override
    public String qnameFor(String s) {
        return this.model.qnameFor(s);
    }

    @Override
    public PrefixMapping lock() {
        return this.model.lock();
    }

    @Override
    public boolean samePrefixMappingAs(PrefixMapping prefixMapping) {
        return this.model.samePrefixMappingAs(prefixMapping);
    }

    @Override
    public RDFReader getReader() {
        return this.model.getReader();
    }

    @Override
    public RDFReader getReader(String s) {
        return this.model.getReader(s);
    }

    @Override
    public String setReaderClassName(String s, String s1) {
        return this.model.setReaderClassName(s, s1);
    }

    @Override
    public void resetRDFReaderF() {
        this.model.resetRDFReaderF();
    }

    @Override
    public String removeReader(String s) throws IllegalArgumentException {
        return this.model.removeReader(s);
    }

    @Override
    public RDFWriter getWriter() {
        return this.model.getWriter();
    }

    @Override
    public RDFWriter getWriter(String s) {
        return this.model.getWriter(s);
    }

    @Override
    public String setWriterClassName(String s, String s1) {
        return this.model.setWriterClassName(s, s1);
    }

    @Override
    public void resetRDFWriterF() {
        this.model.resetRDFWriterF();
    }

    @Override
    public String removeWriter(String s) throws IllegalArgumentException {
        return this.model.removeWriter(s);
    }

    @Override
    public Statement asStatement(Triple triple) {
        return this.model.asStatement(triple);
    }

    @Override
    public Graph getGraph() {
        return this.model.getGraph();
    }

    @Override
    public RDFNode asRDFNode(Node node) {
        return this.model.asRDFNode(node);
    }

    @Override
    public Resource wrapAsResource(Node node) {
        return this.model.wrapAsResource(node);
    }

    @Override
    public String toString() {
        String deductionSize = "";
        if (this.getDeductions() != null) {
            deductionSize = ", deductionsSize=" + getDeductions().size();
        }
        return "RDFModel{" +
                "size=" + model.size() +
                ", model=" + model +
                ", xuuid=" + xuuid +
                ", date='" + date + '\'' +
                deductionSize +
                ", onto=" + onto +
                '}';
    }

    public RDFModel getDeductions() {
        return this.inferences;
    }

    @Override
    public long size() {
        return this.model.size();
    }

    @Override
    public boolean isEmpty() {
        return this.model.isEmpty();
    }

    @Override
    public ResIterator listSubjects() {
        return this.model.listSubjects();
    }

    @Override
    public NsIterator listNameSpaces() {
        return this.model.listNameSpaces();
    }

    @Override
    public Resource getResource(String s) {
        return this.model.getResource(s);
    }

    @Override
    public Property getProperty(String s, String s1) {
        return this.model.getProperty(s, s1);
    }

    @Override
    public Resource createResource() {
        return this.model.createResource();
    }

    @Override
    public Resource createResource(AnonId anonId) {
        return this.model.createResource(anonId);
    }

    @Override
    public Resource createResource(String s) {
        return this.model.createResource(s);
    }

    @Override
    public Property createProperty(String s, String s1) {
        return this.model.createProperty(s, s1);
    }

    @Override
    public Literal createLiteral(String s, String s1) {
        return this.model.createLiteral(s, s1);
    }

    @Override
    public Literal createLiteral(String s, boolean b) {
        return this.model.createLiteral(s, b);
    }

    @Override
    public Literal createTypedLiteral(String s, RDFDatatype rdfDatatype) {
        return this.model.createTypedLiteral(s, rdfDatatype);
    }

    @Override
    public Literal createTypedLiteral(Object o, RDFDatatype rdfDatatype) {
        return this.model.createTypedLiteral(o, rdfDatatype);
    }

    @Override
    public Literal createTypedLiteral(Object o) {
        return this.model.createTypedLiteral(o);
    }

    @Override
    public Statement createStatement(Resource resource, Property property, RDFNode rdfNode) {
        return this.model.createStatement(resource, property, rdfNode);
    }

    @Override
    public RDFList createList() {
        return this.model.createList();
    }

    @Override
    public RDFList createList(Iterator<? extends RDFNode> iterator) {
        return this.model.createList(iterator);
    }

    @Override
    public RDFList createList(RDFNode[] rdfNodes) {
        return this.model.createList(rdfNodes);
    }

    @Override
    public Model add(Statement statement) {
        return this.model.add(statement);
    }

    @Override
    public Model add(Statement[] statements) {
        return this.model.add(statements);
    }

    @Override
    public Model remove(Statement[] statements) {
        return this.model.remove(statements);
    }

    @Override
    public Model add(List<Statement> list) {
        return this.model.add(list);
    }

    @Override
    public Model remove(List<Statement> list) {
        return this.model.remove(list);
    }

    @Override
    public Model add(StmtIterator stmtIterator) {
        return this.model.add(stmtIterator);
    }

    @Override
    public Model add(Model model) {
        return this.model.add(model);
    }

    @Override
    public Model read(String s) {
        return this.model.read(s);
    }

    @Override
    public Model read(InputStream inputStream, String s) {
        return this.model.read(inputStream, s);
    }

    @Override
    public Model read(InputStream inputStream, String s, String s1) {
        return this.model.read(inputStream, s, s1);
    }

    @Override
    public Model read(Reader reader, String s) {
        return this.model.read(reader, s);
    }

    @Override
    public Model read(String s, String s1) {
        return this.model.read(s, s1);
    }

    @Override
    public Model read(Reader reader, String s, String s1) {
        return this.model.read(reader, s, s1);
    }

    @Override
    public Model read(String s, String s1, String s2) {
        return this.model.read(s, s1, s2);
    }

    @Override
    public Model write(Writer writer) {
        return this.model.write(writer);
    }

    @Override
    public Model write(Writer writer, String s) {
        return this.model.write(writer, s);
    }

    @Override
    public Model write(Writer writer, String s, String s1) {
        return this.model.write(writer, s, s1);
    }

    @Override
    public Model write(OutputStream outputStream) {
        return this.model.write(outputStream);
    }

    @Override
    public Model write(OutputStream outputStream, String s) {
        return this.model.write(outputStream, s);
    }

    @Override
    public Model write(OutputStream outputStream, String s, String s1) {
        return this.model.write(outputStream, s, s1);
    }

    @Override
    public Model remove(Statement statement) {
        return this.model.remove(statement);
    }

    @Override
    public Statement getRequiredProperty(Resource resource, Property property) {
        return this.model.getRequiredProperty(resource, property);
    }

    @Override
    public Statement getProperty(Resource resource, Property property) {
        return this.model.getProperty(resource, property);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property property) {
        return this.model.listSubjectsWithProperty(property);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property property) {
        return this.model.listResourcesWithProperty(property);
    }

    @Override
    public ResIterator listSubjectsWithProperty(Property property, RDFNode rdfNode) {
        return this.model.listSubjectsWithProperty(property, rdfNode);
    }

    @Override
    public ResIterator listResourcesWithProperty(Property property, RDFNode rdfNode) {
        return this.model.listResourcesWithProperty(property, rdfNode);
    }

    @Override
    public NodeIterator listObjects() {
        return this.model.listObjects();
    }

    @Override
    public NodeIterator listObjectsOfProperty(Property property) {
        return this.model.listObjectsOfProperty(property);
    }

    @Override
    public NodeIterator listObjectsOfProperty(Resource resource, Property property) {
        return this.model.listObjectsOfProperty(resource, property);
    }

    @Override
    public boolean contains(Resource resource, Property property) {
        return this.model.contains(resource, property);
    }

    @Override
    public boolean containsResource(RDFNode rdfNode) {
        return this.model.containsResource(rdfNode);
    }

    @Override
    public boolean contains(Resource resource, Property property, RDFNode rdfNode) {
        return this.model.contains(resource, property, rdfNode);
    }

    @Override
    public boolean contains(Statement statement) {
        return this.model.contains(statement);
    }

    @Override
    public boolean containsAny(StmtIterator stmtIterator) {
        return this.model.containsAny(stmtIterator);
    }

    @Override
    public boolean containsAll(StmtIterator stmtIterator) {
        return this.model.containsAll(stmtIterator);
    }

    @Override
    public boolean containsAny(Model model) {
        return this.model.containsAny(model);
    }

    @Override
    public boolean containsAll(Model model) {
        return this.model.containsAll(model);
    }

    @Override
    public boolean isReified(Statement statement) {
        return this.model.isReified(statement);
    }

    @Override
    public Resource getAnyReifiedStatement(Statement statement) {
        return this.model.getAnyReifiedStatement(statement);
    }

    @Override
    public void removeAllReifications(Statement statement) {
        this.model.removeAllReifications(statement);
    }

    @Override
    public void removeReification(ReifiedStatement reifiedStatement) {
        this.model.removeReification(reifiedStatement);
    }

    @Override
    public StmtIterator listStatements() {
        return this.model.listStatements();
    }

    @Override
    public StmtIterator listStatements(Selector selector) {
        return this.model.listStatements(selector);
    }

    @Override
    public StmtIterator listStatements(Resource resource, Property property, RDFNode rdfNode) {
        return this.model.listStatements(resource, property, rdfNode);
    }

    @Override
    public ReifiedStatement createReifiedStatement(Statement statement) {
        return this.model.createReifiedStatement(statement);
    }

    @Override
    public ReifiedStatement createReifiedStatement(String s, Statement statement) {
        return this.model.createReifiedStatement(s, statement);
    }

    @Override
    public RSIterator listReifiedStatements() {
        return this.model.listReifiedStatements();
    }

    @Override
    public RSIterator listReifiedStatements(Statement statement) {
        return this.model.listReifiedStatements(statement);
    }

    @Override
    public Model query(Selector selector) {
        return this.model.query(selector);
    }

    @Override
    public Model union(Model model) {
        return this.model.union(model);
    }

    @Override
    public Model intersection(Model model) {
        return this.model.intersection(model);
    }

    @Override
    public Model difference(Model model) {
        return this.model.difference(model);
    }

    @Override
    public Model begin() {
        return this.model.begin();
    }

    @Override
    public Model abort() {
        return this.model.abort();
    }

    @Override
    public Model commit() {
        return this.model.commit();
    }

    @Override
    public Object executeInTransaction(Command command) {
        return this.model.executeInTransaction(command);
    }

    @Override
    public boolean independent() {
        return this.model.independent();
    }

    @Override
    public boolean supportsTransactions() {
        return this.model.supportsTransactions();
    }

    @Override
    public boolean supportsSetOperations() {
        return this.model.supportsSetOperations();
    }

    @Override
    public boolean isIsomorphicWith(Model model) {
        return this.model.isIsomorphicWith(model);
    }

    @Override
    public void close() {
        this.model.close();
    }

    @Override
    public Lock getLock() {
        return this.model.getLock();
    }

    @Override
    public Model register(ModelChangedListener modelChangedListener) {
        return this.model.register(modelChangedListener);
    }

    @Override
    public Model unregister(ModelChangedListener modelChangedListener) {
        return this.model.unregister(modelChangedListener);
    }

    @Override
    public Model notifyEvent(Object o) {
        return this.model.notifyEvent(o);
    }

    @Override
    public Model removeAll() {
        return this.model.removeAll();
    }

    @Override
    public Model removeAll(Resource resource, Property property, RDFNode rdfNode) {
        return this.model.removeAll(resource, property, rdfNode);
    }

    @Override
    public boolean isClosed() {
        return this.model.isClosed();
    }
}
