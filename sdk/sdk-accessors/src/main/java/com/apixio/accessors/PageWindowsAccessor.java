package com.apixio.accessors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;

import com.apixio.XUUID;
import com.apixio.XUUIDPrefixes;
import com.apixio.bizlogic.patient.logic.PatientLogic;
import com.apixio.dao.blobdao.BlobDAO;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DocCacheElemUtility;
import com.apixio.dao.utility.LinkDataUtility;
import com.apixio.dao.utility.PageUtility;
import com.apixio.dao.utility.LinkDataUtility.PdsIDAndPatientUUID;
import com.apixio.ensemble.ifc.DocCacheElement;
import com.apixio.ensemble.ifc.PageWindow;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Patient;
import com.apixio.protobuf.Doccacheelements.DocCacheElementProto;
import com.apixio.sdk.FxEnvironment;
import com.apixio.signals.PageWindowUtils;
import com.apixio.signals.PipelinePageWindow;

public class PageWindowsAccessor extends BaseAccessor
{
    private   PatientLogic patientLogic;
    protected BlobDAO      blobDAO;

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception
    {
        DaoServices daoServices;

        super.setEnvironment(env);

        daoServices = fxEnv.getDaoServices();

        blobDAO      = daoServices.getBlobDAO();
        patientLogic = new PatientLogic(daoServices);
    }

    /**
     * Signature:  List<com.apixio.ensemble.ifc.PageWindow> pageWindows(com.apixio.model.patient.Patient)
     */
    @Override
    public String getID()
    {
        return "pageWindows";
    }

    @Override
    public Object eval(AccessorContext context, List<Object> args) throws Exception
    {
        Object arg0 = args.get(0);
        if (arg0 instanceof String) {
            return pageWindowsFromDocument(UUID.fromString((String) arg0));
        }
        return pageWindowsFromPatient((Patient) args.get(0));
    }

    private List<PageWindow> pageWindowsFromDocument(UUID docuuid) throws Exception {
        Patient apo = patientLogic.getSinglePartialPatient(docuuid, true);
        for (Document document: apo.getDocuments()) 
        {
            if (document.getInternalUUID().equals(docuuid)) {
                LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = patientLogic.getPdsIDAndAuthPatientUUIDdByPatientUUID(apo.getPatientId());
                return pageWindowsFromDocument(apo, pdsIDAndPatientUUID, document);
            }
        }
        return null;
    }

    private List<PageWindow> pageWindowsFromDocument(Patient apo, PdsIDAndPatientUUID pdsIDAndPatientUUID, Document document) throws Exception 
    {
        PipelinePageWindow  fullDocPw;
        PageUtility.constructDocument(pdsIDAndPatientUUID.pdsID, document, blobDAO);
        DocCacheElemUtility.reconstituteCleanTextInDocument(pdsIDAndPatientUUID.pdsID, document, blobDAO);
        DocCacheElemUtility.updatePatientUuidInDocumentCleanText(apo.getPatientId(), document);
        List<DocCacheElementProto> protolist = DocCacheElemUtility.extractDocCacheElementProtoList(document);
        List<PageWindow> pws = new ArrayList<>();
        if (protolist != null && protolist.size() > 0) {

            fullDocPw = PageWindowUtils.generateFullDocumentPageWindow(apo, document,
                                                                       protolist.stream().map(
                                                                           p -> new DocCacheElementImpl(p)
                                                                           ).collect(Collectors.toList()));        

            pws.add(fullDocPw);

            addRight(pws, PageWindowUtils.generatePageWindows(fullDocPw));
            addRight(pws, PageWindowUtils.generatePerPagePageWindows(fullDocPw));
        }
        return pws;
    }

    private List<PageWindow> pageWindowsFromPatient(Patient apo) throws Exception
    {
        // this code is taken from bits and pieces of apx-spark-apps/siggen/src/main/scala/com/apixio/sparkapps/executor/DocumentSignalExecutor.scala,
        // tracing backwards from where pageSigGen.process(pw) is done

        // Document            document    = apo.getDocuments().iterator().next();
        List<PageWindow>    pws         = new ArrayList<>();
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = patientLogic.getPdsIDAndAuthPatientUUIDdByPatientUUID(apo.getPatientId());
        for (Document document: apo.getDocuments()) 
        {
            try {
                List<PageWindow> newpws = pageWindowsFromDocument(apo, pdsIDAndPatientUUID, document);
                pws.addAll(newpws);
            } catch (Exception ex) {
                System.out.println("Could not load document " + document.getInternalUUID().toString());
                ex.printStackTrace();
            }
        }

        return pws;
    }

    private void addRight(List<PageWindow> pws, Iterator<Pair<Integer, PageWindow>> iter)
    {
        while (iter.hasNext())
            pws.add(iter.next().getRight());
    }

    /**
     *
     */
    public static class DocCacheElementImpl implements DocCacheElement
    {
        private DateTime     documentDate;
        private List<String> externalIds;
        private String       content;
        private String       encounterId;
        private String       extractionType;
        private String       organization;
        private String       originalId;
        private String       sourceId;
        private String       title;
        private XUUID        documentId;
        private XUUID        patientId;
        private int          pageNumber;

        DocCacheElementImpl(DocCacheElementProto pb)
        {
            //  scala code ported from RichPatient.convertToDocCacheElem:
            patientId  = XUUID.fromStringWithType(pb.getPatientUUID(),  XUUIDPrefixes.PATIENT_TYPE);
            documentId = XUUID.fromStringWithType(pb.getDocumentUUID(), XUUIDPrefixes.DOC_TYPE);

            originalId     = pb.getOriginalId();
            encounterId    = pb.getEncId();
            sourceId       = pb.getSourceId();
            documentDate   = new DateTime(pb.getDocumentDate());
            title          = pb.getTitle();
            pageNumber     = pb.getPageNumber();
            extractionType = pb.getExtractionType();
            content        = pb.getContent();
            organization   = pb.getOrg();
            externalIds    = new ArrayList<>(pb.getExternalIdsList());
        }

        public XUUID getPatientId()
        {
            return patientId;
        }

        public XUUID getDocumentId()
        {
            return documentId;
        }

        public String getOriginalId()
        {
            return originalId;
        }

        public String getEncounterId()
        {
            return encounterId;
        }

        public String getSourceId()
        {
            return sourceId;
        }

        public DateTime getDocumentDate()
        {
            return documentDate;
        }

        public String getTitle()
        {
            return title;
        }

        public int getPageNumber()
        {
            return pageNumber;
        }

        public String getExtractionType()
        {
            return extractionType;
        }

        public String getContent()
        {
            return content;
        }

        public String getOrganization()
        {
            return organization;
        }

        public List<String> getExternalIds()
        {
            return externalIds;
        }
    }

}
