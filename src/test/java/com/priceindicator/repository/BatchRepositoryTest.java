package com.priceindicator.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.priceindicator.domain.BatchRunId;
import com.priceindicator.domain.Price;
import com.priceindicator.domain.PriceBatch;
import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.PricePayload;
import com.priceindicator.repository.exception.BatchNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Slf4j
class BatchRepositoryTest {

    private BatchRepository repository;
    private final int coresNumber = Runtime.getRuntime().availableProcessors();

    @BeforeEach
    void setUp() {
        repository = new BatchRepository();
    }

    @Test
    void announceBatch_shouldCreateAnEntry() throws InterruptedException {
        //arrange
        List<BatchRunId> batches = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(coresNumber);

        //act
        for (int j = 0; j < 10; j++) {
            BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
            batches.add(batchRunId);
            for (int k = 0; k < 10; k++) {
                executorService.execute(() -> repository.announceBatchRun(batchRunId));
            }
        }
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);

        //assert
        Map<BatchRunId, List<Price>> repoBatchesMap = getRepoBatchesMap();
        assertTrue(repoBatchesMap.keySet().containsAll(batches));
        repoBatchesMap.forEach((key, value) -> assertNotNull(value));
    }

    @Test
    void addPrices_shouldBeAbleToAddPricesConcurrently() throws InterruptedException {
        //arrange
        ExecutorService executorService = Executors.newFixedThreadPool(coresNumber);
        int batchesCount = 20;

        //act
        for (int i = 0; i < batchesCount; i ++) {
            BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
            repository.announceBatchRun(batchRunId);

            for (int j = 0; j < 20; j++) {
                List<Price> prices = new ArrayList<>();
                IntStream.range(0, 100).forEach(number -> {
                    Price price = Price.builder()
                        .id(InstrumentId.of(UUID.randomUUID().toString()))
                        .asOf(LocalDateTime.now())
                        .payload(PricePayload.of("payload " + number))
                        .build();
                    prices.add(price);
                });
                PriceBatch batch = PriceBatch.builder().batchRunId(batchRunId).prices(prices).build();

                executorService.execute(() -> repository.addPrices(batch));
            }
        }
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);

        //assert
        Map<BatchRunId, List<Price>> repoBatchesMap = getRepoBatchesMap();
        assertEquals(batchesCount, repoBatchesMap.size());
        repoBatchesMap.forEach((batchRunId, prices) -> assertEquals(2000, prices.size()));
    }

    @Test
    void addPrices_withNoBatch_shouldThrowException() {
        //arrange
        PriceBatch batch = PriceBatch.builder().batchRunId(BatchRunId.of(UUID.randomUUID())).prices(Collections.emptyList()).build();

        //act && assert
        assertThrows(BatchNotFoundException.class, () -> repository.addPrices(batch));
    }

    @Test
    void removeBatch_shouldRemoveAndAddWhenItIsAddedAgain() {
        //arrange
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
        repository.announceBatchRun(batchRunId);

        //act
        repository.removeBatchRun(batchRunId);

        //assert
        Map<BatchRunId, List<Price>> repoBatchesMap = getRepoBatchesMap();
        assertNull(repoBatchesMap.get(batchRunId));
    }

    @Test
    void releaseBatch_shouldReturnCorrectPrices() {
        //arrange
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
        repository.announceBatchRun(batchRunId);
        List<Price> prices = new ArrayList<>();
        int pricesCount = 100;
        IntStream.range(0, pricesCount).forEach(number -> {
            Price price = Price.builder()
                .id(InstrumentId.of(UUID.randomUUID().toString()))
                .asOf(LocalDateTime.now())
                .payload(PricePayload.of("payload " + number))
                .build();
            prices.add(price);
        });
        PriceBatch batch = PriceBatch.builder().batchRunId(batchRunId).prices(prices).build();
        repository.addPrices(batch);


        //act
        List<Price> priceList = repository.releaseBatchRun(batchRunId);

        //assert
        assertEquals(pricesCount, priceList.size());
    }

    @Test
    void releaseBatch_withNoBatch_shouldReturnEmptyList() {
        //arrange
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());

        //act && assert
        assertTrue(repository.releaseBatchRun(batchRunId).isEmpty());
    }

    //yes, I know it is hacky, but I believe it is better than return values only for tests
    private Map<BatchRunId, List<Price>> getRepoBatchesMap() {
        return (Map<BatchRunId, List<Price>>) ReflectionTestUtils.getField(repository, "batchRuns");
    }
}
