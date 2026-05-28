package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.application.details.JobRecapProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.jobs.Job;
import org.jobrunr.storage.JobSearchRequestBuilder;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ComplexDemoRecapProvider implements JobRecapProvider {

    private final StorageProvider storageProvider;

    @Inject
    public ComplexDemoRecapProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @Override
    public String providerKey() {
        return "complex-demo-recap-provider";
    }

    @Override
    public Map<String, Long> determineRecap(UUID jobId) {
        Map<String, Long> counters = new HashMap<>();
        counters.put("policenSelektiert", 0L);
        counters.put("policenRelevant", 0L);
        counters.put("policenFailed", 0L);
        counters.put("policenSperre", 0L);
        counters.put("policenAnnulliert", 0L);
        counters.put("policenHerausgefilter", 0L);
        counters.put("druckauftraegeVerarbeitet", 0L);
        counters.put("druckauftraegeGedruckt", 0L);

        getChildJobs(jobId).forEach(job -> updateCounters(counters, job.getResult()));
        return new HashMap<>(counters);
    }

    private List<Job> getChildJobs(UUID jobId) {
        return storageProvider.getJobList(JobSearchRequestBuilder.aJobSearchRequest()
                .withParentId(jobId)
                .build(), AmountRequest.fromString("limit=1000000"));
    }

    private void updateCounters(Map<String, Long> counters, Object result) {
        if (!(result instanceof ComplexParameterDemoJobRecap(
                long policenSelektiert, long policenRelevant, long policenFailed, long policenSperre,
                long policenAnnulliert, long policenHerausgefilter, long druckauftraegeVerarbeitet,
                long druckauftraegeGedruckt
        ))) {
            return;
        }

        counters.computeIfPresent("policenSelektiert", (key, value) -> value + policenSelektiert);
        counters.computeIfPresent("policenRelevant", (key, value) -> value + policenRelevant);
        counters.computeIfPresent("policenFailed", (key, value) -> value + policenFailed);
        counters.computeIfPresent("policenSperre", (key, value) -> value + policenSperre);
        counters.computeIfPresent("policenAnnulliert", (key, value) -> value + policenAnnulliert);
        counters.computeIfPresent("policenHerausgefilter", (key, value) -> value + policenHerausgefilter);
        counters.computeIfPresent("druckauftraegeVerarbeitet", (key, value) -> value + druckauftraegeVerarbeitet);
        counters.computeIfPresent("druckauftraegeGedruckt", (key, value) -> value + druckauftraegeGedruckt);
    }
}

