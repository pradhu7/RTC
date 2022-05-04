package com.apixio.validation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.apixio.model.patient.Actor;
import com.apixio.model.patient.ActorRole;
import com.apixio.model.patient.Address;
import com.apixio.model.patient.AddressType;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.CareSite;
import com.apixio.model.patient.CareSiteType;
import com.apixio.model.patient.ClinicalActor;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.CodedBaseObject;
import com.apixio.model.patient.ContactDetails;
import com.apixio.model.patient.Demographics;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.DocumentContent;
import com.apixio.model.patient.DocumentType;
import com.apixio.model.patient.EditType;
import com.apixio.model.patient.Encounter;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.Name;
import com.apixio.model.patient.NameType;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Problem;
import com.apixio.model.patient.Procedure;
import com.apixio.model.patient.Source;
import com.apixio.model.patient.event.Event;
import com.apixio.model.patient.event.EventType;
import com.apixio.model.utility.PatientJSONParser;
import com.apixio.validation.APOcheckers.ActorChecker;
import com.apixio.validation.APOcheckers.ActorRoleChecker;
import com.apixio.validation.APOcheckers.AddressChecker;
import com.apixio.validation.APOcheckers.AddressTypeChecker;
import com.apixio.validation.APOcheckers.BaseObjectChecker;
import com.apixio.validation.APOcheckers.CareSiteChecker;
import com.apixio.validation.APOcheckers.CareSiteTypeChecker;
import com.apixio.validation.APOcheckers.ClinicalActorChecker;
import com.apixio.validation.APOcheckers.ClinicalCodeChecker;
import com.apixio.validation.APOcheckers.CodedBaseObjectChecker;
import com.apixio.validation.APOcheckers.ContactDetailsChecker;
import com.apixio.validation.APOcheckers.DemographicsChecker;
import com.apixio.validation.APOcheckers.DocumentChecker;
import com.apixio.validation.APOcheckers.DocumentContentChecker;
import com.apixio.validation.APOcheckers.DocumentTypeChecker;
import com.apixio.validation.APOcheckers.EditTypeChecker;
import com.apixio.validation.APOcheckers.EncounterChecker;
import com.apixio.validation.APOcheckers.EventChecker;
import com.apixio.validation.APOcheckers.EventTypeChecker;
import com.apixio.validation.APOcheckers.ExternalIDChecker;
import com.apixio.validation.APOcheckers.GenderChecker;
import com.apixio.validation.APOcheckers.NameChecker;
import com.apixio.validation.APOcheckers.NameTypeChecker;
import com.apixio.validation.APOcheckers.PatientChecker;
import com.apixio.validation.APOcheckers.ProblemChecker;
import com.apixio.validation.APOcheckers.ProcedureChecker;
import com.apixio.validation.APOcheckers.SourceChecker;

public class ValidationRunner {

    public static void main(String[] args) {

        CheckerManager myCheckerManager = new CheckerManager();

        myCheckerManager.addChecker(Actor.class, new ActorChecker());
        myCheckerManager.addChecker(ActorRole.class, new ActorRoleChecker());
        myCheckerManager.addChecker(Address.class, new AddressChecker());
        myCheckerManager
            .addChecker(AddressType.class, new AddressTypeChecker());
        myCheckerManager.addChecker(BaseObject.class, new BaseObjectChecker());
        myCheckerManager.addChecker(CareSite.class, new CareSiteChecker());
        myCheckerManager.addChecker(CareSiteType.class,
                new CareSiteTypeChecker());
        myCheckerManager.addChecker(ClinicalActor.class,
                new ClinicalActorChecker());
        myCheckerManager.addChecker(ClinicalCode.class,
                new ClinicalCodeChecker());
        myCheckerManager.addChecker(CodedBaseObject.class,
                new CodedBaseObjectChecker());
        myCheckerManager.addChecker(ContactDetails.class,
                new ContactDetailsChecker());
        myCheckerManager.addChecker(Demographics.class,
                new DemographicsChecker());
        myCheckerManager.addChecker(Document.class, new DocumentChecker());
        myCheckerManager.addChecker(DocumentContent.class,
                new DocumentContentChecker());
        myCheckerManager.addChecker(DocumentType.class,
                new DocumentTypeChecker());
        myCheckerManager.addChecker(EditType.class, new EditTypeChecker());
        myCheckerManager.addChecker(Encounter.class, new EncounterChecker());
        myCheckerManager.addChecker(Event.class, new EventChecker());
        myCheckerManager.addChecker(EventType.class, new EventTypeChecker());
        myCheckerManager.addChecker(ExternalID.class, new ExternalIDChecker());
        myCheckerManager.addChecker(Gender.class, new GenderChecker());
        myCheckerManager.addChecker(Name.class, new NameChecker());
        myCheckerManager.addChecker(NameType.class, new NameTypeChecker());
        myCheckerManager.addChecker(Patient.class, new PatientChecker());
        myCheckerManager.addChecker(Problem.class, new ProblemChecker());
        myCheckerManager.addChecker(Procedure.class, new ProcedureChecker());
        myCheckerManager.addChecker(Source.class, new SourceChecker());

        myCheckerManager.addExclusion(org.joda.time.DateTime.class);
        myCheckerManager.addExclusion(java.lang.Class.class);
        myCheckerManager.addExclusion(java.lang.Boolean.class);
        myCheckerManager.addExclusion(java.lang.String.class);
        myCheckerManager.addExclusion(java.lang.Long.class);
        myCheckerManager.addExclusion(java.util.UUID.class);
        myCheckerManager.addExclusion(java.util.LinkedHashMap.class);
        myCheckerManager.addExclusion(java.util.HashSet.class);
        myCheckerManager.addExclusion(java.util.HashMap.class);

        PatientJSONParser parser = new PatientJSONParser();

        // The following section was modified by Alex Beyk to validate one large json file
        // instead of many small json files.
        // Notes:
        // * Most of the changes are quick updates done in the middle of HCC 3.0 End to End testing
        // * Input file remains hard coded and the output file is now the same file name with .txt appended
        // * To run, ensure the path is set correctly and point to the appropriate .json file and
        //   in Eclipse, right mouse click on ValidationRunner on the left pane and select "Run As" Java Application
        //   Check the output file (the .txt file) for the word "ERROR", "WARNING" don't seem as alarming for now

        String inputLoc = "C:\\!.Alex\\!.Apixio\\Test.Data\\september 2014\\patients.json";
        String outputLoc = inputLoc + ".txt";
        BufferedReader breader = null;
        try {
            breader = new BufferedReader(new InputStreamReader(new FileInputStream(inputLoc)));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        try {
            String line = null;
            String line2 = null;
            BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputLoc)));
            while ((line = breader.readLine()) != null) {
                try {
                    String apoString = line;
                    Patient myPatient = parser.parsePatientData(apoString);
                    List<Message> messages = myCheckerManager.run(myPatient,
                            myPatient.getPrimaryExternalID()
                            .getAssignAuthority()
                            + "_"
                            + myPatient.getPrimaryExternalID().getId());
                    for (Message m : messages) {
                        line2 = m.getMessageType() + ": " + m.getMessage() + "\n";
                        System.out.println(line2);
                        bwriter.write(line2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            bwriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            breader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
