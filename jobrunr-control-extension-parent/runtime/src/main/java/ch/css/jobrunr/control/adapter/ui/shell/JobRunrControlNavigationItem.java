package ch.css.jobrunr.control.adapter.ui.shell;

import java.util.Set;

/**
 * A navigation entry contributed to the shared UI shell.
 *
 * @param id stable identifier used for active-item highlighting
 * @param label visible menu label
 * @param href absolute or application-relative URL
 * @param icon Bootstrap icon name without the {@code bi-} prefix
 * @param requiredRoles roles of which at least one is required
 */
public record JobRunrControlNavigationItem(
        String id,
        String label,
        String href,
        String icon,
        Set<String> requiredRoles) {

    public JobRunrControlNavigationItem {
        requiredRoles = requiredRoles == null ? Set.of() : Set.copyOf(requiredRoles);
    }
}
