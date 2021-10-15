package com.priceindicator.repository;

import static java.util.Objects.isNull;

import com.priceindicator.domain.Price;
import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.PricePayload;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.springframework.stereotype.Repository;

@Repository
public class PriceRepository {

    private final ConcurrentMap<InstrumentId, ConcurrentSkipListSet<Price>> prices = new ConcurrentHashMap<>();

    public void addPrices(List<Price> incomingPrices) {
        incomingPrices.forEach(price -> {
            prices.putIfAbsent(price.getId(), new ConcurrentSkipListSet<>(Comparator.comparing(Price::getAsOf)));
            prices.get(price.getId()).add(price);
        });
    }

    public Optional<PricePayload> getPriceById(InstrumentId instrumentId) {
        SortedSet<Price> sortedPrices = prices.get(instrumentId);
        if (isNull(sortedPrices) || sortedPrices.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(sortedPrices.last().getPayload());
    }
}

