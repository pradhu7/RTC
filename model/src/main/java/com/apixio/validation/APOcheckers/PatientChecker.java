package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import com.apixio.model.patient.Patient;
import com.apixio.validation.Checker;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class PatientChecker implements Checker<Patient> {

    public List<Message> check(Patient patient) {

        String type = patient.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        if (patient.getProblems() == null) {
            result.add(new Message(type + " problems is null",
                        MessageType.WARNING));
        }

        if (patient.getProcedures() == null) {
            result.add(new Message(type + " procedures is null",
                        MessageType.WARNING));
        }

        if (patient.getPrescriptions() == null) {
            result.add(new Message(type + " prescriptions is null",
                        MessageType.WARNING));
        }

        if (patient.getAdministrations() == null) {
            result.add(new Message(type + " administrations is null",
                        MessageType.WARNING));
        }

        if (patient.getLabs() == null) {
            result.add(new Message(type + " labs is null", MessageType.WARNING));
        }

        if (patient.getApixions() == null) {
            result.add(new Message(type + " apixions is null",
                        MessageType.WARNING));
        }

        if (patient.getBiometricValues() == null) {
            result.add(new Message(type + " biometricValues is null",
                        MessageType.WARNING));
        }

        if (patient.getAllergies() == null) {
            result.add(new Message(type + " allergies is null",
                        MessageType.WARNING));
        }

        if (patient.getPatientId() == null) {
            result.add(new Message(type + " patientId is null",
                        MessageType.ERROR));
        }

        if (patient.getPrimaryExternalID() == null) {
            result.add(new Message(type + " primaryExternalID is null",
                        MessageType.ERROR));
        }

        // externalIDs;

        if (patient.getPrimaryDemographics() == null) {
            result.add(new Message(type + " primaryDemographics is null",
                        MessageType.WARNING));
        }

        // alternateDemographics;

        if (patient.getPrimaryContactDetails() == null) {
            result.add(new Message(type + " primaryContactDetails is null",
                        MessageType.WARNING));
        }

        // private List<ContactDetails> alternateContactDetails;

        if (patient.getFamilyHistories() == null) {
            result.add(new Message(type + " familyHistories is null",
                        MessageType.WARNING));
        }

        // private List<SocialHistory> socialHistories;

        if (patient.getClinicalEvents() == null) {
            result.add(new Message(type + " clinicalEvents is null",
                        MessageType.WARNING));
        }

        return result;
    }

}
