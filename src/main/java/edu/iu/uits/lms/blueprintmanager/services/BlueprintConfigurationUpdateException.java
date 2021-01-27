package edu.iu.uits.lms.blueprintmanager.services;

import lombok.Getter;

@Getter
public class BlueprintConfigurationUpdateException extends RuntimeException {

    private final String failureMessage;

    public BlueprintConfigurationUpdateException() {
        super();
        this.failureMessage = null;
    }

    public BlueprintConfigurationUpdateException(String failureMessage) {
        super();
        this.failureMessage = failureMessage;
    }
}
