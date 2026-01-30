package ch.css.jobrunr.control.adapter.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateExtensionsTest {

    @Test
    void shouldComputePageRangeForFewPages() {
        Map<String, Object> pagination = Map.of("page", 1, "totalPages", 3);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(pagination);

        assertEquals(3, pageRange.size());
        assertEquals(0, pageRange.get(0).getIndex());
        assertEquals(1, pageRange.get(0).getLabel());
        assertEquals(2, pageRange.get(2).getIndex());
        assertEquals(3, pageRange.get(2).getLabel());
    }

    @Test
    void shouldComputePageRangeForManyPagesAtStart() {
        Map<String, Object> pagination = Map.of("page", 0, "totalPages", 10);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(pagination);

        assertEquals(5, pageRange.size());
        assertEquals(0, pageRange.get(0).getIndex());
        assertEquals(4, pageRange.get(4).getIndex());
    }

    @Test
    void shouldComputePageRangeForManyPagesInMiddle() {
        Map<String, Object> pagination = Map.of("page", 5, "totalPages", 10);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(pagination);

        assertEquals(5, pageRange.size());
        assertEquals(3, pageRange.get(0).getIndex());
        assertEquals(7, pageRange.get(4).getIndex());
    }

    @Test
    void shouldComputePageRangeForManyPagesAtEnd() {
        Map<String, Object> pagination = Map.of("page", 9, "totalPages", 10);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(pagination);

        assertEquals(5, pageRange.size());
        assertEquals(5, pageRange.get(0).getIndex());
        assertEquals(9, pageRange.get(4).getIndex());
    }

    @Test
    void shouldComputePageRangeForExactlyFivePages() {
        Map<String, Object> pagination = Map.of("page", 2, "totalPages", 5);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(pagination);

        assertEquals(5, pageRange.size());
        assertEquals(0, pageRange.get(0).getIndex());
        assertEquals(4, pageRange.get(4).getIndex());
    }

    @Test
    void shouldCreatePageItemWithCorrectIndexAndLabel() {
        TemplateExtensions.PageItem pageItem = new TemplateExtensions.PageItem(3);

        assertEquals(3, pageItem.getIndex());
        assertEquals(4, pageItem.getLabel());
    }
}
