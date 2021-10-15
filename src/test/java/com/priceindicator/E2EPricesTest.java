package com.priceindicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.priceindicator.domain.BatchRunId;
import com.priceindicator.domain.BatchRunStatus;
import com.priceindicator.domain.Price;
import com.priceindicator.domain.PriceBatch;
import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.PricePayload;
import com.priceindicator.service.PriceConsumer;
import com.priceindicator.service.PricePublisher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class E2EPricesTest {

    @Autowired
    private PriceConsumer priceConsumer;

    @Autowired
    private PricePublisher pricePublisher;

    @Test
    void testPositiveFlow() throws InterruptedException {
        //announce
        BatchRunStatus batchStatus = pricePublisher.announceBatchRun();

        //publish
        Price targetPrice = null;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int j = 0; j < 100; j++) {
            List<Price> prices = new ArrayList<>();
            IntStream.range(0, 100).forEach(number -> {
                Price price = Price.builder()
                    .id(InstrumentId.of(UUID.randomUUID().toString()))
                    .asOf(LocalDateTime.now().minusHours(1))
                    .payload(PricePayload.of("payload " + number))
                    .build();
                prices.add(price);
            });
            targetPrice = Price.builder()
                .id(prices.get(0).getId())
                .asOf(LocalDateTime.now())
                .payload(prices.get(0).getPayload())
                .build();
            prices.add(targetPrice);
            PriceBatch batch = PriceBatch.builder().batchRunId(batchStatus.getBatchRunId()).prices(prices).build();
            executorService.execute(() -> pricePublisher.publishBatch(batch));
        }
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);

        //finish
        pricePublisher.finishBatchRun(batchStatus.getBatchRunId());

        //fetch
        assertEquals(targetPrice.getPayload(), priceConsumer.getLastPriceById(targetPrice.getId()).get());
    }

    @Test
    void when_publishWithoutAnnounce_canNotFetch() {
        //publish
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
        Price price = Price.builder()
            .id(InstrumentId.of(UUID.randomUUID().toString()))
            .asOf(LocalDateTime.now().minusHours(1))
            .payload(PricePayload.of("payload"))
            .build();
        PriceBatch batch = PriceBatch.builder().batchRunId(batchRunId).prices(Collections.singletonList(price)).build();
        try {
            pricePublisher.publishBatch(batch);
        } catch (Exception e) {
            log.error("Exception happened", e);
        }

        //finish
        pricePublisher.finishBatchRun(batchRunId);

        //fetch
        assertTrue(priceConsumer.getLastPriceById(price.getId()).isEmpty());
    }

    @Test
    void when_publishWithoutFinish_canNotFetch() {
        //announce
        BatchRunStatus batchStatus = pricePublisher.announceBatchRun();

        //publish
        BatchRunId batchRunId = batchStatus.getBatchRunId();
        Price price = Price.builder()
            .id(InstrumentId.of(UUID.randomUUID().toString()))
            .asOf(LocalDateTime.now().minusHours(1))
            .payload(PricePayload.of("payload"))
            .build();
        PriceBatch batch = PriceBatch.builder().batchRunId(batchRunId).prices(Collections.singletonList(price)).build();
        pricePublisher.publishBatch(batch);

        //fetch
        assertTrue(priceConsumer.getLastPriceById(price.getId()).isEmpty());
    }

    @Test
    void when_publishAndCancel_canNotFetch() {
        //announce
        BatchRunStatus batchStatus = pricePublisher.announceBatchRun();

        //publish
        BatchRunId batchRunId = batchStatus.getBatchRunId();
        Price price = Price.builder()
            .id(InstrumentId.of(UUID.randomUUID().toString()))
            .asOf(LocalDateTime.now().minusHours(1))
            .payload(PricePayload.of("payload"))
            .build();
        PriceBatch batch = PriceBatch.builder().batchRunId(batchRunId).prices(Collections.singletonList(price)).build();
        pricePublisher.publishBatch(batch);

        //cancel
        pricePublisher.cancelBatchRun(batchRunId);

        //fetch
        assertTrue(priceConsumer.getLastPriceById(price.getId()).isEmpty());
    }

    @Test
    void when_publishAndFetchBeforeFinish_canNotFetch() {
        //announce
        BatchRunStatus batchStatus = pricePublisher.announceBatchRun();

        //publish
        BatchRunId batchRunId = batchStatus.getBatchRunId();
        Price price = Price.builder()
            .id(InstrumentId.of(UUID.randomUUID().toString()))
            .asOf(LocalDateTime.now().minusHours(1))
            .payload(PricePayload.of("payload"))
            .build();
        PriceBatch batch = PriceBatch.builder().batchRunId(batchRunId).prices(Collections.singletonList(price)).build();
        pricePublisher.publishBatch(batch);

        //fetch
        assertTrue(priceConsumer.getLastPriceById(price.getId()).isEmpty());

        //finish
        pricePublisher.finishBatchRun(batchRunId);

        assertTrue(priceConsumer.getLastPriceById(price.getId()).isPresent());
    }
}
