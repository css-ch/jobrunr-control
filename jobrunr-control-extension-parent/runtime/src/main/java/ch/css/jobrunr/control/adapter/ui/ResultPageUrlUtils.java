package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class ResultPageUrlUtils {

    private static final Logger LOG = Logger.getLogger(ResultPageUrlUtils.class);

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final String stage;

    @Inject
    public ResultPageUrlUtils(JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
                              @ConfigProperty(name = "stage", defaultValue = "dev") String stage) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.stage = stage.toLowerCase();
    }

    public String getResultPageUrl(JobExecutionInfo job, String host, String port) {
        Optional<JobDefinition> jobDefinition = jobDefinitionDiscoveryService.findJobByType(job.getJobType());
        if(jobDefinition.isPresent()) {
            String resultPageUrlPattern = jobDefinition.get().jobSettings().resultPageUrl();
            if(resultPageUrlPattern == null || resultPageUrlPattern.isBlank()) {
                return null;
            }
            String startDateStr = job.getStartedAt() != null ? LocalDate.ofInstant(job.startedAt(), ZoneId.systemDefault()).toString() : "";
            String endDateStr = job.finishedAt() != null ? LocalDate.ofInstant(job.finishedAt(), ZoneId.systemDefault()).toString() : "";
            resultPageUrlPattern = resultPageUrlPattern.replaceAll("\\{jobId}", job.getJobId().toString());
            resultPageUrlPattern = resultPageUrlPattern.replaceAll("\\{stage}", stage);
            resultPageUrlPattern = resultPageUrlPattern.replaceAll("\\{startDate}", startDateStr);
            resultPageUrlPattern = resultPageUrlPattern.replaceAll("\\{endDate}", endDateStr);
            resultPageUrlPattern = resultPageUrlPattern.replaceAll("\\{host}", host);
            resultPageUrlPattern = resultPageUrlPattern.replaceAll("\\{port}", port);
            return resultPageUrlPattern;
        } else {
            LOG.warn("No job definition found for job type " + job.jobType());
        }
        return null;

    }
}
