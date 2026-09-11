package com.dnd.campaignmanager.common;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super(resourceType + " " + id + " not found");
    }
}
