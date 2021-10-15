package com.priceindicator.service;

import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.PricePayload;
import java.util.Optional;

public interface PriceConsumer {
    Optional<PricePayload> getLastPriceById(InstrumentId instrumentId);
}
