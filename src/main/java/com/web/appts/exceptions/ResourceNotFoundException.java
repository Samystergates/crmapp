
package com.web.appts.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    String resourceName;
    String fieldName;
    long fieldVal;
    String fieldValS;

    public ResourceNotFoundException(String resourceName, String fieldName, long fieldVal) {
        super(String.format("%s not found with %s : %l", resourceName, fieldName, fieldVal));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldVal = fieldVal;
    }

    public ResourceNotFoundException(String resourceName, String feildName, String feildValue) {
        super(String.format("%s not found with %s : %s", resourceName, feildName, feildValue));
        this.resourceName = resourceName;
        this.fieldName = feildName;
        this.fieldValS = feildValue;
    }

    public String getResourceName() {
        return this.resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public long getFieldVal() {
        return this.fieldVal;
    }

    public void setFieldVal(long fieldVal) {
        this.fieldVal = fieldVal;
    }
}
