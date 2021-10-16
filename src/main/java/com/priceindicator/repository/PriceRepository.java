package com.priceindicator.repository;

import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.Price;
import com.priceindicator.domain.PricePayload;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PriceRepository {

    private final LastPriceRepository lastPriceRepository;

    private final ConcurrentMap<InstrumentId, ConcurrentSkipListSet<Price>> prices = new ConcurrentHashMap<>();

    public void addPrices(List<Price> incomingPrices) {
        incomingPrices.forEach(price -> {
            prices.putIfAbsent(price.getId(), new ConcurrentSkipListSet<>(Comparator.comparing(Price::getAsOf)));
            prices.get(price.getId()).add(price);
        });

        releasePrices();
    }

    public Optional<PricePayload> getLastPriceById(InstrumentId instrumentId) {
        return lastPriceRepository.getPriceById(instrumentId);
    }

    private void releasePrices() {
        Map<InstrumentId, Price> collect = prices.entrySet().stream().collect(Collectors.toMap(
            Entry::getKey,
            e -> e.getValue().last()
        ));

        lastPriceRepository.addPrices(collect);
    }
}

