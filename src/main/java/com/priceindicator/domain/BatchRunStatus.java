package com.priceindicator.domain;

import lombok.Data;

@Data
public class BatchRunStatus {

    public enum Status {
        ANNOUNCED,
        IN_PROGRESS,
        CANCELED,
        FINISHED,
        ERROR
    }

    private final BatchRunId batchRunId;
    private final Status status;
}
