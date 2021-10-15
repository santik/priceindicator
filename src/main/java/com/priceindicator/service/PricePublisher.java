package com.priceindicator.service;

import com.priceindicator.domain.BatchRunId;
import com.priceindicator.domain.BatchRunStatus;
import com.priceindicator.domain.PriceBatch;

public interface PricePublisher {
    BatchRunStatus announceBatchRun();
    BatchRunStatus publishBatch(PriceBatch batch);
    BatchRunStatus finishBatchRun(BatchRunId batchRunId);
    BatchRunStatus cancelBatchRun(BatchRunId batchRunId);
}
