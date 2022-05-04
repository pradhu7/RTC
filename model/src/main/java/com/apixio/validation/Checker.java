package com.apixio.validation;

import java.util.List;

public interface Checker<T> {
    public List<Message> check(T object);
}
