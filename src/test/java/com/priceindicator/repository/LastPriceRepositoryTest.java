package com.priceindicator.repository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.Price;
import com.priceindicator.domain.PricePayload;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LastPriceRepositoryTest {

    private LastPriceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LastPriceRepository();
    }

    @Test
    void addPrices_shouldReplacePrices() {
        //arrange
        InstrumentId targetInstrumentId = InstrumentId.of("instrumentId");
        Price price = Price.builder()
            .id(targetInstrumentId)
            .asOf(LocalDateTime.now())
            .payload(PricePayload.of("payload"))
            .build();
        Price price1 = Price.builder()
            .id(InstrumentId.of("anotherInstrumentId"))
            .asOf(LocalDateTime.now())
            .payload(PricePayload.of("payload"))
            .build();
        Price newPrice = Price.builder()
            .id(targetInstrumentId)
            .asOf(LocalDateTime.now())
            .payload(PricePayload.of("payload"))
            .build();
        Map<InstrumentId, Price> prices = new HashMap<>();
        prices.put(price.getId(), price);
        prices.put(price1.getId(), price1);
        repository.addPrices(prices);

        //act && assert
        Map<InstrumentId, Price> repoPricesMap = getRepoPricesMap();
        assertSame(price, repoPricesMap.get(targetInstrumentId));
        assertSame(price1, repoPricesMap.get(price1.getId()));
        repository.addPrices(Collections.singletonMap(targetInstrumentId, newPrice));
        assertSame(newPrice, repoPricesMap.get(targetInstrumentId));
    }

    @Test
    void getPriceById_withNullId_shouldReturnEmpty() {
        //arrange
        InstrumentId instrumentId = null;

        //act
        Optional<PricePayload> priceById = repository.getPriceById(instrumentId);

        //assert
        assertTrue(priceById.isEmpty());
    }

    @Test
    void getPriceById_withNotExistingId_shouldReturnEmpty() {
        //arrange
        InstrumentId instrumentId = InstrumentId.of("id");

        //act
        Optional<PricePayload> priceById = repository.getPriceById(instrumentId);

        //assert
        assertTrue(priceById.isEmpty());
    }

    @Test
    void getPriceById_withExistingId_shouldReturnEmpty() {
        //arrange
        InstrumentId instrumentId = InstrumentId.of("id");
        PricePayload payload = PricePayload.of("payload");
        Price newPrice = Price.builder()
            .id(instrumentId)
            .asOf(LocalDateTime.now())
            .payload(payload)
            .build();
        repository.addPrices(Collections.singletonMap(instrumentId, newPrice));

        //act
        Optional<PricePayload> priceById = repository.getPriceById(instrumentId);

        //assert
        assertSame(payload, priceById.get());
    }

    //yes, I know it is hacky, but I believe it is better than return values only for tests
    private Map<InstrumentId, Price> getRepoPricesMap() {
        return (Map<InstrumentId, Price>) ReflectionTestUtils.getField(repository, "prices");
    }
}