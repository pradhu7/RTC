package com.apixio.model.chart.elastic;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ESIntegerValue extends ESValue<Integer> {

    public ESIntegerValue(Integer value) {
        super(value, false);
    }

    public static Map<String, Object> getElasticTypeMapping() {
        return ImmutableMap.of("type", "integer");
    }

    @Override
    public String getJsonValue() {
        return String.valueOf(value);
    }
}
