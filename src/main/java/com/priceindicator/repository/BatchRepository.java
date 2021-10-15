package com.priceindicator.repository;

import com.priceindicator.domain.BatchRunId;
import com.priceindicator.domain.Price;
import com.priceindicator.domain.PriceBatch;
import com.priceindicator.repository.exception.BatchNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class BatchRepository {

    private final ConcurrentMap<BatchRunId, CopyOnWriteArrayList<Price>> batchRuns = new ConcurrentHashMap<>();

    public void announceBatchRun(BatchRunId batchRunId) {
        batchRuns.putIfAbsent(batchRunId, new CopyOnWriteArrayList<>());
    }

    public void addPrices(PriceBatch batch) {
        if (!batchRuns.containsKey(batch.getBatchRunId())) {
            throw new BatchNotFoundException();
        }

        batchRuns.computeIfPresent(batch.getBatchRunId(),
            (batchRunId, prices) -> {
                prices.addAll(batch.getPrices());
                return prices;
            });
    }

    public void removeBatchRun(BatchRunId batchRunId) {
        batchRuns.computeIfPresent(batchRunId, (id, prices) -> null);
    }

    public List<Price> releaseBatchRun(BatchRunId batchRunId) {
        List<Price> prices = batchRuns.get(batchRunId);
        if (Objects.isNull(prices)) {
            return new ArrayList<>();
        }
        removeBatchRun(batchRunId);
        return new ArrayList<>(prices);
    }
}
