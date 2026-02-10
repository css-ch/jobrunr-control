package ch.css.jobrunr.control.adapter.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for pagination calculations.
 */
public class TemplateExtensions {

    /**
     * Represents a page in pagination controls.
     *
     * @param index 0-based page index
     * @param label 1-based page label for display
     */
    public record PageItem(int index, int label) {
        public PageItem(int index) {
            this(index, index + 1);
        }
    }

    /**
     * Computes the page range to display in pagination controls.
     * Shows max 5 pages around the current page.
     *
     * @param metadata Pagination metadata containing page and totalPages
     * @return List of PageItem objects to display
     */
    public static List<PageItem> computePageRange(PaginationHelper.PaginationMetadata metadata) {
        int currentPage = metadata.page();
        int totalPages = metadata.totalPages();

        List<PageItem> pages = new ArrayList<>();

        if (totalPages <= 5) {
            // Show all pages if there are 5 or fewer
            for (int i = 0; i < totalPages; i++) {
                pages.add(new PageItem(i));
            }
        } else {
            // Show 5 pages centered around current page
            int start = Math.max(0, currentPage - 2);
            int end = Math.min(totalPages, start + 5);

            // Adjust start if we're near the end
            if (end - start < 5) {
                start = Math.max(0, end - 5);
            }

            for (int i = start; i < end; i++) {
                pages.add(new PageItem(i));
            }
        }

        return pages;
    }
}
