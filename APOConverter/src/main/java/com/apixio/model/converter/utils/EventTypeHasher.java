package com.apixio.model.converter.utils;

import com.apixio.model.event.*;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;

/**
 * Created by jctoledo on 1/12/17.
 */
public class EventTypeHasher {

    public static String hashProcedureEvent(EventType et){
        String rm = null;
        //source
        rm += hashRef(et.getSource());
        //subject
        rm += hashRef(et.getSubject());
        //fact
        //fact code
        rm += hashCodeType(et.getFact().getCode());
        //fact time
        rm += hashTime(et.getFact().getTime());
        //fact values
        List<AttributeType> vals = et.getFact().getValues().getAttribute();
        for (AttributeType at : vals){
            if (at.getName().equals("problemName")){
                rm += at.getValue();
            }
            if(at.getName().equals("code_source_id")){
                rm += at.getValue();
            }
        }
        //evidence
        //evidence attributes
        List<AttributeType> evidences = et.getEvidence().getAttributes().getAttribute();
        for (AttributeType at: evidences){
            if(at.getName().equals("transactionType")){
                rm += at.getValue();
            }
            if(at.getName().equals("transactionDate")){
                rm += at.getValue();
            }
            if(at.getName().equals("renderingProviderId")){
                rm += at.getValue();
            }
            if(at.getName().equals("version")){
                rm += at.getValue();
            }
        }
        //evidence inferred
        rm += Boolean.toString(et.getEvidence().isInferred());
        //evidence source
        rm += hashRef(et.getEvidence().getSource());

        return DigestUtils.md5Hex(rm);
    }
    private static String hashCodeType(CodeType f){
        return DigestUtils.md5Hex(f.getCode()+f.getCodeSystem()+f.getCodeSystemName()+f.getDisplayName());
    }
    private static String hashTime(TimeRangeType t){
        return DigestUtils.md5Hex(t.getStartTime().toString()+t.getEndTime().toString());
    }

    private static String hashRef(ReferenceType s){
        return DigestUtils.md5Hex(s.getUri()+s.getType());
    }
}
