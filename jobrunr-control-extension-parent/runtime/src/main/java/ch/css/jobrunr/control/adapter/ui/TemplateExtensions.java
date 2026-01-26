package ch.css.jobrunr.control.adapter.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for pagination calculations.
 */
public class TemplateExtensions {

    /**
     * Represents a page in pagination controls.
     */
    public static class PageItem {
        private final int index; // 0-based
        private final int label; // 1-based for display

        public PageItem(int index) {
            this.index = index;
            this.label = index + 1;
        }

        @SuppressWarnings("unused") // Used in templates
        public int getIndex() {
            return index;
        }

        @SuppressWarnings("unused") // Used in templates
        public int getLabel() {
            return label;
        }
    }

    /**
     * Computes the page range to display in pagination controls.
     * Shows max 5 pages around the current page.
     *
     * @param pagination Pagination metadata map containing 'page' and 'totalPages'
     * @return List of PageItem objects to display
     */
    public static List<PageItem> computePageRange(Map<String, Object> pagination) {
        int currentPage = (int) pagination.get("page");
        int totalPages = (int) pagination.get("totalPages");

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
