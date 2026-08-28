package ch.css.jobrunr.control.adapter.ui.shell;

import java.util.List;

/**
 * Contributes entries to the shared navigation surrounding JobRunr Control pages.
 */
public interface JobRunrControlNavigationContributor {

    List<JobRunrControlNavigationItem> navigationItems();
}
