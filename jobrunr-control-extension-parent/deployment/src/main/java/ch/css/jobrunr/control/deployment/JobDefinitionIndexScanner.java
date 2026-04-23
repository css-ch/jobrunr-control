package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.deployment.scanner.JobRequestAnalyzer;
import ch.css.jobrunr.control.deployment.scanner.JobSettingsExtractor;
import ch.css.jobrunr.control.deployment.scanner.ParameterExtractor;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSettings;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scans the Jandex index for JobRequestHandler implementations annotated with @ConfigurableJob
 * and extracts job definitions.
 * <p>
 * This class coordinates three specialized components:
 * <ul>
 *   <li>{@link JobRequestAnalyzer} - Analyzes JobRequest types and type hierarchies</li>
 *   <li>{@link ParameterExtractor} - Extracts job parameters from record components</li>
 *   <li>{@link JobSettingsExtractor} - Extracts job settings from annotations</li>
 * </ul>
 */
final class JobDefinitionIndexScanner {

    private static final Logger LOG = Logger.getLogger(JobDefinitionIndexScanner.class);

    private static final DotName JOB_REQUEST_HANDLER = DotName.createSimple(JobRequestHandler.class.getName());

    private JobDefinitionIndexScanner() {
        // Helper class - no instantiation
    }

    /**
     * Finds all job specifications by scanning the Jandex index for JobRequestHandler implementations.
     *
     * @param index the Jandex index to scan
     * @return a set of discovered job definitions
     */
    public static Set<JobDefinition> findJobSpecifications(IndexView index) {
        Set<JobDefinition> jobDefinitions = new java.util.HashSet<>();

        // Initialize helper components
        JobRequestAnalyzer requestAnalyzer = new JobRequestAnalyzer(index);
        ParameterExtractor parameterExtractor = new ParameterExtractor(index);
        JobSettingsExtractor settingsExtractor = new JobSettingsExtractor();

        LOG.debugf("Searching for JobRequestHandler implementations");

        // Find all classes that implement JobRequestHandler
        var implementations = index.getAllKnownImplementations(JOB_REQUEST_HANDLER);

        for (ClassInfo classInfo : implementations) {
            LOG.debugf("Inspecting implementation: %s", classInfo.name());

            // Find the run method with @ConfigurableJob annotation
            MethodInfo runMethod = settingsExtractor.findConfigurableJobMethod(classInfo);
            if (runMethod == null) {
                continue;
            }

            LOG.debugf("Found @ConfigurableJob run method on %s", classInfo.name());

            // Extract the JobRequest type parameter
            Type jobRequestType = requestAnalyzer.findJobRequestType(classInfo);
            if (jobRequestType == null) {
                continue;
            }

            // Get the JobRequest class info
            ClassInfo requestClassInfo = index.getClassByName(jobRequestType.name());
            if (requestClassInfo == null) {
                LOG.debugf("Could not find ClassInfo for JobRequest type: %s", jobRequestType.name());
                continue;
            }

            // Analyze parameters
            ParameterExtractor.AnalyzedParameters analyzedParams =
                    parameterExtractor.analyzeRecordParameters(requestClassInfo);

            // Extract job settings
            JobSettings jobSettings = settingsExtractor.extractJobSettings(runMethod);

            boolean isBatchJob = settingsExtractor.getBatchJobFlag(runMethod);
            // jobType must stay aligned with JobDetails.getClassName() (handler simple name),
            // because JobRunrSchedulerAdapter.mapToScheduledJobInfo and ConfigurableJobSearchAdapter
            // look up definitions and jobs by that name. The user-facing display name lives in
            // JobSettings.name() and is read separately by the UI/REST layer.
            String jobType = classInfo.simpleName();

            // Log discovered job
            LOG.infof("Discovered job: %s (batch=%s, externalParams=%s) with %s parameters",
                    jobType, isBatchJob, analyzedParams.usesExternalParameters(),
                    analyzedParams.parameters().size());

            // Create job definition
            jobDefinitions.add(new JobDefinition(
                    jobType,
                    isBatchJob,
                    requestClassInfo.name().toString(),
                    classInfo.name().toString(),
                    analyzedParams.parameters(),
                    analyzedParams.parameterSections(),
                    jobSettings,
                    analyzedParams.usesExternalParameters(),
                    analyzedParams.externalParametersClassName()
            ));
        }

        validateJobTypesAreUnique(jobDefinitions);

        return jobDefinitions;
    }

    /**
     * Fails the build when two handlers share the same simple class name. The jobType (handler
     * simple name) is used as the lookup key in {@code JobDefinitionDiscoveryAdapter} and as the
     * {@code jobtype:} label on every scheduled job. A collision would make runtime lookups
     * non-deterministic and mix unrelated jobs in the UI search history.
     */
    private static void validateJobTypesAreUnique(Set<JobDefinition> jobDefinitions) {
        Map<String, List<String>> handlersByJobType = jobDefinitions.stream()
                .collect(Collectors.groupingBy(
                        JobDefinition::jobType,
                        Collectors.mapping(JobDefinition::handlerClassName, Collectors.toList())));

        List<String> collisions = handlersByJobType.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " → " + entry.getValue().stream().sorted().toList())
                .sorted()
                .toList();

        if (!collisions.isEmpty()) {
            throw new IllegalStateException(
                    "Duplicate @ConfigurableJob handler simple names detected. Each handler must have a unique simple class name "
                            + "because it is used as the JobDefinition.jobType lookup key and as the 'jobtype:' label on scheduled jobs. "
                            + "Conflicting handlers: " + String.join("; ", collisions));
        }
    }
}

