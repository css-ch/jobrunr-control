package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import ch.css.jobrunr.control.testutils.JobDefinitionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateScheduledJobUseCase")
class UpdateScheduledJobUseCaseTest {

    @Mock
    private JobDefinitionDiscoveryService discoveryService;

    @Mock
    private JobParameterValidator validator;

    @Mock
    private ParameterStorageHelper storageHelper;

    @Mock
    private AuditLoggerHelper auditLogger;

    @InjectMocks
    private UpdateScheduledJobUseCase useCase;

    private JobDefinition testJobDefinition;

    @BeforeEach
    void setUp() {
        testJobDefinition = new JobDefinitionBuilder()
                .withJobType("TestJob")
                .withDisplayName("Test Job")
                .build();
    }

    @Test
    @DisplayName("should update scheduled job successfully")
    void shouldUpdateScheduledJobSuccessfully() {
        UUID jobId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "Updated Job";
        Map<String, String> parameters = Map.of("param1", "newValue");
        Map<String, Object> convertedParams = Map.of("param1", "newValue");
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        when(discoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);

        UUID result = useCase.execute(jobId, jobType, jobName, parameters, scheduledAt, false);

        assertThat(result).isEqualTo(jobId);
        verify(storageHelper).updateJobWithParameters(
                eq(jobId), eq(testJobDefinition), eq(jobType), eq(jobName),
                eq(convertedParams), eq(false), eq(scheduledAt), isNull());
        verify(auditLogger).logJobUpdated(jobName, jobId, convertedParams);
    }

    @Test
    @DisplayName("should throw exception when job definition not found")
    void shouldThrowExceptionWhenJobDefinitionNotFound() {
        UUID jobId = UUID.randomUUID();
        String invalidJobType = "NonExistentJob";

        when(discoveryService.requireJobByType(invalidJobType))
                .thenThrow(new JobNotFoundException("Job type '" + invalidJobType + "' not found"));

        assertThatThrownBy(() -> useCase.execute(jobId, invalidJobType, "Job", Map.of(), Instant.now(), false))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job type")
                .hasMessageContaining(invalidJobType);

        verifyNoInteractions(validator, storageHelper, auditLogger);
    }

}
