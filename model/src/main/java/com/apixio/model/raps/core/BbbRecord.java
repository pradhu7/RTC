package com.apixio.model.raps.core;

import static com.apixio.model.raps.RapsUtils.*;

public class BbbRecord extends BatchRecordBase {
    private static final int FILLER_MAX_LENGTH_WHEN_OVERPAYMENT = 475;
    private static final int FILLER_MAX_LENGTH_WHEN_NO_OVERPAYMENT = 497;
    private static final int OVERPAYMENT_ID_FIELD_LENGTH = 12;

    private final String overpaymentId;
    private final Integer overpaymentIdErrorCode;
    private final Integer paymentYear;
    private final Integer paymentYearErrorCode;

    public BbbRecord(int sequenceNumber, String planNumber, String overpaymentId,
            Integer overpaymentIdErrorCode, Integer paymentYear, Integer paymentYearErrorCode, String filler) {
        super(sequenceNumber, planNumber, filler);
        if (overpaymentId != null && filler.length() > FILLER_MAX_LENGTH_WHEN_OVERPAYMENT) {
            throw new IllegalArgumentException("filler must be no longer than " + FILLER_MAX_LENGTH_WHEN_OVERPAYMENT + " when overpayment indicator is specified");
        }

        if (overpaymentId == null && paymentYear != null || overpaymentId != null && paymentYear == null) {
            throw new IllegalArgumentException("overpayment id and year must be present or absent together");
        }

        if (overpaymentId != null && overpaymentId.length() > OVERPAYMENT_ID_FIELD_LENGTH) {
            throw new IllegalArgumentException("overpayment id must be not longer than " + OVERPAYMENT_ID_FIELD_LENGTH + " chars");
        }
        this.overpaymentId = overpaymentId;
        this.overpaymentIdErrorCode = checkCode(overpaymentIdErrorCode);
        if (paymentYear != null && (paymentYear < 1000 || paymentYear > 9999)) {
            throw new IllegalArgumentException("payment year must be between 1000 and 9999");
        }
        this.paymentYear = paymentYear;
        this.paymentYearErrorCode = checkCode(paymentYearErrorCode);

    }

    public String getOverpaymentId() {
        return overpaymentId;
    }

    public Integer getOverpaymentIdErrorCode() {
        return overpaymentIdErrorCode;
    }

    public Integer getPaymentYear() {
        return paymentYear;
    }

    public Integer getPaymentYearErrorCode() {
        return paymentYearErrorCode;
    }

    @Override
    public int getFillerMaxLength() {
        return FILLER_MAX_LENGTH_WHEN_NO_OVERPAYMENT;
    }

    @Override
    public String toString() {
        StringBuilder record = new StringBuilder(RecordId.BBB.toString())
            .append(formatSequenceNumber(sequenceNumber))
            .append(planNumber)
            .append(pad(overpaymentId == null ? "" : overpaymentId.toString(), OVERPAYMENT_ID_FIELD_LENGTH))
            .append(formatErrorCode(overpaymentIdErrorCode))
            .append(paymentYear == null ? "    " : paymentYear.toString())
            .append(formatErrorCode(paymentYearErrorCode))
            .append(filler);
        return toRapsRecordLength(record);
    }
}
