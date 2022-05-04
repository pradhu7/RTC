package com.apixio.bizlogic.patient;

import com.apixio.bizlogic.patient.logic.PatientAdminLogic;
import com.apixio.bizlogic.patient.logic.PatientLogic;
import com.apixio.dao.patient2.PatientDAO2;
import com.apixio.dao.patient2.PatientUtility;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.LinkDataUtility;
import com.apixio.model.patient.Patient;
import com.apixio.useracct.buslog.BatchLogic;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * Created by dyee on 9/26/17.
 */
@Ignore("Integration")
public class ApoValidationTest {
    private DaoServices daoServices;
    BatchLogic batchLogic;
    PatientLogic patientLogic;
    PatientAdminLogic patientAdminLogic;
    PatientDAO2 patientDAO;

    @Before
    public void setUp() throws Exception
    {
        DAOTestUtils util = new DAOTestUtils();
        daoServices = util.daoServices;
        batchLogic = daoServices.getBatchLogic();
        patientLogic = new PatientLogic(daoServices);
        patientAdminLogic = new PatientAdminLogic(daoServices);
        patientDAO = daoServices.getPatientDAO2();
    }

    @Test
    public void testsDaoServices() throws Exception
    {
        // fill in batchName
        String batchName = "1023_MadhuTestUpdatedMs3rdRound-38";
//        String batchName = "472_SuspectingLabs";

        // fill in batchID
        String pdsId = "1023";
//        String pdsId = "472";

        //
        // Don't execute test if we don't fill in batchName and pdsId, this will prevent
        // the build from failing - since we are using this test as a debug tool
        //
        if(StringUtils.isEmpty(batchName) || StringUtils.isEmpty(pdsId))
        {
            return;
        }

        Map<UUID, Boolean> apoInS3 = retrieveAllDocUUIDs(batchName, pdsId);

        UUID [] docUUIDs = {
                //UUID.fromString("d40ded23-156b-4cc3-824f-b0ae1e4851f1")
                UUID.fromString("4f8f0fba-a4ee-439c-9b14-2cbdd0ff3150")
        };

        for(UUID docUUID : docUUIDs)
        {
            if (!apoInS3.isEmpty() && apoInS3.containsKey(docUUID) && testAPO(docUUID, batchName))
            {
                apoInS3.put(docUUID, Boolean.TRUE);
            }
        }

        for(Boolean state : apoInS3.values()) {
            assertTrue(state);
        }

        System.out.println("DONE");
    }

    private boolean testUUIDs(UUID docUUID) throws Exception
    {
        // new cluster
        LinkDataUtility.PdsIDAndPatientUUID newPdsIDAndPatientUUID1 = patientLogic.getPdsIDAndPatientUUIDByDocumentUUID(docUUID);
        LinkDataUtility.PdsIDAndPatientUUID newPdsIDAndPatientUUID2 = patientLogic.getPdsIDAndAuthPatientUUIDdByPatientUUID(newPdsIDAndPatientUUID1.patientUUID);
        LinkDataUtility.PdsIDAndPatientUUIDs newPdsIDAndPatientUUIDs = patientLogic.getPdsIDAndPatientUUIDsByPatientUUID(newPdsIDAndPatientUUID1.patientUUID);


        // compare new cluster to itself!!!
        boolean newPdsMatch = newPdsIDAndPatientUUID1.pdsID.equals(newPdsIDAndPatientUUID2.pdsID) &&
                newPdsIDAndPatientUUID1.pdsID.equals(newPdsIDAndPatientUUIDs.pdsID);

        boolean newPuuidMatch = newPdsIDAndPatientUUID1.patientUUID.equals(newPdsIDAndPatientUUID2.patientUUID) &&
                !newPdsIDAndPatientUUIDs.patientUUIDs.isEmpty() &&
                newPdsIDAndPatientUUIDs.patientUUIDs.contains(newPdsIDAndPatientUUID1.patientUUID);

        return newPuuidMatch && newPdsMatch;
    }

    private boolean testAPO(UUID docUUID, String batchName) throws Exception
    {
        if (!testUUIDs(docUUID))
            return false;

        Patient newAPO = patientLogic.getSinglePartialPatient(docUUID, false);


        if (newAPO == null)
        {
            return false;
        }
        else
        {
            UUID newDocUUID = PatientUtility.getSourceFileArchiveUUID(newAPO, batchName);

            return newDocUUID != null;
        }
    }

    private Map<UUID, Boolean> retrieveAllDocUUIDs(String batchID, String pdsID) throws Exception
    {
        Map<UUID, Boolean> apoInS3 = new HashMap<>();

        // get it from old cassandra org cf
        Iterator<UUID> uuidIterator = patientDAO.getBatchDocumentUUIDs(batchID, pdsID);

        if (uuidIterator == null)
            throw new RuntimeException("No doccuuids in old Cassandra for pdsID: " + pdsID + "; and batchID:" + batchID);

        while (uuidIterator.hasNext())
        {
            apoInS3.put(uuidIterator.next(), Boolean.FALSE);
        }

        return apoInS3;
    }

}
