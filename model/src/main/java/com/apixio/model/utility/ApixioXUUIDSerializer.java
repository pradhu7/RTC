package com.apixio.model.utility;

import com.apixio.XUUID;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class ApixioXUUIDSerializer extends JsonSerializer<XUUID>
{
    @Override
    public void serialize(XUUID value, JsonGenerator jgen, SerializerProvider provider) throws IOException
    {
        jgen.writeString(value.toString());
    }
}

