package com.nokia.filersync.properties;


public class PathListElementAttribute {

    private final PathListElement parent;
    private final String key;
    private Object value;

    public PathListElementAttribute(PathListElement parentEl, String myKey, Object myValue) {
        key = myKey;
        value = myValue;
        parent = parentEl;
    }

    public PathListElement getParent() {
        return parent;
    }

    /**
     * Returns the key.
     * @return String
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value.
     * @return Object
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns the value.
     */
    public void setValue(Object myValue) {
        value = myValue;
    }
}
