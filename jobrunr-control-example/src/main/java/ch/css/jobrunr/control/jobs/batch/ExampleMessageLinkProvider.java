package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.domain.details.BatchMessageExternalLink;
import ch.css.jobrunr.control.domain.details.BatchMessageExternalLinkProvider;
import ch.css.jobrunr.control.domain.details.JobMessage;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.util.List;

/**
 * Example integration that links a batch message to a Google search for its child job ID.
 */
@ApplicationScoped
public class ExampleMessageLinkProvider implements BatchMessageExternalLinkProvider {

    @Override
    public int order() {
        return 100;
    }

    @Override
    public List<BatchMessageExternalLink> linksFor(JobMessage message) {
        URI googleSearch = URI.create(
                "https://www.google.com/search?q=JobRunr+child+job+" + message.childJobId());
        return List.of(new BatchMessageExternalLink(
                "Google",
                googleSearch,
                "bi bi-google",
                null));
    }
}
