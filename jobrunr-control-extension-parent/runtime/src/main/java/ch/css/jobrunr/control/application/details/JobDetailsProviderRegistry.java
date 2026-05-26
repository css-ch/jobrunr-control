package ch.css.jobrunr.control.application.details;

import java.util.Optional;

public interface JobDetailsProviderRegistry {

    Optional<JobMessageProvider> findMessageProvider(String providerKey);

    Optional<JobRecapProvider> findRecapProvider(String providerKey);
}

