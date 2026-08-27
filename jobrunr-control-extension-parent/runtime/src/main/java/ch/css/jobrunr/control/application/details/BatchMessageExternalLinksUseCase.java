package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.details.BatchMessageExternalLink;
import ch.css.jobrunr.control.domain.details.BatchMessageExternalLinkProvider;
import ch.css.jobrunr.control.domain.details.JobMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;

@ApplicationScoped
public class BatchMessageExternalLinksUseCase {

    private static final Logger LOG = Logger.getLogger(BatchMessageExternalLinksUseCase.class);

    private final List<BatchMessageExternalLinkProvider> providers;

    @Inject
    public BatchMessageExternalLinksUseCase(Instance<BatchMessageExternalLinkProvider> providers) {
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(BatchMessageExternalLinkProvider::order)
                        .thenComparing(provider -> provider.getClass().getName()))
                .toList();
    }

    public List<BatchMessageExternalLink> execute(JobMessage message) {
        return providers.stream()
                .flatMap(provider -> linksFrom(provider, message).stream())
                .filter(Objects::nonNull)
                .filter(this::isSafeWebLink)
                .toList();
    }

    private List<BatchMessageExternalLink> linksFrom(BatchMessageExternalLinkProvider provider,
                                                     JobMessage message) {
        try {
            List<BatchMessageExternalLink> links = provider.linksFor(message);
            return links == null ? List.of() : links;
        } catch (RuntimeException exception) {
            LOG.errorf(exception, "Could not create external links for job message rootJobId=%s jobId=%s",
                    message.rootJobId(), message.childJobId());
            return List.of();
        }
    }

    private boolean isSafeWebLink(BatchMessageExternalLink link) {
        return isHttpUri(link.url()) && (link.imageUrl() == null || isHttpUri(link.imageUrl()));
    }

    private boolean isHttpUri(URI uri) {
        return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    }
}
