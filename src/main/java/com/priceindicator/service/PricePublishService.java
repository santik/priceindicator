package com.priceindicator.service;

import com.priceindicator.domain.BatchRunId;
import com.priceindicator.domain.BatchRunStatus;
import com.priceindicator.domain.BatchRunStatus.Status;
import com.priceindicator.domain.Price;
import com.priceindicator.domain.PriceBatch;
import com.priceindicator.repository.BatchRepository;
import com.priceindicator.repository.PriceRepository;
import com.priceindicator.repository.exception.BatchNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PricePublishService implements PricePublisher {

    public static final int MAX_BATCH_SIZE = 1000;
    public static final int MIN_BATCH_SIZE = 1;
    private final BatchRepository batchRepository;
    private final PriceRepository priceRepository;

    @Override
    public BatchRunStatus announceBatchRun() {
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
        BatchRunStatus batchRunStatus = new BatchRunStatus(batchRunId, Status.ANNOUNCED);
        batchRepository.announceBatchRun(batchRunId);
        return batchRunStatus;
    }

    @Override
    public BatchRunStatus publishBatch(PriceBatch batch) {
        //we could reject batches which are not 1000 items,
        //but I think it is very unlikely in a real world that all batches will be exactly 1000
        //so maximum was put to 1000 and minimum 1
        if (batch.getPrices().size() < MIN_BATCH_SIZE || batch.getPrices().size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Incorrect batch size");
        }
        Status status = Status.IN_PROGRESS;
        try {
            batchRepository.addPrices(batch);
        } catch (BatchNotFoundException e) {
            status = Status.ERROR;
        }
        return new BatchRunStatus(batch.getBatchRunId(), status);
    }

    @Override
    public BatchRunStatus finishBatchRun(BatchRunId batchRunId) {
        List<Price> prices = batchRepository.releaseBatchRun(batchRunId);
        priceRepository.addPrices(prices);
        return new BatchRunStatus(batchRunId, Status.FINISHED);
    }

    @Override
    public BatchRunStatus cancelBatchRun(BatchRunId batchRunId) {
        batchRepository.removeBatchRun(batchRunId);
        return new BatchRunStatus(batchRunId, Status.CANCELED);
    }
}
