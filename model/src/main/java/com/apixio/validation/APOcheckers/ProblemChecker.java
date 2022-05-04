package com.apixio.validation.APOcheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.apixio.model.patient.Problem;
import com.apixio.validation.Message;
import com.apixio.validation.MessageType;

public class ProblemChecker<T extends Problem> extends CodedBaseObjectChecker<T>{

    //public class ProblemChecker implements Checker<Problem>{

    @Override
    public List<Message> check(T problem) {

        String type = problem.getClass().getName();

        List<Message> result = new ArrayList<Message>();

        result.addAll(super.check(problem));

        if (StringUtils.isBlank(problem.getProblemName())) {
            result.add(new Message(type + " problemName is emtpy", MessageType.WARNING));
        }

        if (problem.getStartDate() == null && problem.getDiagnosisDate() == null) {

            result.add(new Message(type + " both startDate and diagnosisDate are null", MessageType.ERROR));

        } else {

            if (problem.getStartDate() == null) {
                result.add(new Message(type + " startDate is null", MessageType.WARNING));
            }

            if (problem.getDiagnosisDate() == null) {
                result.add(new Message(type + " diagnosisDate is null", MessageType.WARNING));
            }

        }


        if (problem.getEndDate() == null) {
            result.add(new Message(type + " endDate is null", MessageType.WARNING));
        }

        //private ResolutionStatus resolutionStatus;
        //private String temporalStatus;	// wether a problem is acute, chronic, time limited, etc..

        return result;
    }
    }
