package com.priceindicator.repository;

import static java.util.Objects.isNull;

import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.Price;
import com.priceindicator.domain.PricePayload;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class LastPriceRepository {

    private final Map<InstrumentId, Price> prices = new ConcurrentHashMap<>();

    public void addPrices(Map<InstrumentId, Price> newPrices) {
        prices.putAll(newPrices);
    }

    public Optional<PricePayload> getPriceById(InstrumentId instrumentId) {
        if (isNull(instrumentId) || !prices.containsKey(instrumentId)) {
            return Optional.empty();
        }
        return Optional.of(prices.get(instrumentId).getPayload());
    }
}

