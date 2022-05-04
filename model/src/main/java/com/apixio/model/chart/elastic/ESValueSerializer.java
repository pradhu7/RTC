package com.apixio.model.chart.elastic;

import com.apixio.security.Security;
import com.apixio.security.exception.ApixioSecurityException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class ESValueSerializer<T> extends JsonSerializer<ESValue<T>> {

    private final Security security = Security.getInstance();

    @Override
    public void serialize(ESValue<T> value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException, ApixioSecurityException {
        if (value.isEncrypted) {
            jsonGenerator.writeString(security.encrypt(value.getJsonValue()));
        } else {
            jsonGenerator.writeString(value.getJsonValue());
        }
    }
}
