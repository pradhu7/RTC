package com.apixio.model.raps.core;

import static com.apixio.model.raps.RapsUtils.*;

/**
 * Risk assessment code can be absent of there is a 419 error code;
 */
public class RiskAssessmentCodeCluster {

    private RiskAssessmentCode code;

    private Integer error;

    public RiskAssessmentCodeCluster (RiskAssessmentCode code, Integer error) {
        this.code = code;
        this.error = checkCode(error);
    }

    public RiskAssessmentCode getCode() {
        return code;
    }

    public Integer getError() {
        return error;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
            .append(code == null ? "" : code.toString())
            .append(formatErrorCode(error));
        return builder.toString();
    }
}
