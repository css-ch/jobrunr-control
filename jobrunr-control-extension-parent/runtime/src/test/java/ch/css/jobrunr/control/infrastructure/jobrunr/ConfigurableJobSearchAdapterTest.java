package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.JobDefinition;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
class ConfigurableJobSearchAdapterTest {

    private static final String HANDLER_CLASS = "example.RootHandler";
    private static final String REQUEST_CLASS = "example.RootRequest";

    @Test
    void stampedRootIsVisible() {
        Job root = rootJob();
        root.getMetadata().put(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY, root.getId().toString());

        assertThat(ConfigurableJobSearchAdapter.isCanonicalRootJob(root, definition())).isTrue();
    }

    @Test
    void stampedNestedJobIsHiddenEvenWhenItsHandlerAndRequestLookLikeTheRoot() {
        Job nested = rootJob();
        nested.getMetadata().put(JobWorkflowResolver.WORKFLOW_ROOT_ID_METADATA_KEY, UUID.randomUUID().toString());

        assertThat(ConfigurableJobSearchAdapter.isCanonicalRootJob(nested, definition())).isFalse();
    }

    @Test
    void legacyRootIsRecognizedByConfiguredHandlerAndRequestType() {
        assertThat(ConfigurableJobSearchAdapter.isCanonicalRootJob(rootJob(), definition())).isTrue();
    }

    @Test
    void legacyNestedLambdaWithInheritedLabelIsHidden() {
        Job nested = new Job(new JobDetails(
                HANDLER_CLASS,
                null,
                "enqueueChildren",
                List.of(new JobParameter("java.util.List", List.of()))));

        assertThat(ConfigurableJobSearchAdapter.isCanonicalRootJob(nested, definition())).isFalse();
    }

    @Test
    void legacyContinuationWithDifferentHandlerIsHidden() {
        Job continuation = new Job(new JobDetails(
                "example.PostHandler",
                null,
                "run",
                List.of(new JobParameter("example.PostRequest", new Object()))));

        assertThat(ConfigurableJobSearchAdapter.isCanonicalRootJob(continuation, definition())).isFalse();
    }

    private Job rootJob() {
        return new Job(new JobDetails(
                HANDLER_CLASS,
                null,
                "run",
                List.of(new JobParameter(REQUEST_CLASS, new Object()))));
    }

    private JobDefinition definition() {
        return new JobDefinition(
                "RootHandler",
                true,
                REQUEST_CLASS,
                HANDLER_CLASS,
                List.of(),
                List.of(),
                null,
                false,
                null,
                List.of(),
                null);
    }
}
