package com.priceindicator.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.priceindicator.domain.BatchRunId;
import com.priceindicator.domain.BatchRunStatus;
import com.priceindicator.domain.BatchRunStatus.Status;
import com.priceindicator.domain.Price;
import com.priceindicator.domain.PriceBatch;
import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.PricePayload;
import com.priceindicator.repository.BatchRepository;
import com.priceindicator.repository.PriceRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PricePublishServiceTest {

    private BatchRepository batchRepository;
    private PriceRepository priceRepository;
    private PricePublishService service;

    @BeforeEach
    public void setUp() {
        batchRepository = mock(BatchRepository.class);
        priceRepository = mock(PriceRepository.class);
        service = new PricePublishService(batchRepository, priceRepository);
    }

    @Test
    void announceBatch_shouldReturnCorrectStatusWithBatchId() {
        //arrange
        ArgumentCaptor<BatchRunId> batchRunIdArgumentCaptor = ArgumentCaptor.forClass(BatchRunId.class);

        //act
        BatchRunStatus batchStatus = service.announceBatchRun();

        //assert
        verify(batchRepository).announceBatchRun(batchRunIdArgumentCaptor.capture());
        assertEquals(Status.ANNOUNCED, batchStatus.getStatus());
        assertEquals(batchStatus.getBatchRunId(), batchRunIdArgumentCaptor.getValue());
    }

    @Test
    void publishBatchPart_withValidPart_shouldReturnCorrectStatusAndAddPricesToRepo() {
        //arrange
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
        InstrumentId instrumentalId = InstrumentId.of(UUID.randomUUID().toString());
        PricePayload payload = PricePayload.of("payload");
        Price price = Price.builder().id(instrumentalId).asOf(LocalDateTime.now()).payload(payload).build();
        List<Price> prices = Collections.singletonList(price);
        PriceBatch batchPart = PriceBatch.builder().batchRunId(batchRunId).prices(prices).build();

        //act
        BatchRunStatus batchStatus = service.publishBatch(batchPart);

        //assert
        verify(batchRepository).addPrices(batchPart);
        assertEquals(Status.IN_PROGRESS, batchStatus.getStatus());
    }

    @Test
    void publishBatch_withTooBigNumber_shouldThrowException() {
        //arrange
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
        InstrumentId instrumentalId = InstrumentId.of(UUID.randomUUID().toString());
        PricePayload payload = PricePayload.of("payload");
        List<Price> prices = new ArrayList<>();
        IntStream.range(0, 1001).forEach(i -> {
            Price price = Price.builder().id(instrumentalId).asOf(LocalDateTime.now()).payload(payload).build();
            prices.add(price);
        });
        PriceBatch batchPart = PriceBatch.builder().batchRunId(batchRunId).prices(prices).build();

        //act && assert
        assertThrows(IllegalArgumentException.class, () -> service.publishBatch(batchPart));
    }

    @Test
    void publishBatch_withEmptyPrices_shouldThrowException() {
        //arrange
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
        List<Price> prices = new ArrayList<>();
        PriceBatch batchPart = PriceBatch.builder().batchRunId(batchRunId).prices(prices).build();

        //act && assert
        assertThrows(IllegalArgumentException.class, () -> service.publishBatch(batchPart));
    }

    @Test
    void finishBatch_shouldReleaseBatchAndAddPrices() {
        //arrange
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());
        List<Price> prices = Collections.emptyList();
        when(batchRepository.releaseBatchRun(batchRunId)).thenReturn(prices);

        //act
        BatchRunStatus batchStatus = service.finishBatchRun(batchRunId);

        //assert
        verify(priceRepository).addPrices(prices);
        assertEquals(Status.FINISHED, batchStatus.getStatus());
        assertEquals(batchRunId, batchStatus.getBatchRunId());
    }

    @Test
    void cancelBatch_shouldRemoveBatch() {
        //arrange
        BatchRunId batchRunId = BatchRunId.of(UUID.randomUUID());

        //act
        BatchRunStatus batchStatus = service.cancelBatchRun(batchRunId);

        //assert
        verify(batchRepository).removeBatchRun(batchRunId);
        assertEquals(Status.CANCELED, batchStatus.getStatus());
        assertEquals(batchRunId, batchStatus.getBatchRunId());
    }
}