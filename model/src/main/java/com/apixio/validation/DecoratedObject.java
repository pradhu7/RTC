package com.apixio.validation;

public class DecoratedObject {

    private Object object;
    private String path;

    public DecoratedObject(Object object, String path) {
        this.object = object;
        this.path = path;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
