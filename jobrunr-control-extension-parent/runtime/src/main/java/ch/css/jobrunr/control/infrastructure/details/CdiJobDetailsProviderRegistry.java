package ch.css.jobrunr.control.infrastructure.details;

import ch.css.jobrunr.control.domain.details.JobDetailsProviderRegistry;
import ch.css.jobrunr.control.domain.details.JobMessageProvider;
import ch.css.jobrunr.control.domain.details.JobRecapProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class CdiJobDetailsProviderRegistry implements JobDetailsProviderRegistry {

    private static final Logger LOG = Logger.getLogger(CdiJobDetailsProviderRegistry.class);

    private final Map<String, JobMessageProvider> messageProviders;
    private final Map<String, JobRecapProvider> recapProviders;
    private final DefaultJobDetailsProvider defaultJobDetailsProvider;

    @Inject
    public CdiJobDetailsProviderRegistry(Instance<JobMessageProvider> messageProviders,
                                         Instance<JobRecapProvider> recapProviders, DefaultJobDetailsProvider defaultJobDetailsProvider) {
        this.messageProviders = messageProviders.stream()
                .collect(Collectors.toMap(JobMessageProvider::providerKey, provider -> provider, (existing, replacement) -> {
                    LOG.warnf("Duplicate JobMessageProvider key '%s' detected. Keeping existing provider %s and ignoring %s",
                            existing.providerKey(), existing.getClass().getName(), replacement.getClass().getName());
                    return existing;
                }));
        this.recapProviders = recapProviders.stream()
                .collect(Collectors.toMap(JobRecapProvider::providerKey, provider -> provider, (existing, replacement) -> {
                    LOG.warnf("Duplicate JobRecapProvider key '%s' detected. Keeping existing provider %s and ignoring %s",
                            existing.providerKey(), existing.getClass().getName(), replacement.getClass().getName());
                    return existing;
                }));
        this.defaultJobDetailsProvider = defaultJobDetailsProvider;
    }

    @Override
    public JobMessageProvider getMessageProvider(String providerKey) {
        if(providerKey == null || providerKey.isBlank()) {
            return defaultJobDetailsProvider;
        }
        JobMessageProvider provider = messageProviders.get(providerKey);
        if (provider == null) {
            LOG.warnf("Configured JobMessageProvider with key '%s' was not found. Falling back to default message lookup.", providerKey);
            return defaultJobDetailsProvider;
        }
        return provider;
    }

    @Override
    public JobRecapProvider getRecapProvider(String providerKey) {
        if(providerKey == null || providerKey.isBlank()) {
            return defaultJobDetailsProvider;
        }
        JobRecapProvider provider = recapProviders.get(providerKey);
        if (provider == null) {
            LOG.warnf("Configured JobRecapProvider with key '%s' was not found. Falling back to default recap lookup.", providerKey);
            return defaultJobDetailsProvider;
        }
        return provider;
    }
}

