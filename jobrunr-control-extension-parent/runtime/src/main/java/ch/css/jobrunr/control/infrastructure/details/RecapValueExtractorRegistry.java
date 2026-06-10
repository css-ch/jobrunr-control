package ch.css.jobrunr.control.infrastructure.details;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class RecapValueExtractorRegistry {

    private static final Logger LOG = Logger.getLogger(RecapValueExtractorRegistry.class);

    private final Map<String, RecapValueExtractor> extractorsByRecapClassName;

    @Inject
    public RecapValueExtractorRegistry(Instance<RecapValueExtractor> extractors) {
        Map<String, RecapValueExtractor> byRecapClassName = new HashMap<>();
        for (RecapValueExtractor extractor : extractors) {
            RecapValueExtractor previous = byRecapClassName.put(extractor.recapClassName(), extractor);
            if (previous != null) {
                LOG.warnf("Duplicate RecapValueExtractor for recap class '%s' found. Replacing '%s' with '%s'.",
                        extractor.recapClassName(), previous.getClass().getName(), extractor.getClass().getName());
            }
        }
        LOG.infof("Registered %d RecapValueExtractor bean(s).", byRecapClassName.size());
        this.extractorsByRecapClassName = Map.copyOf(byRecapClassName);
    }

    public Optional<RecapValueExtractor> findByRecapClassName(String recapClassName) {
        if (recapClassName == null || recapClassName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(extractorsByRecapClassName.get(recapClassName));
    }
}
