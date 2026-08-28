package ch.css.jobrunr.control.adapter.ui.shell;

import java.util.List;

/**
 * Contributes entries to the shared navigation surrounding JobRunr Control pages.
 */
public interface JobRunrControlNavigationContributor {

    /**
     * Determines the order of this contributor relative to other contributors.
     * Lower values are rendered first.
     */
    default int order() {
        return 1000;
    }

    List<JobRunrControlNavigationItem> navigationItems();
}
