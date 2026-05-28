package ch.css.jobrunr.control.application.details;

import java.util.Map;
import java.util.UUID;

public interface JobRecapProvider {

    String providerKey();

    Map<String, Long> determineRecap(UUID jobId);
}

