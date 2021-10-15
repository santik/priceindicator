package com.priceindicator.domain;

import java.util.UUID;
import lombok.Data;

@Data(staticConstructor="of")
public class BatchRunId {
    private final UUID batchRunId;
}
