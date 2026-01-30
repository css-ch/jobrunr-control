package ch.css.jobrunr.control.adapter.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaginationHelperTest {

    @Test
    void shouldCreatePaginationMetadataForFirstPage() {
        Map<String, Object> metadata = PaginationHelper.createPaginationMetadata(0, 10, 25);

        assertEquals(0, metadata.get("page"));
        assertEquals(10, metadata.get("size"));
        assertEquals(25L, metadata.get("totalElements"));
        assertEquals(3, metadata.get("totalPages"));
        assertTrue((Boolean) metadata.get("hasNext"));
        assertFalse((Boolean) metadata.get("hasPrevious"));
        assertEquals(1, metadata.get("startItem"));
        assertEquals(10, metadata.get("endItem"));
    }

    @Test
    void shouldCreatePaginationMetadataForMiddlePage() {
        Map<String, Object> metadata = PaginationHelper.createPaginationMetadata(1, 10, 25);

        assertEquals(1, metadata.get("page"));
        assertTrue((Boolean) metadata.get("hasNext"));
        assertTrue((Boolean) metadata.get("hasPrevious"));
        assertEquals(11, metadata.get("startItem"));
        assertEquals(20, metadata.get("endItem"));
    }

    @Test
    void shouldCreatePaginationMetadataForLastPage() {
        Map<String, Object> metadata = PaginationHelper.createPaginationMetadata(2, 10, 25);

        assertEquals(2, metadata.get("page"));
        assertFalse((Boolean) metadata.get("hasNext"));
        assertTrue((Boolean) metadata.get("hasPrevious"));
        assertEquals(21, metadata.get("startItem"));
        assertEquals(25, metadata.get("endItem"));
    }

    @Test
    void shouldHandleEmptyList() {
        Map<String, Object> metadata = PaginationHelper.createPaginationMetadata(0, 10, 0);

        assertEquals(0L, metadata.get("totalElements"));
        assertEquals(0, metadata.get("totalPages"));
        assertTrue((Boolean) metadata.get("isEmpty"));
        assertEquals(0, metadata.get("startItem"));
        assertEquals(0, metadata.get("endItem"));
    }

    @Test
    void shouldCorrectInvalidPageNumber() {
        Map<String, Object> metadata = PaginationHelper.createPaginationMetadata(10, 10, 25);

        assertEquals(2, metadata.get("page")); // Corrected to last valid page
    }

    @Test
    void shouldPaginateItemsCorrectly() {
        List<String> items = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");

        PaginationHelper.PaginationResult<String> result = PaginationHelper.paginate(items, 0, 3);

        assertEquals(3, result.getPageItems().size());
        assertEquals(List.of("A", "B", "C"), result.getPageItems());
        assertEquals(0, result.getMetadata().get("page"));
        assertEquals(10L, result.getMetadata().get("totalElements"));
    }

    @Test
    void shouldPaginateLastPagePartially() {
        List<String> items = List.of("A", "B", "C", "D", "E");

        PaginationHelper.PaginationResult<String> result = PaginationHelper.paginate(items, 1, 3);

        assertEquals(2, result.getPageItems().size());
        assertEquals(List.of("D", "E"), result.getPageItems());
    }

    @Test
    void shouldHandleEmptyListInPagination() {
        List<String> items = List.of();

        PaginationHelper.PaginationResult<String> result = PaginationHelper.paginate(items, 0, 10);

        assertTrue(result.getPageItems().isEmpty());
        assertEquals(0L, result.getMetadata().get("totalElements"));
    }
}
