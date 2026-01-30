package ch.css.jobrunr.control.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledJobInfoTest {

    private static JobDefinition createTestJobDefinition(String jobType) {
        return new JobDefinition(
                jobType,
                false,
                "test.JobRequest",
                "test.JobHandler",
                List.of(),
                new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null),
                false,
                null
        );
    }

    @Test
    void shouldDetectExternalParameters() {
        UUID jobId = UUID.randomUUID();
        UUID paramSetId = UUID.randomUUID();
        Map<String, Object> params = Map.of("__parameterSetId", paramSetId.toString());

        ScheduledJobInfo jobInfo = new ScheduledJobInfo(
                jobId,
                "Test Job",
                createTestJobDefinition("TestJobType"),
                java.time.Instant.now(),
                params,
                false
        );

        assertTrue(jobInfo.hasExternalParameters());
        Optional<UUID> retrievedId = jobInfo.getParameterSetId();
        assertTrue(retrievedId.isPresent());
        assertEquals(paramSetId, retrievedId.get());
    }

    @Test
    void shouldNotDetectExternalParametersForInlineJob() {
        UUID jobId = UUID.randomUUID();
        Map<String, Object> params = Map.of("normalParam", "value");

        ScheduledJobInfo jobInfo = new ScheduledJobInfo(
                jobId,
                "Test Job",
                createTestJobDefinition("TestJobType"),
                java.time.Instant.now(),
                params,
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
                createTestJobDefinition("TestJobType"),
                java.time.Instant.now(),
                Map.of(),
                false
        );

        assertFalse(jobInfo.hasExternalParameters());
        assertFalse(jobInfo.getParameterSetId().isPresent());
    }
}
