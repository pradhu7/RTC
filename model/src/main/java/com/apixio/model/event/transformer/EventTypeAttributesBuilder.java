package com.apixio.model.event.transformer;

import com.apixio.model.Builder;
import com.apixio.model.event.AttributeType;
import com.apixio.model.event.AttributesType;

/**
 * EventAttributesTypeBuilder is a helper class that lets us quickly build attributes for
 * varous events fields the way we want it.
 *
 * Created by vvyas on 1/23/14.
 */
public class EventTypeAttributesBuilder implements Builder<AttributesType> {
    private AttributesType attrs = new AttributesType();

    public EventTypeAttributesBuilder add(String name, String value) {
        AttributeType attr = new AttributeType();
        attr.setName(name);
        attr.setValue(value);
        attrs.getAttribute().add(attr);
        return this;
    }

    @Override
    public AttributesType build() { return attrs; }
}
