package ch.css.jobrunr.control.adapter.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateExtensionsTest {

    @Test
    void shouldComputePageRangeForFewPages() {
        var metadata = PaginationHelper.createPaginationMetadata(1, 10, 30);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(metadata);

        assertEquals(3, pageRange.size());
        assertEquals(0, pageRange.get(0).index());
        assertEquals(1, pageRange.get(0).label());
        assertEquals(2, pageRange.get(2).index());
        assertEquals(3, pageRange.get(2).label());
    }

    @Test
    void shouldComputePageRangeForManyPagesAtStart() {
        var metadata = PaginationHelper.createPaginationMetadata(0, 10, 100);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(metadata);

        assertEquals(5, pageRange.size());
        assertEquals(0, pageRange.get(0).index());
        assertEquals(4, pageRange.get(4).index());
    }

    @Test
    void shouldComputePageRangeForManyPagesInMiddle() {
        var metadata = PaginationHelper.createPaginationMetadata(5, 10, 100);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(metadata);

        assertEquals(5, pageRange.size());
        assertEquals(3, pageRange.get(0).index());
        assertEquals(7, pageRange.get(4).index());
    }

    @Test
    void shouldComputePageRangeForManyPagesAtEnd() {
        var metadata = PaginationHelper.createPaginationMetadata(9, 10, 100);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(metadata);

        assertEquals(5, pageRange.size());
        assertEquals(5, pageRange.get(0).index());
        assertEquals(9, pageRange.get(4).index());
    }

    @Test
    void shouldComputePageRangeForExactlyFivePages() {
        var metadata = PaginationHelper.createPaginationMetadata(2, 10, 50);

        List<TemplateExtensions.PageItem> pageRange = TemplateExtensions.computePageRange(metadata);

        assertEquals(5, pageRange.size());
        assertEquals(0, pageRange.get(0).index());
        assertEquals(4, pageRange.get(4).index());
    }

    @Test
    void shouldCreatePageItemWithCorrectIndexAndLabel() {
        TemplateExtensions.PageItem pageItem = new TemplateExtensions.PageItem(3);

        assertEquals(3, pageItem.index());
        assertEquals(4, pageItem.label());
    }
}
