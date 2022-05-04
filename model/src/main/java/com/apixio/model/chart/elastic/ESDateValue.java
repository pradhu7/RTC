package com.apixio.model.chart.elastic;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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

@JsonDeserialize(using = ESDateValue.ESDateValueDeserializer.class)
public class ESDateValue extends ESValue<DateTime> {

    public static DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();

    public ESDateValue(DateTime value) {
        super(value, false);
    }

    @Override
    public String getJsonValue() {
        return value.toString(dateFormatter);
    }

    public static Map<String, Object> getElasticTypeMapping() {
        //TODO: what format of dates do we want to specify??
        return ImmutableMap.of("type", "date");
    }

    public static class ESDateValueDeserializer extends JsonDeserializer<ESDateValue> {

        @Override
        public ESDateValue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JsonNode node = jsonParser.readValueAsTree();
            return new ESDateValue(DateTime.parse(node.textValue(), ESDateValue.dateFormatter));
        }
    }
}
