package com.apixio.daoquery;

public class ProgramPerformanceQueryHelper {
    static String addComme(Boolean withComma, String query) {
        if (withComma) {
            return query + ", ";
        } else {
            return query + " ";
        }
    }

    static public String TotalCountQ() {
        return TotalCountQ(true);
    }
    static String TotalCountQ(Boolean withComma) {
        String q = "sum(1) AS {FIELD_TOTAL_COUNT}";
        return addComme(withComma, q);
    }

    static public String ClaimedCountQ() {
        return ClaimedCountQ(true);
    }
    static public String ClaimedCountQ(Boolean withComma) {
        String q = "sum(CAST({FIELD_IS_CLAIMED} AS INTEGER)) AS {FIELD_CLAIMED_COUNT}";
        return addComme(withComma, q);
    }

    static public String ClaimedRafQ() {
        return ClaimedRafQ(true);
    }
    static String ClaimedRafQ(Boolean withComma) {
        String q = "sum(case when {FIELD_IS_CLAIMED} = TRUE and {FIELD_HIERARCHY_FILTERED} = FALSE then {FIELD_REPORTABLE_RAF} else 0.0 end) AS {FIELD_CLAIMED_RAF_COUNT}";
        return addComme(withComma, q);
    }

    static public String OpendRafQ() {
        return OpendRafQ(true);
    }
    static String OpendRafQ(Boolean withComma) {
        String q = "sum(case when {FIELD_IS_CLAIMED} = FALSE and {FIELD_HIERARCHY_FILTERED} = FALSE then {FIELD_REPORTABLE_RAF} else 0.0 end) AS {FIELD_OPEN_RAF_COUNT}";
        return addComme(withComma, q);
    }

    static public String DeliveredCountQ() {
        return DeliveredCountQ(true);
    }
    static String DeliveredCountQ(Boolean withComma) {
        String q = "sum(CAST({FIELD_DELIVERY_DATE} > 0 or {FIELD_DECISION} = 'delivered' AS INTEGER)) AS {FIELD_DELIVERED_COUNT}";
        return addComme(withComma, q);
    }

    static public String NotDeliveredCountQ() {
        return NotDeliveredCountQ(true);
    }
    static String NotDeliveredCountQ(Boolean withComma) {
        String q = "(count(*) - sum(CAST({FIELD_DELIVERY_DATE} > 0 or {FIELD_DECISION} = 'delivered' AS INTEGER))) AS {FIELD_NOT_DELIVERED_COUNT}";
        return addComme(withComma, q);
    }

    static public String AcceptedCountQ() {
        return AcceptedCountQ(true);
    }
    static String AcceptedCountQ(Boolean withComma) {
        String q = "sum(CAST({FIELD_RESPONSE_DATE} > 0 and {FIELD_DECISION} = 'accepted' AS INTEGER)) AS {FIELD_ACCEPTED_COUNT}";
        return addComme(withComma, q);
    }

    static public String RejectedCountQ() {
        return RejectedCountQ(true);
    }
    static String RejectedCountQ(Boolean withComma) {
        String q = "sum(CAST({FIELD_RESPONSE_DATE} > 0 and {FIELD_DECISION} = 'rejected' AS INTEGER)) AS {FIELD_REJECTED_COUNT}";
        return addComme(withComma, q);
    }

    static public String SnoozedCountQ() {
        return SnoozedCountQ(true);
    }
    static String SnoozedCountQ(Boolean withComma) {
        String q = "sum(CAST({FIELD_RESPONSE_DATE} > 0 and {FIELD_DECISION} = 'snooze' AS INTEGER)) AS {FIELD_SNOOZED_COUNT}";
        return addComme(withComma, q);
    }

    static public String AcceptedOnClaimedCountQ(Boolean withComma) {
        String q = "sum(CAST({FIELD_RESPONSE_DATE} > 0 and {FIELD_DECISION} = 'accepted' and {FIELD_HIERARCHY_FILTERED} = TRUE AS INTEGER)) AS {FIELD_ACCEPT_ON_CLAIMED_COUNT}";
        return addComme(withComma, q);
    }

    static public String responseCountQ() {
        return responseCountQ(true);
    }
    static String responseCountQ(Boolean withComma) {
        String q = "sum(CAST({FIELD_RESPONSE_DATE} > 0 and ({FIELD_DECISION} = 'accepted' or {FIELD_DECISION} = 'rejected') AS INTEGER)) AS {FIELD_RESPONSE_COUNT}";
        return addComme(withComma, q);
    }

    static public String responseRateQ() {
        return responseRateQ(true);
    }
    static public String responseRateQ(Boolean withComma) {
        String q = "CASE WHEN sum(CASE WHEN {FIELD_DELIVERY_DATE} > 0 or {FIELD_DECISION} = 'delivered' THEN 1 ELSE 0 END) > 0 THEN" +
                   " sum(CASE WHEN {FIELD_RESPONSE_DATE} > 0 and ({FIELD_DECISION} = 'accepted' or {FIELD_DECISION} = 'rejected') THEN 1 ELSE 0 END)" +
                   "  / sum(CASE WHEN {FIELD_DELIVERY_DATE} > 0 or {FIELD_DECISION} = 'delivered' THEN 1 ELSE 0 END)" +
                   " ELSE 0 END as responseRate";
        return addComme(withComma, q);
    }

    static String acceptRateQ() {
        return acceptRateQ(true);
    }
    static public String acceptRateQ(Boolean withComma) {
        String q = "CASE WHEN sum(CASE WHEN {FIELD_RESPONSE_DATE} > 0 and ({FIELD_DECISION} = 'accepted' or {FIELD_DECISION} = 'rejected') THEN 1 ELSE 0 END) > 0 THEN\n" +
            " sum(CASE WHEN {FIELD_RESPONSE_DATE} > 0 and {FIELD_DECISION} = 'accepted' THEN 1 ELSE 0 END) " +
            "  / sum(CASE WHEN {FIELD_RESPONSE_DATE} > 0 and ({FIELD_DECISION} = 'accepted' or {FIELD_DECISION} = 'rejected') THEN 1 ELSE 0 END)" +
            " ELSE 0 END as acceptRate";
        return addComme(withComma, q);
    }
}
