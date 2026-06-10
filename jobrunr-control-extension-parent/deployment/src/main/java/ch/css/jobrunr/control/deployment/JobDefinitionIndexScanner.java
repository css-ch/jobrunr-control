package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.deployment.scanner.*;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDetailPage;
import ch.css.jobrunr.control.domain.JobRecapParameter;
import ch.css.jobrunr.control.domain.JobSettings;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobTypeLabel;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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

    private static final Pattern JOB_TYPE_ALLOWED_CHARS = Pattern.compile("[A-Za-z0-9_-]+");

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
        RecapParameterExtractor recapParameterExtractor = new RecapParameterExtractor(index);
        JobDetailPageExtractor jobDetailPageExtractor = new JobDetailPageExtractor();

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

            // Extract job detail page
            JobDetailPage jobDetailPage = jobDetailPageExtractor.extractJobDetailPage(runMethod);
            String recapParameterClass = jobDetailPage != null ? jobDetailPage.recapParameterClass() : null;
            List<JobRecapParameter> analyzedRecapParameters = recapParameterClass != null && !recapParameterClass.isBlank()
                    ? recapParameterExtractor.analyzeRecapParameters(recapParameterClass)
                    : List.of();

            boolean isBatchJob = settingsExtractor.getBatchJobFlag(runMethod);
            // jobType defaults to the handler simple name, which stays aligned with
            // JobDetails.getClassName() so JobRunrSchedulerAdapter.mapToScheduledJobInfo and
            // ConfigurableJobSearchAdapter can resolve definitions and jobs by that name. The
            // optional @ConfigurableJob(jobType = "...") override exists as an escape hatch for
            // handlers whose simple name exceeds the 37-character label budget — see
            // JobTypeLabel. The user-facing display name lives separately in JobSettings.name().
            String jobTypeOverride = settingsExtractor.getJobTypeOverride(runMethod);
            String jobType = jobTypeOverride != null && !jobTypeOverride.isBlank()
                    ? jobTypeOverride
                    : classInfo.simpleName();

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
                    analyzedParams.externalParametersClassName(),
                    analyzedRecapParameters,
                    jobDetailPage
            ));
        }

        validateJobTypeFormat(jobDefinitions);
        validateJobTypesAreUnique(jobDefinitions);

        return jobDefinitions;
    }

    /**
     * Fails the build when any {@code jobType} exceeds the JobRunr label budget or contains
     * characters that would be unsafe inside the {@code jobtype:} label (used in search, URLs,
     * dashboard filters). The error message points developers to the
     * {@code @ConfigurableJob(jobType = "...")} escape hatch so long handler class names can be
     * kept for readability while still producing a short, safe label value.
     */
    private static void validateJobTypeFormat(Set<JobDefinition> jobDefinitions) {
        List<String> violations = jobDefinitions.stream()
                .map(JobDefinitionIndexScanner::describeJobTypeViolation)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();

        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid @ConfigurableJob jobType detected. The jobType is stored as the '"
                            + JobTypeLabel.PREFIX + "<jobType>' label on every scheduled job, so it must be at most "
                            + JobTypeLabel.MAX_JOBTYPE_LENGTH + " characters and only contain [A-Za-z0-9_-]. "
                            + "Rename the handler or set @ConfigurableJob(jobType = \"...\") to a shorter, safe identifier. "
                            + "Violations: " + String.join("; ", violations));
        }
    }

    private static String describeJobTypeViolation(JobDefinition jobDefinition) {
        String jobType = jobDefinition.jobType();
        String handler = jobDefinition.handlerClassName();
        if (jobType == null || jobType.isBlank()) {
            return handler + " → jobType is empty";
        }
        if (jobType.length() > JobTypeLabel.MAX_JOBTYPE_LENGTH) {
            return handler + " → jobType '" + jobType + "' is " + jobType.length()
                    + " characters (max " + JobTypeLabel.MAX_JOBTYPE_LENGTH + ")";
        }
        if (!JOB_TYPE_ALLOWED_CHARS.matcher(jobType).matches()) {
            return handler + " → jobType '" + jobType + "' contains characters outside [A-Za-z0-9_-]";
        }
        return null;
    }

    /**
     * Fails the build when two handlers produce the same effective {@code jobType}. The jobType
     * is used as the lookup key in {@code JobDefinitionDiscoveryAdapter} and as the
     * {@code jobtype:} label on every scheduled job. A collision would make runtime lookups
     * non-deterministic and mix unrelated jobs in the UI search history. Collisions can stem
     * from handler simple-name duplication or from two handlers configuring the same
     * {@code @ConfigurableJob(jobType = "...")} override.
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
                    "Duplicate @ConfigurableJob jobType detected. Each handler must produce a unique jobType "
                            + "because it is used as the JobDefinition.jobType lookup key and as the '"
                            + JobTypeLabel.PREFIX + "' label on scheduled jobs. Rename the handler(s) or use "
                            + "@ConfigurableJob(jobType = \"...\") to disambiguate. "
                            + "Conflicting handlers: " + String.join("; ", collisions));
        }
    }
}

