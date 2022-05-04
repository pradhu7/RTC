package com.apixio.nassembly.model;

import com.apixio.model.nassembly.Accessor;

import java.util.Map;

public interface PatientLogicAccessor<T> extends Accessor<T> {

    Map<String, Object> getInstructions();
}
