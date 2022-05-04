package com.apixio.nassembly.merger;

import com.apixio.datacatalog.ClinicalActorInfoOuterClass.ClinicalActorInfo;
import com.apixio.datacatalog.ContactInfoOuterClass.ContactInfo;
import com.apixio.datacatalog.DataCatalogMetaOuterClass.DataCatalogMeta;
import com.apixio.datacatalog.ExternalIdOuterClass;
import com.apixio.datacatalog.NameOuterClass.Name;
import com.apixio.datacatalog.SummaryObjects;
import com.apixio.nassembly.clinicalactor.ClinicalActorExchange;
import com.apixio.nassembly.clinicalactor.ClinicalActorMerger;
import org.junit.Before;
import org.junit.Test;
import scala.collection.Iterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.apixio.nassembly.merger.ProtoGenerator.*;

public class ClinicalActorMergerTest {


    public static ClinicalActorInfo createBaseInfo() {
        ClinicalActorInfo.Builder builder = ClinicalActorInfo.newBuilder();
        builder.setActorRole("FAKE_ROLE");
        builder.setPrimaryId(
                ExternalIdOuterClass.ExternalId.newBuilder().setAssignAuthority("NPI").setId("1234567890").build()
        );
        return builder.build();
    }

    public static ClinicalActorInfo setName(ClinicalActorInfo actorInfo, Name name) {
        return actorInfo.toBuilder()
                .setActorGivenName(name)
                .build();
    }

    public static ClinicalActorInfo setContactDetails(ClinicalActorInfo actorInfo, ContactInfo contactInfo) {
        return actorInfo.toBuilder()
                .setContactDetails(contactInfo)
                .build();
    }

    public static SummaryObjects.ClinicalActorSummary createClinicalActor(ClinicalActorInfo actorInfo) {
        SummaryObjects.ClinicalActorSummary.Builder builder = SummaryObjects.ClinicalActorSummary.newBuilder();
        builder.setClinicalActorInfo(actorInfo);
        builder.addParsingDetails(generateParsingDetails());
        builder.addSources(generateSource());
        builder.setDataCatalogMeta(generateDataCatalogMeta());
        return builder.build();
    }

    public static ClinicalActorExchange createExchange(SummaryObjects.ClinicalActorSummary summary) {
        ClinicalActorExchange exchange = new ClinicalActorExchange();
        exchange.fromProto(Collections.singleton(summary.toByteArray()));
        return exchange;
    }


    @Before
    public void setUp()
    {
    }

    @Test
    public void testMerge()
    {
        ArrayList<SummaryObjects.ClinicalActorSummary> actors = new ArrayList<>();
        int actorCount = 1000;
        for (int i = 0; i < actorCount; i++){
            ClinicalActorInfo info = createBaseInfo();
                if (i % 21 + 1 == 21) // not the first element
                    info = setName(info, generateName());
                if (i % 23 + 1 == 23) // not the first element
                    info = setContactDetails(info, generateContactInfo());
            actors.add(createClinicalActor(info));
        }

        long startTimeMs = System.currentTimeMillis();
        List<ClinicalActorExchange> exchanges = actors
                .stream()
                .map(ClinicalActorMergerTest::createExchange)
                .collect(Collectors.toList());

        ClinicalActorMerger merger = new ClinicalActorMerger();
        ClinicalActorExchange mergedExchange = merger.merge(exchanges);
        long endTimeMs = System.currentTimeMillis();

        assert((endTimeMs - startTimeMs) < 2000);
        System.out.println("Merge for " + actorCount + " actors took " + (endTimeMs - startTimeMs) + " milliseconds");

        Iterator<SummaryObjects.ClinicalActorSummary> mergedSummaries = mergedExchange.getActors().iterator();
        SummaryObjects.ClinicalActorSummary mergedActor = mergedSummaries.next();
        // Should only be 1 actor
        
        ClinicalActorInfo mergedInfo = mergedActor.getClinicalActorInfo();

        // Check that the merge preserved info
        assert(mergedActor.hasClinicalActorInfo());
        assert(mergedInfo.hasContactDetails());
        assert(mergedInfo.getActorGivenName().getGivenNamesCount() == 1);
        assert(mergedInfo.getActorGivenName().getFamilyNamesCount() == 1);

        // tracking data
        assert(mergedActor.getParsingDetailsCount() == actorCount || mergedActor.getParsingDetailsCount() == 100);
        assert(mergedActor.getSourcesCount() == actorCount || mergedActor.getSourcesCount() == 100);

        // check catalog meta
        DataCatalogMeta meta = mergedActor.getDataCatalogMeta();
        long maxEditTime = actors.stream().map(a -> a.getDataCatalogMeta().getLastEditTime()).max(Long::compare).get();
        assert(meta.getLastEditTime() == maxEditTime);
        assert(meta.getOtherOriginalIdsList().size() == actorCount);
    }
}
