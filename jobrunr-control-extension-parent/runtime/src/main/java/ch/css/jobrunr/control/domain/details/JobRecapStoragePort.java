package ch.css.jobrunr.control.domain.details;

import java.util.Map;
import java.util.UUID;

public interface JobRecapStoragePort {

    void writeRecap(UUID batchJobId, UUID childJobId, Map<String,Long> recap);

    Map<String,Long> readRecap(UUID batchJobId);
}
