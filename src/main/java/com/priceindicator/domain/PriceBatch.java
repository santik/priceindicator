package com.priceindicator.domain;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceBatch {
    private BatchRunId batchRunId;
    private List<Price> prices;
}
