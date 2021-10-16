package com.priceindicator.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.priceindicator.domain.Price;
import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.PricePayload;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

@Slf4j
class PriceRepositoryTest {

    private PriceRepository repository;
    private LastPriceRepository lastPriceRepository;
    private final int coresNumber = Runtime.getRuntime().availableProcessors();

    @BeforeEach
    void setUp() {
        lastPriceRepository = mock(LastPriceRepository.class);
        repository = new PriceRepository(lastPriceRepository);
    }

    @Test
    void addPrices() throws InterruptedException {
        //arrange
        int idsCount = 100;
        int pricesCount = 1000;
        List<Price> prices = new ArrayList<>();
        Map<InstrumentId, Integer> instrumentsCount = new HashMap<>();
        List<InstrumentId> ids = new ArrayList<>();
        IntStream.range(0, idsCount).forEach(number -> ids.add(InstrumentId.of(UUID.randomUUID().toString())));
        Random random = new Random();

        //act
        IntStream.range(0, pricesCount).forEach(number -> {
            Price price = Price.builder()
                .id(ids.get(random.nextInt(idsCount)))
                .asOf(LocalDateTime.now())
                .payload(PricePayload.of("payload " + number + UUID.randomUUID()))
                .build();
            prices.add(price);
            instrumentsCount.putIfAbsent(price.getId(), 0);
            instrumentsCount.put(price.getId(), instrumentsCount.get(price.getId()) + 1);
        });

        ExecutorService executorService = Executors.newFixedThreadPool(coresNumber);
        for (int j = 0; j < 5; j++) {
            executorService.execute(() -> repository.addPrices(prices));
        }

        executorService.shutdown();
        executorService.awaitTermination(6, TimeUnit.SECONDS);

        //assert
        Map<InstrumentId, SortedSet<Price>> repoPricesMap = getRepoPricesMap(repository);

        assertEquals(idsCount, repoPricesMap.size());
        assertEquals(pricesCount, repoPricesMap.values().stream().mapToInt(Set::size).sum());
        repoPricesMap.forEach((instrumentId, pricesSet) -> assertEquals(pricesSet.size(), instrumentsCount.get(instrumentId)));
    }

    @Test
    void addPrices_withPrices_shouldAddCorrectPriceToLastPriceRepo() {
        //arrange
        InstrumentId instrumentId = InstrumentId.of(UUID.randomUUID().toString());
        String nowPricePayload = "Now price";
        Price yesterdayPrice = Price.builder()
            .id(instrumentId)
            .asOf(LocalDateTime.now().minusDays(1))
            .payload(PricePayload.of("Yesterday price"))
            .build();
        Price hourAgoPrice = Price.builder()
            .id(instrumentId)
            .asOf(LocalDateTime.now().minusHours(1))
            .payload(PricePayload.of("Hour ago price"))
            .build();
        Price nowPrice = Price.builder()
            .id(instrumentId)
            .asOf(LocalDateTime.now())
            .payload(PricePayload.of(nowPricePayload))
            .build();

        repository.addPrices(List.of(yesterdayPrice, nowPrice, hourAgoPrice));
        ArgumentCaptor<Map<InstrumentId, Price>> argumentCaptor = ArgumentCaptor.forClass(Map.class);

        //act
        verify(lastPriceRepository).addPrices(argumentCaptor.capture());

        //assert
        assertEquals(nowPrice, argumentCaptor.getValue().get(instrumentId));
    }

    @Test
    void getLastPriceById_shouldReturnPriceFromLastPriceRepo() {
        //arrange
        InstrumentId instrumentId = InstrumentId.of(UUID.randomUUID().toString());

        //act
        repository.getLastPriceById(instrumentId);

        //assert
        verify(lastPriceRepository).getPriceById(instrumentId);
    }

    //yes, I know it is hacky, but I believe it is better than return values only for tests
    private Map<InstrumentId, SortedSet<Price>> getRepoPricesMap(PriceRepository repository) {
        return (Map<InstrumentId, SortedSet<Price>>) ReflectionTestUtils.getField(repository, "prices");
    }
}
