package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.Procedure;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class ProcedureChecker<T extends Procedure> extends CodedBaseObjectChecker<T>{

    @Override
    public List<Message> check(T procedure) {

        String type = procedure.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(procedure));

        if (StringUtils.isBlank(procedure.getProcedureName())) {
            result.add(new Message(type + " procedureName is null", MessageType.ERROR));
        }

        if (procedure.getPerformedOn() == null) {
            result.add(new Message(type + " performedOn is null", MessageType.ERROR));
        }

        if (procedure.getEndDate() == null) {
            result.add(new Message(type + " endDate is null", MessageType.WARNING));
        }

        //private String interpretation;
        //private Anatomy bodySite;
        //private List<ClinicalCode> supportingDiagnosis = new LinkedList<ClinicalCode>();

        return result;
    }
}
