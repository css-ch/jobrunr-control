package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.deployment.scanner.JobRequestAnalyzer;
import ch.css.jobrunr.control.deployment.scanner.JobSettingsExtractor;
import ch.css.jobrunr.control.deployment.scanner.ParameterExtractor;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSettings;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.Set;

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
class JobDefinitionIndexScanner {

    private static final Logger log = Logger.getLogger(JobDefinitionIndexScanner.class);

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

        log.debugf("Searching for JobRequestHandler implementations");

        // Find all classes that implement JobRequestHandler
        var implementations = index.getAllKnownImplementations(JOB_REQUEST_HANDLER);

        for (ClassInfo classInfo : implementations) {
            log.debugf("Inspecting implementation: %s", classInfo.name());

            // Find the run method with @ConfigurableJob annotation
            MethodInfo runMethod = settingsExtractor.findConfigurableJobMethod(classInfo);
            if (runMethod == null) {
                continue;
            }

            log.debugf("Found @ConfigurableJob run method on %s", classInfo.name());

            // Extract the JobRequest type parameter
            Type jobRequestType = requestAnalyzer.findJobRequestType(classInfo);
            if (jobRequestType == null) {
                continue;
            }

            // Get the JobRequest class info
            ClassInfo requestClassInfo = index.getClassByName(jobRequestType.name());
            if (requestClassInfo == null) {
                log.debugf("Could not find ClassInfo for JobRequest type: %s", jobRequestType.name());
                continue;
            }

            // Extract job metadata
            String jobType = classInfo.simpleName();
            boolean isBatchJob = settingsExtractor.getBatchJobFlag(runMethod);

            // Analyze parameters
            ParameterExtractor.AnalyzedParameters analyzedParams =
                    parameterExtractor.analyzeRecordParameters(requestClassInfo);

            // Extract job settings
            JobSettings jobSettings = settingsExtractor.extractJobSettings(runMethod);

            // Log discovered job
            log.infof("Discovered job: %s (batch=%s, externalParams=%s) with %s parameters",
                    jobType, isBatchJob, analyzedParams.usesExternalParameters(),
                    analyzedParams.parameters().size());

            // Create job definition
            jobDefinitions.add(new JobDefinition(
                    jobType,
                    isBatchJob,
                    requestClassInfo.name().toString(),
                    classInfo.name().toString(),
                    analyzedParams.parameters(),
                    jobSettings,
                    analyzedParams.usesExternalParameters(),
                    analyzedParams.parameterSetFieldName()
            ));
        }

        return jobDefinitions;
    }
}

