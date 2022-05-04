package com.apixio.util.nassembly;

import com.apixio.datacatalog.ClinicalCodeOuterClass;
import com.apixio.datacatalog.ExternalIdOuterClass.ExternalId;
import com.apixio.model.external.CaretParserException;

public class CaretParser {

    public static String toString(ExternalId eid) {
        if (eid.getId().isEmpty()) {
            return eid.getAssignAuthority();
        } else {
            return eid.getId() + "^^" + eid.getAssignAuthority();
        }
    }

    public static String toString(ClinicalCodeOuterClass.ClinicalCode code) {
        StringBuilder b = new StringBuilder();

        if (!code.getDisplayName().isEmpty()) {
            b.append(code.getDisplayName());
        }
        b.append("^");

        if (!code.getCode().isEmpty()) {
            b.append(code.getCode());
        }
        b.append("^");

        if (!code.getSystem().isEmpty()) {
            b.append(code.getSystem());
        }
        b.append("^");

        if (!code.getSystemOid().isEmpty()) {
            b.append(code.getSystemOid());
        }
        b.append("^");

        if (!code.getSystemVersion().isEmpty()) {
            b.append(code.getSystemVersion());
        }

        return b.toString();
    }

    public static ExternalId toExternalId (String str) throws CaretParserException {
        ExternalId.Builder builder = ExternalId.newBuilder();
        String[] parts = str.split("\\^");
        if (parts.length == 1) {
            String authority = parts[0].trim();
            if (authority.isEmpty()) {
                throw new CaretParserException("ExternalId is empty");
            }
            builder.setAssignAuthority(authority);
        } else if (parts.length == 3) {
            String id = parts[0].trim();
            if (id.isEmpty()) {
                throw new CaretParserException("ExternalId.id is empty");
            }
            builder.setId(id);

            // parts[1] is ignored for historical reasons

            String authority = parts[2].trim();
            if (authority.isEmpty()) {
                throw new CaretParserException("ExternalId.assignAuthority is empty");
            }
            builder.setAssignAuthority(authority);
        } else {
            throw new CaretParserException("ExternalId has 1 or more than 2 carets");
        }

        return builder.build();
    }
}
