package ch.css.jobrunr.control.infrastructure.details;

import ch.css.jobrunr.control.domain.details.JobMessageLevelCounters;
import ch.css.jobrunr.control.domain.details.JobMessageLevelSearch;
import ch.css.jobrunr.control.domain.details.JobMessageSortOrder;
import ch.css.jobrunr.control.domain.details.JobMessageStoragePort;
import ch.css.jobrunr.control.domain.details.JobMessagesPaged;
import ch.css.jobrunr.control.domain.details.JobRecapStoragePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DbBasedJobDetailsProvider")
class DbBasedJobDetailsProviderTest {

    @Mock
    private JobMessageStoragePort jobMessageStoragePort;

    @Mock
    private JobRecapStoragePort jobRecapStoragePort;

    @Test
    @DisplayName("delegates recap and message lookups to storage ports")
    void delegatesToStoragePorts() {
        DbBasedJobDetailsProvider provider = new DbBasedJobDetailsProvider(jobMessageStoragePort, jobRecapStoragePort);
        UUID batchJobId = UUID.randomUUID();
        JobMessagesPaged pagedMessages = new JobMessagesPaged(List.of(), 0, 0, 25);
        JobMessageLevelCounters counters = new JobMessageLevelCounters(1, 2, 3, 4);
        Map<String, Long> recap = Map.of("processed", 7L);

        when(jobMessageStoragePort.searchMessages(
                batchJobId,
                JobMessageLevelSearch.ALL,
                "needle",
                JobMessageSortOrder.NEWEST_FIRST,
                2,
                25
        )).thenReturn(pagedMessages);
        when(jobMessageStoragePort.determineMessageLevelCounters(batchJobId)).thenReturn(counters);
        when(jobRecapStoragePort.readRecap(batchJobId)).thenReturn(recap);

        assertThat(provider.providerKey()).isEqualTo(DbBasedJobDetailsProvider.PROVIDER_KEY);
        assertThat(provider.searchJobMessages(
                batchJobId,
                JobMessageLevelSearch.ALL,
                "needle",
                JobMessageSortOrder.NEWEST_FIRST,
                2,
                25
        )).isSameAs(pagedMessages);
        assertThat(provider.determineJobMessageCounter(batchJobId)).isSameAs(counters);
        assertThat(provider.determineRecap(batchJobId)).isSameAs(recap);

        verify(jobMessageStoragePort).searchMessages(
                batchJobId,
                JobMessageLevelSearch.ALL,
                "needle",
                JobMessageSortOrder.NEWEST_FIRST,
                2,
                25
        );
        verify(jobMessageStoragePort).determineMessageLevelCounters(batchJobId);
        verify(jobRecapStoragePort).readRecap(batchJobId);
    }
}
