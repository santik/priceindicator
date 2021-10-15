package com.priceindicator.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Price {
    private InstrumentId id;
    private LocalDateTime asOf;
    private PricePayload payload;
}
