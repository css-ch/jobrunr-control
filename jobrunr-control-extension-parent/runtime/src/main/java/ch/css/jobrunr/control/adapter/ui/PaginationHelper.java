package ch.css.jobrunr.control.adapter.ui;

import java.util.List;

/**
 * Helper class for pagination logic shared across UI controllers.
 */
public final class PaginationHelper {

    private PaginationHelper() {
        // Utility class
    }

    /**
     * Creates pagination metadata for the given parameters.
     *
     * @param page          Current page number (0-based)
     * @param size          Page size
     * @param totalElements Total number of elements
     * @return PaginationMetadata containing pagination information
     */
    public static PaginationMetadata createPaginationMetadata(int page, int size, long totalElements) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalElements / size));
        if (page > totalPages - 1) {
            throw new IllegalArgumentException(String.format("page %d is greater than %d", page, totalPages));
        }

        int lastPage = Math.max(0, totalPages - 1);
        boolean isEmpty = totalElements == 0;
        boolean hasNext = page < lastPage;
        boolean hasPrevious = page > 0;

        // Compute display values for pagination info (Qute doesn't support complex arithmetic)
        int startItem = isEmpty ? 0 : (page * size + 1);
        int endItem = isEmpty ? 0 : Math.min((page + 1) * size, (int) totalElements);

        return new PaginationMetadata(
                page,
                size,
                totalElements,
                totalPages,
                hasNext,
                hasPrevious,
                hasNext ? page + 1 : page,
                hasPrevious ? page - 1 : 0,
                lastPage,
                isEmpty,
                startItem,
                endItem
        );
    }

    /**
     * Pagination metadata containing all pagination-related information.
     */
    public record PaginationMetadata(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious,
            int nextPage,
            int previousPage,
            int lastPage,
            boolean isEmpty,
            int startItem,
            int endItem) {
    }

    /**
     * Result of pagination operation containing page items and metadata.
     *
     * @param <T> Type of items being paginated
     */
    public record PaginationResult<T>(List<T> pageItems, PaginationMetadata metadata,
                                      List<TemplateExtensions.PageItem> pageRange) {
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
        PaginationMetadata metadata = createPaginationMetadata(page, size, items.size());

        int start = Math.min(metadata.page() * metadata.size(), items.size());
        int end = Math.min(start + metadata.size(), items.size());
        List<T> pageItems = items.subList(start, end);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(metadata);

        return new PaginationResult<>(pageItems, metadata, pageRange);
    }
}
