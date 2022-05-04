package com.apixio.dao.merge;

import java.util.*;

import com.apixio.model.WithMetadata;
import com.apixio.model.patient.*;
import org.apache.commons.lang.StringUtils;

import org.joda.time.DateTime;


public class MergeHelper {

    /**
     * Check that there is a clinical actor to add, and that it hasn't already been added.
     * Copy only the primary id and role.
     */
    public static void copyClinicalActorPrimaryIdIfNotEmpty(Patient newPatient, Patient oldPatient, UUID clinicalActorId) {
        if (clinicalActorId == null)
            return;
        ClinicalActor clinicalActor = oldPatient.getClinicalActorById(clinicalActorId);
        if (clinicalActor != null
                // not already copied
                && newPatient.getClinicalActorById(clinicalActorId) == null
                // and there is some identifying information
                && clinicalActor.getPrimaryId() != null) {
            ClinicalActor copy = new ClinicalActor();
            copy.setClinicalActorId(clinicalActorId);
            copy.setPrimaryId(clinicalActor.getPrimaryId());
            copy.setRole(clinicalActor.getRole());
            newPatient.addClinicalActor(copy);
        }
    }

    public static void copyClinicalActorPrimaryIdsIfNotEmpty(Patient newPatient, Patient oldPatient, Collection<UUID> clinicalActorIds) {
        if (clinicalActorIds == null)
            return;
        for (UUID clinicalActorId : clinicalActorIds)
            copyClinicalActorPrimaryIdIfNotEmpty(newPatient, oldPatient, clinicalActorId);
    }

    public static void copyEncounterPrimaryIdIfNotEmpty(Patient newPatient, Patient oldPatient, UUID sourceEncounterId) {
        if (sourceEncounterId == null)
            return;
        Encounter sourceEncounter = oldPatient.getEncounterById(sourceEncounterId);
        if (sourceEncounter != null
                && newPatient.getEncounterById(sourceEncounterId) == null
                && sourceEncounter.getOriginalId() != null) {
            Encounter copy = new Encounter();
            copy.setEncounterId(sourceEncounterId);
            copy.setOriginalId(sourceEncounter.getOriginalId());
            newPatient.addEncounter(copy);
        }
    }

//    public static void populateUUIDMap(Map<UUID, BaseObject> uuidMap, Iterable<? extends BaseObject> baseObjects) {
//        for (BaseObject baseObject : baseObjects) {
//            uuidMap.put(baseObject.getInternalUUID(), baseObject);
//        }
//    }


    public static Object mergeObject(Object mergedObject, Object object) {
        // this is the entry point for all object merges

        // merge only makes sense if you have two things to merge
        if (mergedObject != null && object != null) {
            if (mergedObject instanceof Iterable<?>) {
                mergedObject = mergeList((Iterable<?>) mergedObject, (Iterable<?>) object);
            } else if (mergedObject instanceof Encounter) {
            	mergedObject = mergeEncounter((Encounter) mergedObject, (Encounter) object);
            } else if (mergedObject instanceof CareSite) {
                mergedObject = mergeCareSite((CareSite) mergedObject, (CareSite) object);
            } else if (mergedObject instanceof ClinicalActor) {
                mergedObject = mergeClinicalActor((ClinicalActor) mergedObject, (ClinicalActor) object);
            } else if (mergedObject instanceof Document) {
                mergedObject = mergeDocument((Document)mergedObject, (Document)object);
            } else if (mergedObject instanceof Actor) { // has to come after ClinicalActor
                mergedObject = mergeActor((Actor) mergedObject, (Actor) object);
            } else if (mergedObject instanceof ContactDetails) {
                mergedObject = mergeContactDetails((ContactDetails) mergedObject, (ContactDetails) object);
            } else if (mergedObject instanceof Organization) {
                mergedObject = mergeOrganization((Organization) mergedObject, (Organization) object);
            } else if (mergedObject instanceof ExternalID) {
                //mergedObject = mergeWithMetadata((WithMetadata)mergedObject, (WithMetadata)object);
                // TODO: I think we should treat external ids like primitives, right? else delete this and let it fall to WithMetadata -lschneider
                mergedObject = mergePrimitive(mergedObject, object);

            // base classes in order of highest to lowest
            } else if (mergedObject instanceof CodedBaseObject) {
                mergeCodedBaseObject((CodedBaseObject) mergedObject, (CodedBaseObject) object);
            } else if (mergedObject instanceof BaseObject) {
                mergeBaseObject((BaseObject) mergedObject, (BaseObject) object);
            } else if (mergedObject instanceof WithMetadata) {
                mergeWithMetadata((WithMetadata) mergedObject, (WithMetadata) object);

            // default to primitive merge
            } else {
            	//System.out.println("Falling back to primitive merge for " + mergedObject.getClass());
                mergedObject = mergePrimitive(mergedObject, object);
            }

        // else just make sure if one is not null that one gets returned
        } else if (mergedObject == null) {
            mergedObject = object;
        }
        return mergedObject;
    }

    private static Document mergeDocument(Document mergedDocument, Document document)
    {
        mergeCodedBaseObject(mergedDocument, document);
        if (mergedDocument.getDocumentDate() == null) {
            mergedDocument.setDocumentDate(document.getDocumentDate());
        }
        if (mergedDocument.getDocumentTitle() == null){
            mergedDocument.setDocumentTitle(document.getDocumentTitle());
        }
        if (mergedDocument.getDocumentContents() == null || mergedDocument.getDocumentContents().size() == 0){
            mergedDocument.setDocumentContents(document.getDocumentContents());
        }

        return mergedDocument;
    }

    // the most basic merge. Keep the mergedObject unless it is null, in which case take thew new object
    private static Object mergePrimitive(Object mergedObject, Object object) {
        if (mergedObject == null) {
            mergedObject = object;
        }
        return mergedObject;
    }

    public static Map<String, String> mergeStringMap(Map<String, String> mergedStringMap, Map<String, String> stringMap) {
        if (mergedStringMap == null) {
        	mergedStringMap = stringMap;
        } else {
	        if (stringMap != null) {
	            // for each new map item, add if not already in list
	            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
	                if (!mergedStringMap.containsKey(entry.getKey())) {
	                    mergedStringMap.put(entry.getKey(), entry.getValue());
	                }
	            }
	        }
        }
        return mergedStringMap;
    }


	public static String getSummaryKey(ExternalID eid) {
		String summaryKey = null;
		if (eid != null) {
            // if we have both id and aa then make a composite key
			if (StringUtils.isNotBlank(eid.getId()) && StringUtils.isNotBlank(eid.getAssignAuthority()))
            {
                summaryKey = eid.getId() + "^" + eid.getAssignAuthority();
            }
            // else use only assign authority
            else if (StringUtils.isNotBlank(eid.getAssignAuthority()))
            {
                summaryKey = eid.getAssignAuthority();
            }
            // TODO: should we consider the case where id is not null but assign authority is? the parser should not allow this.

			// for legacy data, we may have used source, so fall back on that.
			if (StringUtils.isBlank(summaryKey)) {
				summaryKey = eid.getSource();
			}
		}
		return summaryKey;
	}

	public static DateTime getLatestSourceDate(Patient patient) {
        DateTime sourceDate = null;
        if (patient != null) {
	        for (Source existingSource : patient.getSources()) {
	            DateTime existingSourceDate = existingSource.getCreationDate();
	            if (sourceDate == null || existingSourceDate.isAfter(sourceDate)) {
	                sourceDate = existingSourceDate;
	            }
	        }
        }
        return sourceDate;
    }

    public static DateTime getLatestEditDate(Patient patient) {
        DateTime sourceDate = null;
        if (patient != null) {
            for (Source existingSource : patient.getSources()) {
                DateTime existingEditDate = existingSource.getLastEditDateTime();
                if (sourceDate == null || existingEditDate.isAfter(sourceDate)) {
                    sourceDate = existingEditDate;
                }
            }
        }
        return sourceDate;
    }

    private static <T> Iterable<?> mergeList(Iterable<?> mergedObjectList, Iterable<?> objectList) {

        MappedObjectMerger listMerger = new MappedObjectMerger();
        listMerger.addObjects(mergedObjectList);
        listMerger.addObjects(objectList);

        return new ArrayList(listMerger.getObjects());
    }

    private static CareSite mergeCareSite(CareSite mergedCareSite, CareSite careSite) {
    	 mergeBaseObject(mergedCareSite, careSite);
    	 mergedCareSite.setAddress((Address) mergeObject(mergedCareSite.getAddress(), careSite.getAddress()));
    	 mergedCareSite.setCareSiteName((String) mergeObject(mergedCareSite.getCareSiteName(), careSite.getCareSiteName()));
    	 mergedCareSite.setCareSiteType((CareSiteType) mergeObject(mergedCareSite.getCareSiteType(), careSite.getCareSiteType()));
    	 return mergedCareSite;
    }

    // can we assume mergedEncounter is not null?
    private static Encounter mergeEncounter(Encounter mergedEncounter, Encounter encounter) {
        mergeCodedBaseObject(mergedEncounter, encounter);
        mergedEncounter.setChiefComplaints((List<ClinicalCode>) mergeObject(mergedEncounter.getChiefComplaints(), encounter.getChiefComplaints()));
        mergedEncounter.setEncounterStartDate((DateTime) mergePrimitive(mergedEncounter.getEncounterStartDate(), encounter.getEncounterStartDate()));
        mergedEncounter.setEncounterEndDate((DateTime) mergePrimitive(mergedEncounter.getEncounterEndDate(), encounter.getEncounterEndDate()));
        mergedEncounter.setEncType((EncounterType) mergePrimitive(mergedEncounter.getEncType(), encounter.getEncType()));
        mergedEncounter.setSiteOfService((CareSite) mergeObject(mergedEncounter.getSiteOfService(), encounter.getSiteOfService()));
        return mergedEncounter;
    }

    private static ContactDetails mergeContactDetails(ContactDetails mergedContactDetails, ContactDetails contactDetails) {
        mergeBaseObject(mergedContactDetails, contactDetails);

        // leave primary address TODO unless this is in the merge key it should be included in the merge
        MappedObjectMerger addressMerger = new MappedObjectMerger();
        addressMerger.addObject(contactDetails.getPrimaryAddress());
        addressMerger.addObjects(mergedContactDetails.getAlternateAddresses());
        addressMerger.addObjects(contactDetails.getAlternateAddresses());
        List<Address> mergedAlternateAddresses = new ArrayList<>();
        for (Object addressObject : addressMerger.getObjects())
            mergedAlternateAddresses.add((Address) addressObject);
        mergedContactDetails.setAlternateAddress(mergedAlternateAddresses);

        // leave primary email TODO unless this is in the merge key it should be included in the merge
        MappedObjectMerger emailMerger = new MappedObjectMerger();
        emailMerger.addObject(contactDetails.getPrimaryEmail());
        emailMerger.addObjects(mergedContactDetails.getAlternateEmails());
        emailMerger.addObjects(contactDetails.getAlternateEmails());
        List<String> mergedEmails = new ArrayList<>();
        for (Object emailObject : emailMerger.getObjects())
            mergedEmails.add((String) emailObject);
        mergedContactDetails.setAlternateEmails(mergedEmails);

        // leave primary phone number TODO unless this is in the merge key it should be included in the merge
        MappedObjectMerger phoneMerger = new MappedObjectMerger();
        phoneMerger.addObject(contactDetails.getPrimaryPhone());
        phoneMerger.addObjects(mergedContactDetails.getAlternatePhones());
        phoneMerger.addObjects(contactDetails.getAlternatePhones());
        List<TelephoneNumber> mergedPhoneNumbers = new ArrayList<>();
        for (Object phoneObject : phoneMerger.getObjects())
            mergedPhoneNumbers.add((TelephoneNumber) phoneObject);
        mergedContactDetails.setAlternatePhone(mergedPhoneNumbers);

        return mergedContactDetails;
    }

    private static Organization mergeOrganization(Organization mergedOrganization, Organization organization) {
        mergeBaseObject(mergedOrganization, organization);

        // leaving the primary id as it is
        MappedObjectMerger externalIdMerger = new MappedObjectMerger();
        externalIdMerger.addObject(organization.getPrimaryId());
        externalIdMerger.addObjects(mergedOrganization.getAlternateIds());
        externalIdMerger.addObjects(organization.getAlternateIds());
        List<ExternalID> mergedAlternateIds = new ArrayList<>();
        for (Object alternateIdObject : externalIdMerger.getObjects())
            mergedAlternateIds.add((ExternalID) alternateIdObject);
        mergedOrganization.setAlternateIds(mergedAlternateIds);

        mergedOrganization.setContactDetails((ContactDetails) mergeObject(mergedOrganization.getContactDetails(), organization.getContactDetails()));
        mergedOrganization.setName((String) mergePrimitive(mergedOrganization.getName(), organization.getName()));

        return mergedOrganization;
    }

    private static Actor mergeActor(Actor mergedActor, Actor actor) {
        mergeBaseObject(mergedActor, actor);

        MappedBaseObjectMerger nameMerger = new MappedBaseObjectMerger();
        // mergedActor given name will stay, all other names are pooled to supplemental
        nameMerger.addBaseObject(actor.getActorGivenName());
        nameMerger.addBaseObjects(mergedActor.getActorSupplementalNames());
        nameMerger.addBaseObjects(actor.getActorSupplementalNames());
        List<Name> mergedSupplementalNames = new ArrayList<>();
        for (BaseObject nameObject : nameMerger.getBaseObjects())
            mergedSupplementalNames.add((Name) nameObject);
        mergedActor.setActorSupplementalNames(mergedSupplementalNames);

        // primary id should be the same because its in the merge key
        MappedObjectMerger externalIdMerger = new MappedObjectMerger();
        externalIdMerger.addObjects(mergedActor.getAlternateIds());
        externalIdMerger.addObjects(actor.getAlternateIds());
        List<ExternalID> mergedAlternateIds = new ArrayList<>();
        for (Object alternateIdObject : externalIdMerger.getObjects())
            mergedAlternateIds.add((ExternalID) alternateIdObject);
        mergedActor.setAlternateIds(mergedAlternateIds);

        mergedActor.setAssociatedOrg((Organization) mergeObject(mergedActor.getAssociatedOrg(), actor.getAssociatedOrg()));
        mergedActor.setTitle((String) mergePrimitive(mergedActor.getTitle(), actor.getTitle()));
        return mergedActor;
    }

    private static ClinicalActor mergeClinicalActor(ClinicalActor mergedClinicalActor, ClinicalActor clinicalActor) {
        mergeActor(mergedClinicalActor, clinicalActor);
        mergedClinicalActor.setRole((ActorRole) mergePrimitive(mergedClinicalActor.getRole(), clinicalActor.getRole()));
        mergedClinicalActor.setContactDetails((ContactDetails) mergeObject(mergedClinicalActor.getContactDetails(), clinicalActor.getContactDetails()));
        return mergedClinicalActor;
    }

    private static CodedBaseObject mergeCodedBaseObject(CodedBaseObject mergedCodedBaseObject, CodedBaseObject codedBaseObject) {
    	//TODO: do we ever consider editType here?
    	mergeBaseObject(mergedCodedBaseObject,codedBaseObject);

        MappedObjectMerger codeMerger = new MappedObjectMerger();
        codeMerger.addObject(mergedCodedBaseObject.getCode());
        codeMerger.addObjects(mergedCodedBaseObject.getCodeTranslations());
        codeMerger.addObject(codedBaseObject.getCode());
        codeMerger.addObjects(codedBaseObject.getCodeTranslations());

        // clear first to prevent duplication
        mergedCodedBaseObject.setCode(null);
        mergedCodedBaseObject.setCodeTranslations(new ArrayList<ClinicalCode>());

    	for (Object code : codeMerger.getObjects()) {
    		if (mergedCodedBaseObject.getCode() == null) {
    			mergedCodedBaseObject.setCode((ClinicalCode) code);
    		} else {
    			mergedCodedBaseObject.getCodeTranslations().add((ClinicalCode) code);
    		}
    	}
        //mergedCodedBaseObject.setCode((ClinicalCode)mergeObject(mergedCodedBaseObject.getCode(), codedBaseObject.getCode()));
        //mergedCodedBaseObject.setCodeTranslations((List<ClinicalCode>)mergeObject(mergedCodedBaseObject.getCodeTranslations(), codedBaseObject.getCodeTranslations()));

        // TODO: should we combine all of these together? Probably
        MappedObjectMerger actorIdMerger = new MappedObjectMerger();
        actorIdMerger.addObject(mergedCodedBaseObject.getPrimaryClinicalActorId());
        actorIdMerger.addObjects(mergedCodedBaseObject.getSupplementaryClinicalActorIds());
        actorIdMerger.addObject(codedBaseObject.getPrimaryClinicalActorId());
        actorIdMerger.addObjects(codedBaseObject.getSupplementaryClinicalActorIds());

        // clear first to prevent duplication
        mergedCodedBaseObject.setPrimaryClinicalActorId(null);
        mergedCodedBaseObject.setSupplementaryClinicalActors(new ArrayList<UUID>());

    	for (Object clinicalActorId : actorIdMerger.getObjects()) {
    		if (mergedCodedBaseObject.getPrimaryClinicalActorId() == null) {
    			mergedCodedBaseObject.setPrimaryClinicalActorId((UUID) clinicalActorId);
    		} else {
    			mergedCodedBaseObject.getSupplementaryClinicalActorIds().add((UUID) clinicalActorId);
    		}
    	}
        //mergedCodedBaseObject.setPrimaryClinicalActorId((UUID)mergeObject(mergedCodedBaseObject.getPrimaryClinicalActorId(), codedBaseObject.getPrimaryClinicalActorId()));
        //mergedCodedBaseObject.setSupplementaryClinicalActors((List<UUID>)mergeObject(mergedCodedBaseObject.getSupplementaryClinicalActorIds(), codedBaseObject.getSupplementaryClinicalActorIds()));
        mergedCodedBaseObject.setSourceEncounter((UUID)mergeObject(mergedCodedBaseObject.getSourceEncounter(), codedBaseObject.getSourceEncounter()));

        return mergedCodedBaseObject;
    }

    private static BaseObject mergeBaseObject(BaseObject mergedBaseObject, BaseObject baseObject) {
        // TODO: do we ever consider editType here?
    	mergeWithMetadata(mergedBaseObject, baseObject);

        // how do we want to do these?
        // they should probably be the same, right? Otherwise why are we even merging?
        // do we want to handle these the way we did in demographics?
        //mergedBaseObject.setOriginalId((ExternalID)mergeObject(mergedBaseObject.getOriginalId(), baseObject.getOriginalId()));
        //mergedBaseObject.setOtherOriginalIds((List<ExternalID>)mergeObject(mergedBaseObject.getOtherOriginalIds(), baseObject.getOtherOriginalIds()));

        MappedObjectMerger originalIdMerger = new MappedObjectMerger();
        originalIdMerger.addObject(mergedBaseObject.getOriginalId());
        originalIdMerger.addObjects(mergedBaseObject.getOtherOriginalIds());
        originalIdMerger.addObject(baseObject.getOriginalId());
        originalIdMerger.addObjects(baseObject.getOtherOriginalIds());

        // clear first to prevent duplication
        mergedBaseObject.setOriginalId(null);
        mergedBaseObject.setOtherOriginalIds(new ArrayList<ExternalID>());

    	for (Object originalIdObject : originalIdMerger.getObjects()) {
    		if (mergedBaseObject.getOriginalId() == null) {
    			mergedBaseObject.setOriginalId((ExternalID) originalIdObject);
    		} else {
    			mergedBaseObject.getOtherOriginalIds().add((ExternalID) originalIdObject);
    		}
    	}

        // should we have inspected this earlier? How can this change pop up?
        mergedBaseObject.setEditType((EditType)mergeObject(mergedBaseObject.getEditType(), baseObject.getEditType()));
        mergedBaseObject.setLastEditDateTime((DateTime)mergeObject(mergedBaseObject.getLastEditDateTime(), baseObject.getLastEditDateTime()));

        return mergedBaseObject;
    }


    private static WithMetadata mergeWithMetadata(WithMetadata mergedWithMetadata, WithMetadata withMetadata) {
        mergeStringMap(mergedWithMetadata.getMetadata(), withMetadata.getMetadata());
        return mergedWithMetadata;
    }

    public static List<UUID> mergeIdLists(List<UUID> ids1, List<UUID> ids2) {
        Set<UUID> merged = new HashSet<>();
        merged.addAll(ids1);
        merged.addAll(ids2);
        return new ArrayList<>(merged);
    }
}
