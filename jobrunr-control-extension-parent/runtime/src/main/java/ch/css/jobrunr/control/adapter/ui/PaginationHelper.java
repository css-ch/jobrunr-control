package ch.css.jobrunr.control.adapter.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for pagination logic shared across UI controllers.
 */
public class PaginationHelper {

    private PaginationHelper() {
        // Utility class
    }

    /**
     * Creates pagination metadata for the given parameters.
     *
     * @param page          Current page number (0-based)
     * @param size          Page size
     * @param totalElements Total number of elements
     * @return Map containing pagination metadata
     */
    public static Map<String, Object> createPaginationMetadata(int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        page = Math.max(0, Math.min(page, totalPages - 1)); // Ensure page is in valid range

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("totalElements", totalElements);
        pagination.put("totalPages", totalPages);
        pagination.put("hasNext", page < totalPages - 1);
        pagination.put("hasPrevious", page > 0);
        pagination.put("nextPage", page < totalPages - 1 ? page + 1 : page);
        pagination.put("previousPage", page > 0 ? page - 1 : 0);
        pagination.put("lastPage", Math.max(0, totalPages - 1));
        pagination.put("isEmpty", totalElements == 0);

        // Compute display values for pagination info (Qute doesn't support complex arithmetic)
        int startItem = totalElements > 0 ? (page * size + 1) : 0;
        int endItem = totalElements > 0 ? Math.min((page + 1) * size, (int) totalElements) : 0;
        pagination.put("startItem", startItem);
        pagination.put("endItem", endItem);

        return pagination;
    }

    /**
     * Result of pagination operation containing page items and metadata.
     *
     * @param <T> Type of items being paginated
     */
    public static class PaginationResult<T> {
        private final List<T> pageItems;
        private final Map<String, Object> metadata;
        private final List<TemplateExtensions.PageItem> pageRange;

        public PaginationResult(List<T> pageItems, Map<String, Object> metadata, List<TemplateExtensions.PageItem> pageRange) {
            this.pageItems = pageItems;
            this.metadata = metadata;
            this.pageRange = pageRange;
        }

        public List<T> getPageItems() {
            return pageItems;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public List<TemplateExtensions.PageItem> getPageRange() {
            return pageRange;
        }
    }

    /**
     * Applies pagination to a list of items.
     *
     * @param items List of all items
     * @param page  Current page number (0-based)
     * @param size  Page size
     * @param <T>   Type of items
     * @return PaginationResult containing page items and metadata
     */
    public static <T> PaginationResult<T> paginate(List<T> items, int page, int size) {
        long totalElements = items.size();
        Map<String, Object> metadata = createPaginationMetadata(page, size, totalElements);

        // Extract the validated page number from metadata
        int validatedPage = (int) metadata.get("page");

        int start = Math.min(validatedPage * size, items.size());
        int end = Math.min(start + size, items.size());
        List<T> pageItems = items.subList(start, end);

        // Compute page range for pagination controls
        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(metadata);

        return new PaginationResult<>(pageItems, metadata, pageRange);
    }
}
