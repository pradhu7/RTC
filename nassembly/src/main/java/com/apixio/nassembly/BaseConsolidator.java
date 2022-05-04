package com.apixio.nassembly;

import com.apixio.datacatalog.*;
import com.apixio.datacatalog.BaseObjects.ClinicalActor;
import com.apixio.datacatalog.CareSiteOuterClass.CareSite;
import com.apixio.datacatalog.ParsingDetailsOuterClass.ParsingDetails;
import com.apixio.datacatalog.SourceOuterClass.Source;
import com.apixio.datacatalog.UUIDOuterClass.UUID;
import com.apixio.util.nassembly.IdentityFunctions;

import java.util.*;
import java.util.stream.Collectors;

public class BaseConsolidator {

    static class SortActorByCreation implements Comparator<ClinicalActor> {
        public int compare(ClinicalActor a, ClinicalActor b) {
            return -1 * Long.compare(
                    a.getBase().getDataCatalogMeta().getLastEditTime(),
                    b.getBase().getDataCatalogMeta().getLastEditTime()
            );
        }
    }

    static class SortMetaByEditTime implements Comparator<DataCatalogMetaOuterClass.DataCatalogMeta> {
        public int compare(DataCatalogMetaOuterClass.DataCatalogMeta a, DataCatalogMetaOuterClass.DataCatalogMeta b) {
            return -1 * Long.compare(a.getLastEditTime(), b.getLastEditTime());
        }
    }

    static class SortSourceByCreation implements Comparator<Source> {
        public int compare(Source a, Source b) {
            return -1 * Long.compare(a.getCreationDate(), b.getCreationDate());
        }
    }

    static class SortParsingDetailsByDate implements Comparator<ParsingDetails> {
        public int compare(ParsingDetails a, ParsingDetails b) {
            return -1 * Long.compare(a.getParsingDate(), b.getParsingDate());
        }
    }

    /**
     * Creates a map of clinical actor internalId to Clinical Actor proto
     * @param protos list of clinical actors
     * @return
     */
    public static Map<UUID, ClinicalActor> clinicalActorMap(List<ClinicalActor> protos) {
        List<ClinicalActor> sortable = new ArrayList<>(protos);
        sortable.sort(new SortActorByCreation());
        Map<UUID, ClinicalActor> dedupMap = new HashMap<>();

        for (ClinicalActor proto : sortable) {
            UUID id = proto.getInternalId();

            if (!dedupMap.containsKey(id)) {
                dedupMap.put(id, proto);
            }
        }
        return dedupMap;
    }

    /**
     * Creates a map of source internalId to Source proto
     * @param protos list of sources
     * @return
     */
    public static Map<UUID, Source> sourcesMap(List<Source> protos) {
        List<Source> sortable = new ArrayList<>(protos);
        sortable.sort(new SortSourceByCreation());
        Map<UUID, Source> dedupMap = new HashMap<>();

        for (Source proto : sortable) {
            UUID id = proto.getInternalId();

            if (!dedupMap.containsKey(id)) {
                dedupMap.put(id, proto);
            }
        }
        return dedupMap;
    }

    public static List<Source> dedupSources(List<Source> protos) {
        return new ArrayList<>(sourcesMap(protos).values());
    }

    /**
     * Creates a map of parsing details internalId to Parsing Details proto
     * @param protos list of parsing details
     * @return
     */
    public static Map<UUID, ParsingDetails> parsingDetailsMap(List<ParsingDetails> protos) {
        List<ParsingDetails> sortable = new ArrayList<>(protos);
        sortable.sort(new SortParsingDetailsByDate());
        Map<UUID, ParsingDetails> dedupMap = new HashMap<>();

        for (ParsingDetails proto : sortable) {
            UUID id = proto.getInternalId();

            if (!dedupMap.containsKey(id)) {
                dedupMap.put(id, proto);
            }
        }
        return dedupMap;
    }

    public static List<ParsingDetails> dedupParsingDetails(List<ParsingDetails> protos) {
        return new ArrayList<>(parsingDetailsMap(protos).values());
    }

    public static Map<UUID, CareSite> careSiteMap(List<CareSite> protos) {
        Map<UUID, CareSite> dedupMap = new HashMap<>();

        for (CareSite proto : protos) {
            UUID id = IdentityFunctions.identityToUUID(IdentityFunctions.getIdentity(proto));

            if (!dedupMap.containsKey(id)) {
                dedupMap.put(id, proto);
            }
        }
        return dedupMap;
    }

    public static List<CareSite> dedupCareSites(List<CareSite> protos) {
        return new ArrayList<>(careSiteMap(protos).values());
    }

    public static PatientMetaProto.PatientMeta mergePatientMeta(List<PatientMetaProto.PatientMeta> patientMetas) {
        PatientMetaProto.PatientMeta.Builder builder = PatientMetaProto.PatientMeta.newBuilder();
        if (patientMetas.isEmpty()) return null;

        Optional<UUID> patientId = patientMetas.stream()
                .filter(PatientMetaProto.PatientMeta::hasPatientId)
                .map(PatientMetaProto.PatientMeta::getPatientId)
                .findFirst();

        patientId.ifPresent(builder::setPatientId);


        // Other Ids are union of all combined primary and other Ids sans primary key
        Set<ExternalIdOuterClass.ExternalId> otherIds = patientMetas.stream()
                .flatMap(pm -> pm.getExternalIdsList().stream())
                .collect(Collectors.toSet());


        Optional<ExternalIdOuterClass.ExternalId> primaryId = patientMetas.stream()
                .filter(PatientMetaProto.PatientMeta::hasPrimaryExternalId)
                .map(PatientMetaProto.PatientMeta::getPrimaryExternalId)
                .findFirst();

        primaryId.ifPresent(builder::setPrimaryExternalId);
        primaryId.ifPresent(otherIds::add); // it's a set, so this is safe

        builder.addAllExternalIds(otherIds);

        return builder.build();
    }

    public static DataCatalogMetaOuterClass.DataCatalogMeta mergeCatalogMeta(
            List<DataCatalogMetaOuterClass.DataCatalogMeta> metas) {
        if (metas.isEmpty()) return null;

        List<DataCatalogMetaOuterClass.DataCatalogMeta> sortable = new ArrayList<>(metas);
        sortable.sort(new SortMetaByEditTime());

        DataCatalogMetaOuterClass.DataCatalogMeta head = sortable.get(0);
        DataCatalogMetaOuterClass.DataCatalogMeta.Builder builder = head.toBuilder();


        // if original id is null, set it.
        if (!builder.hasOriginalId()) {
            sortable.stream()
                    .filter(DataCatalogMetaOuterClass.DataCatalogMeta::hasOriginalId)
                    .findAny()
                    .ifPresent(meta -> builder.setOriginalId(meta.getOriginalId()));
        }

        Set<ExternalIdOuterClass.ExternalId> otherOriginalIds = metas.stream()
                .flatMap(m -> m.getOtherOriginalIdsList().stream())
                .collect(Collectors.toSet());

        // Add originalIds to otherOriginalids list
        sortable.stream()
                .filter(DataCatalogMetaOuterClass.DataCatalogMeta::hasOriginalId)
                .filter(meta -> !builder.getOriginalId().equals(meta.getOriginalId()))
                .forEach(meta -> otherOriginalIds.add(meta.getOriginalId()));

        builder
                .clearOtherOriginalIds()
                .addAllOtherOriginalIds(otherOriginalIds);

        return builder.build();
    }

    public static List<ClinicalCodeOuterClass.ClinicalCode> mergeCodes(
            List<ClinicalCodeOuterClass.ClinicalCode> codes) {
        Map<String, ClinicalCodeOuterClass.ClinicalCode> mergeMap = new HashMap<>();

        for (ClinicalCodeOuterClass.ClinicalCode c : codes) {
            String id = IdentityFunctions.getIdentity(c);
            if (mergeMap.containsKey(id)) {
                ClinicalCodeOuterClass.ClinicalCode existing = mergeMap.get(id);
                if (existing.getDisplayName().isEmpty() && !c.getDisplayName().isEmpty()) {
                    mergeMap.put(id, c); // Replace if display name exists if new code, but not existing
                }
            } else {
                mergeMap.put(id, c);
            }
        }
        return new ArrayList<>(mergeMap.values());
    }

    public static BaseObjects.Base mergeBase(List<BaseObjects.Base> bases) {
        DataCatalogMetaOuterClass.DataCatalogMeta catalogMeta = mergeCatalogMeta(bases.stream().map(BaseObjects.Base::getDataCatalogMeta).collect(Collectors.toList()));
        Set<UUID> sourceIds = bases.stream().flatMap(c -> c.getSourceIdsList().stream()).collect(Collectors.toSet());
        Set<UUID> parsingDetailsIds =
                bases.stream().flatMap(c -> c.getParsingDetailsIdsList().stream()).collect(Collectors.toSet());

        return BaseObjects.Base.newBuilder()
                .setDataCatalogMeta(catalogMeta)
                .addAllSourceIds(sourceIds)
                .addAllParsingDetailsIds(parsingDetailsIds)
                .build();
    }

    public static CodedBaseObjects.CodedBase mergedCodedBases(List<CodedBaseObjects.CodedBase> codedBases) {
        DataCatalogMetaOuterClass.DataCatalogMeta catalogMeta = mergeCatalogMeta(codedBases.stream().map(CodedBaseObjects.CodedBase::getDataCatalogMeta).collect(Collectors.toList()));
        Set<UUID> sourceIds = codedBases.stream().flatMap(c -> c.getSourceIdsList().stream()).collect(Collectors.toSet());
        Set<UUID> parsingDetailsId =
                codedBases.stream().flatMap(c -> c.getParsingDetailsIdsList().stream()).collect(Collectors.toSet());
        Optional<UUID> primaryActorIdOpt = codedBases.stream().map(CodedBaseObjects.CodedBase::getPrimaryActorId).findFirst();

        CodedBaseObjects.CodedBase.Builder builder = CodedBaseObjects.CodedBase.newBuilder();
        builder.setDataCatalogMeta(catalogMeta);
        builder.addAllSourceIds(sourceIds);
        builder.addAllParsingDetailsIds(parsingDetailsId);
        if (primaryActorIdOpt.isPresent()) {
            builder.setPrimaryActorId(primaryActorIdOpt.get());

            Set<UUID> supplementaryActorIds = codedBases.stream().flatMap(c -> c.getSourceIdsList().stream())
                    .filter(id -> !primaryActorIdOpt.get().equals(id))
                    .collect(Collectors.toSet());
        }
        else {
            Set<UUID> allActorIds = codedBases.stream().flatMap(c -> c.getSourceIdsList().stream())
                    .collect(Collectors.toSet());

            if (!allActorIds.isEmpty()) {
                UUID primaryId = allActorIds.iterator().next();
                builder.setPrimaryActorId(primaryId);
                allActorIds.remove(primaryId);
                builder.addAllSupplementaryActorIds(allActorIds);
            }
        }

        return builder.build();
    }

    public static SummaryObjects.BaseSummary mergeBaseSummaries(List<SummaryObjects.BaseSummary> bases) {
        DataCatalogMetaOuterClass.DataCatalogMeta catalogMeta = mergeCatalogMeta(bases.stream().map(SummaryObjects.BaseSummary::getDataCatalogMeta).collect(Collectors.toList()));
        PatientMetaProto.PatientMeta patientMeta = mergePatientMeta(bases.stream().map(SummaryObjects.BaseSummary::getPatientMeta).collect(Collectors.toList()));

        List<SourceOuterClass.Source> sources = dedupSources(bases.stream().flatMap(b -> b.getSourcesList().stream()).collect(Collectors.toList()));
        List<ParsingDetailsOuterClass.ParsingDetails> parsingDetails = dedupParsingDetails(bases.stream().flatMap(b -> b.getParsingDetailsList().stream()).collect(Collectors.toList()));

        return SummaryObjects.BaseSummary.newBuilder()
                .setDataCatalogMeta(catalogMeta)
                .setPatientMeta(patientMeta)
                .addAllSources(sources)
                .addAllParsingDetails(parsingDetails)
                .build();
    }
}
