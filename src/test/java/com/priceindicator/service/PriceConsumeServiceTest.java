package com.priceindicator.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.PricePayload;
import com.priceindicator.repository.PriceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PriceConsumeServiceTest {

    @Test
    void getLastPriceById_shouldGetItFromRepo() {
        //arrange
        PriceRepository repository = mock(PriceRepository.class);
        PriceConsumeService service = new PriceConsumeService(repository);
        InstrumentId id = InstrumentId.of(UUID.randomUUID().toString());
        PricePayload payload = PricePayload.of("payload");
        when(repository.getLastPriceById(id)).thenReturn(Optional.of(payload));

        //act
        Optional<PricePayload> lastPriceById = service.getLastPriceById(id);

        //assert
        assertEquals(payload, lastPriceById.get());
    }
}