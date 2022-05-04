package com.apixio.model.utility;

import static org.junit.Assert.assertEquals;

import com.apixio.XUUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;

public class XUUIDSerializationTest
{

    ObjectMapper mapper;

    @Before
    public void beforeTest() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(XUUID.class, new ApixioXUUIDSerializer());
        module.addDeserializer(XUUID.class, new ApixioXUUIDDeserializer());
        mapper.registerModule(module);
    }

    @Test
    public void serializationXUUIDWorksWithNull() throws Exception {
        final XUUID testUuid = XUUID.create(null);
        String serialized = mapper.writeValueAsString(testUuid);
        final XUUID deserialized = mapper.readValue(serialized, XUUID.class);
        assertEquals(testUuid, deserialized);
    }

    @Test
    public void serializationXUUIDWorksWithType() throws Exception {
        final XUUID testUuid = XUUID.create("exampleType");
        String serialized = mapper.writeValueAsString(testUuid);
        final XUUID deserialized = mapper.readValue(serialized, XUUID.class);
        assertEquals(testUuid, deserialized);
    }
}
