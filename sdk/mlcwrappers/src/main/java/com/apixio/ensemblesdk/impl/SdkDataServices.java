package com.apixio.ensemblesdk.impl;

import java.util.NoSuchElementException;
import java.util.List;
import java.util.UUID;

import com.apixio.model.patient.Patient;
import com.apixio.ensemble.ifc.DataServices;
import com.apixio.sdk.FxLogger;

/**
 * The SDK intentionally doesn't provide any way for f(x) implementations to get this type of
 * data--the environment (ECC) is responsible for getting all arguments for the function.
 */
public class SdkDataServices implements DataServices
{

    @Override
    public UUID getPatientUUIDByDocumentUUID(UUID documentUUID) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public List<UUID> getDocUUIDsByPatientUUID(UUID patientUUID) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public List<UUID> getDocUUIDsByPatientUUID(String pds, UUID patientUUID) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public Patient getSinglePartialPatientByDocumentUUID(UUID docUUID) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public Patient getSinglePartialPatientByDocumentUUIDWithContent(UUID docUUID) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public Patient getFullPatientByPatientUUID(UUID patientUUID) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public Patient getSummary(String pdsID, UUID patientUUID, String category, String partID) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public Patient getSummary(String pdsID, UUID patientUUID, String category) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public Patient getSummary(UUID patientUUID, String category, String partID) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public Patient getSummary(UUID patientUUID, String category) throws NoSuchElementException
    {
        throw new RuntimeException("Unimplemented");
    }

}
