package ch.css.jobrunr.control.domain.details;

public interface JobDetailsProviderRegistry {

    JobMessageProvider getMessageProvider(String providerKey);

    JobRecapProvider getRecapProvider(String providerKey);
}

