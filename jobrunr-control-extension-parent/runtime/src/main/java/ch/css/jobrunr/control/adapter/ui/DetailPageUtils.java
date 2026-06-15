package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
@Unremovable
public class DetailPageUtils {

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public DetailPageUtils(JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    public boolean hasDetailPage(String jobType) {
        Optional<JobDefinition> jobDefinition = jobDefinitionDiscoveryService.findJobByType(jobType);
        return jobDefinition.isPresent() && jobDefinition.get().jobDetailPage() != null;
    }
}
