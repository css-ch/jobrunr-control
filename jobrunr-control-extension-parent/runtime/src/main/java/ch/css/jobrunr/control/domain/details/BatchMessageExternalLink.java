package ch.css.jobrunr.control.domain.details;

import java.net.URI;
import java.util.Objects;

/**
 * An external link displayed next to a batch message.
 *
 * @param label     optional visible link text
 * @param url       destination of the link
 * @param iconClass optional Bootstrap Icons CSS classes
 * @param imageUrl  optional image displayed instead of the icon
 * @param ariaLabel optional accessible name; falls back to {@code label}
 */
public record BatchMessageExternalLink(String label, URI url, String iconClass, URI imageUrl, String ariaLabel) {

    public BatchMessageExternalLink {
        if (label != null && label.isBlank()) {
            label = null;
        }
        if (ariaLabel != null && ariaLabel.isBlank()) {
            ariaLabel = null;
        }
        if (label == null && ariaLabel == null) {
            throw new IllegalArgumentException("External link needs a label or ariaLabel");
        }
        Objects.requireNonNull(url, "External link URL must not be null");
        if (iconClass != null && iconClass.isBlank()) {
            iconClass = null;
        }
    }

    public BatchMessageExternalLink(String label, URI url, String iconClass, URI imageUrl) {
        this(label, url, iconClass, imageUrl, null);
    }

    public BatchMessageExternalLink(String label, URI url) {
        this(label, url, null, null, null);
    }

    public String accessibleLabel() {
        return ariaLabel != null ? ariaLabel : label;
    }

    public boolean hasLabel() {
        return label != null;
    }

    public boolean hasIcon() {
        return iconClass != null;
    }

    public boolean hasImage() {
        return imageUrl != null;
    }
}
