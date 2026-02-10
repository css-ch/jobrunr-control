package ch.css.jobrunr.control.adapter.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginationHelperTest {

    @Test
    void shouldCreatePaginationMetadataForFirstPage() {
        PaginationHelper.PaginationMetadata metadata = PaginationHelper.createPaginationMetadata(0, 10, 25);

        assertEquals(0, metadata.page());
        assertEquals(10, metadata.size());
        assertEquals(25L, metadata.totalElements());
        assertEquals(3, metadata.totalPages());
        assertTrue(metadata.hasNext());
        assertFalse(metadata.hasPrevious());
        assertEquals(1, metadata.startItem());
        assertEquals(10, metadata.endItem());
    }

    @Test
    void shouldCreatePaginationMetadataForMiddlePage() {
        PaginationHelper.PaginationMetadata metadata = PaginationHelper.createPaginationMetadata(1, 10, 25);

        assertEquals(1, metadata.page());
        assertTrue(metadata.hasNext());
        assertTrue(metadata.hasPrevious());
        assertEquals(11, metadata.startItem());
        assertEquals(20, metadata.endItem());
    }

    @Test
    void shouldCreatePaginationMetadataForLastPage() {
        PaginationHelper.PaginationMetadata metadata = PaginationHelper.createPaginationMetadata(2, 10, 25);

        assertEquals(2, metadata.page());
        assertFalse(metadata.hasNext());
        assertTrue(metadata.hasPrevious());
        assertEquals(21, metadata.startItem());
        assertEquals(25, metadata.endItem());
    }

    @Test
    void shouldHandleEmptyList() {
        PaginationHelper.PaginationMetadata metadata = PaginationHelper.createPaginationMetadata(0, 10, 0);

        assertEquals(0L, metadata.totalElements());
        assertEquals(1, metadata.totalPages());
        assertTrue(metadata.isEmpty());
        assertEquals(0, metadata.startItem());
        assertEquals(0, metadata.endItem());
    }

    @Test
    void shouldThrowIllegalArgumentInvalidPageNumber() {
        assertThrows(IllegalArgumentException.class, () -> {
            PaginationHelper.createPaginationMetadata(10, 10, 25);
        });
    }

    @Test
    void shouldPaginateItemsCorrectly() {
        List<String> items = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");

        PaginationHelper.PaginationResult<String> result = PaginationHelper.paginate(items, 0, 5);

        assertEquals(5, result.pageItems().size());
        assertEquals(List.of("A", "B", "C", "D", "E"), result.pageItems());
        assertEquals(0, result.metadata().page());
        assertEquals(10L, result.metadata().totalElements());
    }

    @Test
    void shouldPaginateLastPagePartially() {
        List<String> items = List.of("A", "B", "C", "D", "E", "F", "G");

        PaginationHelper.PaginationResult<String> result = PaginationHelper.paginate(items, 1, 5);

        assertEquals(2, result.pageItems().size());
        assertEquals(List.of("F", "G"), result.pageItems());
    }

    @Test
    void shouldHandleEmptyListInPagination() {
        List<String> items = List.of();

        PaginationHelper.PaginationResult<String> result = PaginationHelper.paginate(items, 0, 10);

        assertTrue(result.pageItems().isEmpty());
        assertEquals(0L, result.metadata().totalElements());
    }
}
