package com.apixio.nassembly.valid;

import com.apixio.model.nassembly.Aggregator;
import com.apixio.model.nassembly.Combiner;

import java.util.HashMap;
import java.util.Map;

public class CombineTest implements Combiner {
    @Override
    public String getDataTypeName()
    {
        return "impl2";
    }


    @Override
    public Map<String, Aggregator> fromDataTypeToAggregator() {
        return new HashMap<String, Aggregator>() {{
            put("impl1", null);
        }};
    }

    @Override
    public Iterable combine(CombinerInput input) {
        return null;
    }
}
