package ch.css.jobrunr.control.infrastructure.details;

import ch.css.jobrunr.control.application.details.JobDetailsProviderRegistry;
import ch.css.jobrunr.control.application.details.JobMessageProvider;
import ch.css.jobrunr.control.application.details.JobRecapProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class CdiJobDetailsProviderRegistry implements JobDetailsProviderRegistry {

    private static final Logger LOG = Logger.getLogger(CdiJobDetailsProviderRegistry.class);

    private final Map<String, JobMessageProvider> messageProviders;
    private final Map<String, JobRecapProvider> recapProviders;

    @Inject
    public CdiJobDetailsProviderRegistry(Instance<JobMessageProvider> messageProviders,
                                         Instance<JobRecapProvider> recapProviders) {
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
    }

    @Override
    public Optional<JobMessageProvider> findMessageProvider(String providerKey) {
        return Optional.ofNullable(messageProviders.get(providerKey));
    }

    @Override
    public Optional<JobRecapProvider> findRecapProvider(String providerKey) {
        return Optional.ofNullable(recapProviders.get(providerKey));
    }
}

