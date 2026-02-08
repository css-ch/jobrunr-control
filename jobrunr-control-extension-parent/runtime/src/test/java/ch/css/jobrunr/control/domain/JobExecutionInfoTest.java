package ch.css.jobrunr.control.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JobExecutionInfo")
class JobExecutionInfoTest {

    @Test
    @DisplayName("should create with all fields")
    void shouldCreateWithAllFields() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String jobName = "Test Job";
        String jobType = "TestJob";
        JobStatus status = JobStatus.SUCCEEDED;
        Instant startedAt = Instant.now().minusSeconds(60);
        Instant finishedAt = Instant.now();
        BatchProgress batchProgress = new BatchProgress(100, 100, 0);
        Map<String, Object> parameters = Map.of("param1", "value1");
        Map<String, Object> metadata = Map.of("key1", "value1");

        // Act
        JobExecutionInfo executionInfo = new JobExecutionInfo(
                jobId, jobName, jobType, status, startedAt, finishedAt,
                batchProgress, parameters, metadata
        );

        // Assert
        assertThat(executionInfo.jobId()).isEqualTo(jobId);
        assertThat(executionInfo.jobName()).isEqualTo(jobName);
        assertThat(executionInfo.jobType()).isEqualTo(jobType);
        assertThat(executionInfo.status()).isEqualTo(status);
        assertThat(executionInfo.startedAt()).isEqualTo(startedAt);
        assertThat(executionInfo.getFinishedAt()).isPresent().contains(finishedAt);
        assertThat(executionInfo.getBatchProgress()).isPresent().contains(batchProgress);
        assertThat(executionInfo.parameters()).isEqualTo(parameters);
        assertThat(executionInfo.metadata()).isEqualTo(metadata);
    }

    @Test
    @DisplayName("should handle null batch progress")
    void shouldHandleNullBatchProgress() {
        // Arrange & Act
        JobExecutionInfo executionInfo = new JobExecutionInfo(
                UUID.randomUUID(), "Test Job", "TestJob", JobStatus.PROCESSING,
                Instant.now(), null, null, Map.of(), Map.of()
        );

        // Assert
        assertThat(executionInfo.getBatchProgress()).isEmpty();
        assertThat(executionInfo.getFinishedAt()).isEmpty();
    }

    @Test
    @DisplayName("should throw when required fields are null")
    void shouldThrowWhenRequiredFieldsAreNull() {
        // Job ID null
        assertThatThrownBy(() -> new JobExecutionInfo(
                null, "Test", "TestJob", JobStatus.SUCCEEDED,
                Instant.now(), null, null, Map.of(), Map.of()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Job ID");

        // Job Name null
        assertThatThrownBy(() -> new JobExecutionInfo(
                UUID.randomUUID(), null, "TestJob", JobStatus.SUCCEEDED,
                Instant.now(), null, null, Map.of(), Map.of()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Job Name");

        // Job Type null
        assertThatThrownBy(() -> new JobExecutionInfo(
                UUID.randomUUID(), "Test", null, JobStatus.SUCCEEDED,
                Instant.now(), null, null, Map.of(), Map.of()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Job Type");

        // Status null
        assertThatThrownBy(() -> new JobExecutionInfo(
                UUID.randomUUID(), "Test", "TestJob", null,
                Instant.now(), null, null, Map.of(), Map.of()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Status");
    }

    @Test
    @DisplayName("should handle null parameters and metadata")
    void shouldHandleNullParametersAndMetadata() {
        // Act
        JobExecutionInfo executionInfo = new JobExecutionInfo(
                UUID.randomUUID(), "Test Job", "TestJob", JobStatus.SUCCEEDED,
                Instant.now(), null, null, null, null
        );

        // Assert
        assertThat(executionInfo.parameters()).isNotNull().isEmpty();
        assertThat(executionInfo.metadata()).isNotNull().isEmpty();
    }
}
