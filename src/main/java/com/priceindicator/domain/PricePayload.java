package com.priceindicator.domain;

import lombok.Data;

@Data(staticConstructor="of")
public class PricePayload {
    private final String payload;
}
