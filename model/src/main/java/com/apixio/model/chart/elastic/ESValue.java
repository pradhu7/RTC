package com.apixio.model.chart.elastic;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

@Data
@JsonSerialize(using = ESValueSerializer.class)
public abstract class ESValue<T> implements TypeWrapperES {

    protected final T value;
    protected final boolean isEncrypted;

    public ESValue(T value, boolean isEncrypted) {
        this.value = value;
        this.isEncrypted = isEncrypted;
    }

    public abstract String getJsonValue();

    public T getValue() {
        return value;
    }
}
