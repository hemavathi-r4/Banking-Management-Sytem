import model.PageResult;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * PaginationTest - Unit tests for the PageResult model class.
 *
 * STAGE 14 — JUNIT TESTING
 * -------------------------
 * Pure Java unit tests verifying PageResult mathematical calculations,
 * page index bounds, and helper methods.
 */
public class PaginationTest {

    @Test
    public void shouldCalculateTotalPagesCorrectly() {
        List<String> items = Arrays.asList("A", "B", "C", "D", "E");

        PageResult<String> pageResult = new PageResult<>(items, 1, 5, 23);

        assertEquals("Current page should be 1", 1, pageResult.getCurrentPage());
        assertEquals("Page size should be 5", 5, pageResult.getPageSize());
        assertEquals("Total records should be 23", 23, pageResult.getTotalRecords());
        assertEquals("Total pages for 23 records with size 5 should be 5", 5, pageResult.getTotalPages());
        assertTrue("Page 1 of 5 should have next page", pageResult.hasNext());
        assertFalse("Page 1 of 5 should not have previous page", pageResult.hasPrevious());
    }

    @Test
    public void shouldHandleExactPageMultiple() {
        PageResult<String> pageResult = new PageResult<>(Collections.emptyList(), 2, 10, 20);

        assertEquals(2, pageResult.getTotalPages());
        assertFalse("Page 2 of 2 should not have next page", pageResult.hasNext());
        assertTrue("Page 2 of 2 should have previous page", pageResult.hasPrevious());
    }

    @Test
    public void shouldHandleEmptyRecords() {
        PageResult<String> pageResult = new PageResult<>(Collections.emptyList(), 1, 10, 0);

        assertEquals(0, pageResult.getTotalRecords());
        assertEquals(0, pageResult.getTotalPages());
        assertFalse(pageResult.hasNext());
        assertFalse(pageResult.hasPrevious());
        assertEquals(0, pageResult.getStartRecordIndex());
        assertEquals(0, pageResult.getEndRecordIndex());
    }

    @Test
    public void shouldNormalizeInvalidPageNumberAndSize() {
        PageResult<String> pageResult = new PageResult<>(Collections.emptyList(), -5, -10, 15);

        assertEquals("Invalid negative page number should normalize to 1", 1, pageResult.getCurrentPage());
        assertEquals("Invalid negative page size should normalize to 1", 1, pageResult.getPageSize());
        assertEquals(15, pageResult.getTotalPages());
    }

    @Test
    public void shouldCalculateRecordRangeIndexesCorrectly() {
        PageResult<String> pageResult = new PageResult<>(Arrays.asList("X", "Y"), 3, 5, 12);

        assertEquals("Start record index for page 3 with size 5 is 11", 11, pageResult.getStartRecordIndex());
        assertEquals("End record index capped at totalRecords (12)", 12, pageResult.getEndRecordIndex());
    }
}
