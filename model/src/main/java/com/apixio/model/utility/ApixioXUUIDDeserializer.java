package com.apixio.model.utility;

import com.apixio.XUUID;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class ApixioXUUIDDeserializer extends JsonDeserializer<XUUID>
{
    @Override
    public XUUID deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
        return jp == null ? null : XUUID.fromString(jp.getText());
    }
}
