package com.priceindicator.service;

import com.priceindicator.domain.InstrumentId;
import com.priceindicator.domain.PricePayload;
import com.priceindicator.repository.PriceRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceConsumeService implements PriceConsumer {

    private final PriceRepository priceRepository;

    @Override
    public Optional<PricePayload> getLastPriceById(InstrumentId instrumentId) {
        return priceRepository.getPriceById(instrumentId);
    }
}
