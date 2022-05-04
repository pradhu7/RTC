package com.apixio.nassembly.merger;

import com.apixio.datacatalog.*;
import com.apixio.util.nassembly.DataCatalogProtoUtils;
import org.joda.time.DateTime;

import java.util.UUID;

public class ProtoGenerator {

    public static UUIDOuterClass.UUID generateUUID() {
        return DataCatalogProtoUtils.convertUuid(UUID.randomUUID());
    }

    public static ParsingDetailsOuterClass.ParsingDetails generateParsingDetails() {
        return ParsingDetailsOuterClass.ParsingDetails.newBuilder()
                .setInternalId(generateUUID())
                .build();
    }

    public static SourceOuterClass.Source generateSource() {
        return SourceOuterClass.Source.newBuilder()
                .setInternalId(generateUUID())
                .build();
    }

    public static DataCatalogMetaOuterClass.DataCatalogMeta generateDataCatalogMeta() {
        return DataCatalogMetaOuterClass.DataCatalogMeta.newBuilder()
                .setEditType(EditTypeOuterClass.EditType.ACTIVE)
                .setLastEditTime(DateTime.now().getMillis())
                .setOriginalId(ExternalIdOuterClass.ExternalId.newBuilder().setId("fakeId").build())
                .addOtherOriginalIds(ExternalIdOuterClass.ExternalId.newBuilder().setId(UUID.randomUUID().toString()).build())
                .build();
    }

    public static NameOuterClass.Name generateName() {
        return NameOuterClass.Name.newBuilder()
                .addFamilyNames("last name")
                .addGivenNames("first name")
                .build();
    }

    public static ContactInfoOuterClass.ContactInfo generateContactInfo() {
        return ContactInfoOuterClass.ContactInfo.newBuilder()
                .setPrimaryEmail("email")
                .setPrimaryPhone(ContactInfoOuterClass.TelephoneNumber.newBuilder().setPhoneNumber("18001234567").build())
                .build();
    }
}
