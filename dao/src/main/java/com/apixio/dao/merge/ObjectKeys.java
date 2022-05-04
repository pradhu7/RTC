package com.apixio.dao.merge;

import com.apixio.model.patient.*;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.helpers.LogLog;

public class ObjectKeys {

    public static String getObjectKey(Object object) {
        String objectKey = null;
        if (object != null) {
            try
            {
                if (object instanceof Demographics)
                {
                    objectKey = getDemographicsKey((Demographics) object);
                } else if (object instanceof ContactDetails)
                {
                    objectKey = getContactDetailsKey((ContactDetails) object);
                } else if (object instanceof ExternalID)
                {
                    objectKey = getExternalIDKey((ExternalID) object);
                } else if (object instanceof ClinicalCode)
                {
                    objectKey = getClinicalCodeKey((ClinicalCode) object);
                } else if (object instanceof ClinicalActor)
                {
                    objectKey = getClinicalActorKey((ClinicalActor) object);
                } else if (object instanceof Name)
                {
                    objectKey = String.valueOf(object.hashCode());
                } else if (object instanceof Document)
                {
                    objectKey = getDocumentKey((Document) object);
                } else if (object instanceof BaseObject)
                {
                    // most base objects will be keyed by original id
                    // special cases will come above
                    objectKey = getBaseObjectKey((BaseObject) object);
                } else
                {
                    objectKey = object.toString();
                    // TODO: remove system out debug message
                    //System.out.println("Using default object key: " + objectKey);
                }
            }
            catch (Exception ex)
            {
                LogLog.warn("Error getting object key, defaulting to null", ex);
            }
        }
        return objectKey;
    }

    private static String getBaseObjectKey(BaseObject baseObject) {
        if (baseObject.getOriginalId() == null)
            return baseObject.toString(); // just in case of no original id
        return getExternalIDKey(baseObject.getOriginalId());
    }

    private static String getDocumentKey(Document document) {
        return document.getInternalUUID().toString(); // another option that would merge down further is original id
    }

    private static String getClinicalActorKey(ClinicalActor clinicalActor) {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(clinicalActor.getPrimaryId().getId())
                .append(clinicalActor.getPrimaryId().getAssignAuthority())
                .append(clinicalActor.getRole());
        return String.valueOf(hashCodeBuilder.toHashCode());
    }

    private static String getClinicalCodeKey(ClinicalCode clinicalCode) {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(clinicalCode.getCode()).append(clinicalCode.getCodingSystem())
                .append(clinicalCode.getCodingSystemOID()).append(clinicalCode.getCodingSystemVersions())
                .append(clinicalCode.getDisplayName());
        return String.valueOf(hashCodeBuilder.toHashCode());
    }

    private static String getContactDetailsKey(ContactDetails contactDetails) {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(contactDetails.getPrimaryAddress()).append(contactDetails.getPrimaryEmail()).append(contactDetails.getPrimaryPhone());
        if (contactDetails.getAlternateAddresses() != null) {
            hashCodeBuilder.append(contactDetails.getAlternateAddresses().toArray());
        }
        if (contactDetails.getAlternateEmails() != null) {
            hashCodeBuilder.append(contactDetails.getAlternateEmails().toArray());
        }
        if (contactDetails.getAlternatePhones() != null) {
            hashCodeBuilder.append(contactDetails.getAlternatePhones().toArray());
        }
        return String.valueOf(hashCodeBuilder.toHashCode());
    }

    private static String getDemographicsKey(Demographics demographics) {
        //String demographicsKey;
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
                .append(demographics.getDateOfBirth()).append(demographics.getEthnicity())
                .append(demographics.getGender()).append(demographics.getMaritalStatus())
                .append(demographics.getRace()).append(demographics.getReligiousAffiliation());
        if (demographics.getName() != null) {
            hashCodeBuilder.append(demographics.getName().getNameType())
                    .append(demographics.getName().getPrefixes()).append(demographics.getName().getGivenNames())
                    .append(demographics.getName().getFamilyNames()).append(demographics.getName().getSuffixes());
        }
        if (demographics.getLanguages() != null) {
            hashCodeBuilder.append(demographics.getLanguages().toArray());
        }
        return String.valueOf(hashCodeBuilder.toHashCode());
    }

    private static String getExternalIDKey(ExternalID externalId) {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
                .append(externalId.getAssignAuthority()).append(externalId.getId())
                .append(externalId.getSource()).append(externalId.getType());
        return String.valueOf(hashCodeBuilder.toHashCode());
    }

}
