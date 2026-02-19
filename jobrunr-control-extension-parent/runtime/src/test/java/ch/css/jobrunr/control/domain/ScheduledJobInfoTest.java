package ch.css.jobrunr.control.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledJobInfoTest {

    private static JobDefinition createTestJobDefinition(String jobType, boolean usesExternalParameters) {
        return new JobDefinition(
                jobType,
                false,
                "test.JobRequest",
                "test.JobHandler",
                List.of(),
                new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null),
                usesExternalParameters,
                usesExternalParameters ? "parameterSetId" : null
        );
    }

    @Test
    void shouldDetectExternalParameters() {
        UUID jobId = UUID.randomUUID();

        ScheduledJobInfo jobInfo = new ScheduledJobInfo(
                jobId,
                "Test Job",
                createTestJobDefinition("TestJobType", true),
                Instant.now(),
                Map.of(),
                false
        );

        assertTrue(jobInfo.hasExternalParameters());
        Optional<UUID> retrievedId = jobInfo.getParameterSetId();
        assertTrue(retrievedId.isPresent());
        assertEquals(jobId, retrievedId.get());
    }

    @Test
    void shouldNotDetectExternalParametersForInlineJob() {
        UUID jobId = UUID.randomUUID();

        ScheduledJobInfo jobInfo = new ScheduledJobInfo(
                jobId,
                "Test Job",
                createTestJobDefinition("TestJobType", false),
                Instant.now(),
                Map.of("normalParam", "value"),
                false
        );

        assertFalse(jobInfo.hasExternalParameters());
        Optional<UUID> retrievedId = jobInfo.getParameterSetId();
        assertFalse(retrievedId.isPresent());
    }

    @Test
    void shouldHandleEmptyParameters() {
        UUID jobId = UUID.randomUUID();

        ScheduledJobInfo jobInfo = new ScheduledJobInfo(
                jobId,
                "Test Job",
                createTestJobDefinition("TestJobType", false),
                Instant.now(),
                Map.of(),
                false
        );

        assertFalse(jobInfo.hasExternalParameters());
        assertFalse(jobInfo.getParameterSetId().isPresent());
    }
}
