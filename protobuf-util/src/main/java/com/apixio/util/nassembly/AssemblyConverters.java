package com.apixio.util.nassembly;

import com.apixio.datacatalog.GenderOuterClass;
import com.apixio.datacatalog.EditTypeOuterClass;
import com.apixio.datacatalog.MaritalStatusOuterClass;
import com.apixio.model.patient.EditType;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.MaritalStatus;

public class AssemblyConverters {

    public static GenderOuterClass.Gender convertGender(Gender gender) {
        switch (gender) {
            case MALE: return GenderOuterClass.Gender.MALE;
            case FEMALE: return GenderOuterClass.Gender.FEMALE;
            case TRANSGENDER: return GenderOuterClass.Gender.TRANSGENDER;
        }
        return GenderOuterClass.Gender.UNKNOWN;
    }

    public static EditTypeOuterClass.EditType convertEditType(EditType editType) {
        switch (editType) {
            case ACTIVE: return EditTypeOuterClass.EditType.ACTIVE;
            case DELETE: return EditTypeOuterClass.EditType.DELETE;
            case ARCHIVE: return EditTypeOuterClass.EditType.ARCHIVE;
        }
        return EditTypeOuterClass.EditType.ACTIVE;
    }

    public static MaritalStatusOuterClass.MaritalStatus convertMaritalStatus(MaritalStatus maritalStatus) {
        // TODO! Safer conversion for enum evolution
        switch (maritalStatus) {
            case ANNULLED: return MaritalStatusOuterClass.MaritalStatus.ANNULLED;
            case DIVORCED: return MaritalStatusOuterClass.MaritalStatus.DIVORCED;
            case INTERLOCUTORY: return MaritalStatusOuterClass.MaritalStatus.INTERLOCUTORY;
            case LEGALLY_SEPARATED: return MaritalStatusOuterClass.MaritalStatus.LEGALLY_SEPARATED;
            case MARRIED: return MaritalStatusOuterClass.MaritalStatus.MARRIED;
            case POLYGAMOUS: return MaritalStatusOuterClass.MaritalStatus.POLYGAMOUS;
            case NEVER_MARRIED: return MaritalStatusOuterClass.MaritalStatus.NEVER_MARRIED;
            case DOMESTIC_PARTNER: return MaritalStatusOuterClass.MaritalStatus.DOMESTIC_PARTNER;
            case WIDOWED: return MaritalStatusOuterClass.MaritalStatus.WIDOWED;
            case UNMARRIED: return MaritalStatusOuterClass.MaritalStatus.UNMARRIED;
        }
        return MaritalStatusOuterClass.MaritalStatus.UNKNOWN_MARITAL_STATUS;
    }

}
