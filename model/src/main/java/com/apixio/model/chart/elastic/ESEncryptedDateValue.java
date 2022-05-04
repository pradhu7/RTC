package com.apixio.model.chart.elastic;

import com.apixio.security.Security;
import com.apixio.security.exception.ApixioSecurityException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.Map;

@JsonDeserialize(using = ESEncryptedDateValue.ESEncryptedDateValueDeserializer.class)
public class ESEncryptedDateValue extends ESValue<DateTime> {

    public static DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();

    public ESEncryptedDateValue(DateTime value) {
        super(value, true);
    }

    @Override
    public String getJsonValue() {
        return value.toString(dateFormatter);
    }

    public static Map<String, Object> getElasticTypeMapping() {
        //TODO: since this date is encrypted, type should be text??
        return ImmutableMap.of("type", "text");
    }

    public static class ESEncryptedDateValueDeserializer extends JsonDeserializer<ESDateValue> {

        private final Security security = Security.getInstance();

        @Override
        public ESDateValue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException, ApixioSecurityException {
            JsonNode node = jsonParser.readValueAsTree();
            return new ESDateValue(DateTime.parse(security.decrypt(node.textValue()), ESDateValue.dateFormatter));
        }
    }
}
