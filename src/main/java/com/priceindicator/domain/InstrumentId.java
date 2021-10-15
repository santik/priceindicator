package com.priceindicator.domain;

import lombok.Data;

@Data(staticConstructor="of")
public class InstrumentId {
    private final String instrumentId;
}
