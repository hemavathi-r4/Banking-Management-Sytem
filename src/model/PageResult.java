package model;

import java.util.Collections;
import java.util.List;

/**
 * PageResult - Generic model container for paginated database query results.
 *
 * STAGE 14 — SEARCH, FILTERING & PAGINATION
 * -----------------------------------------
 * Holds a single page of results (records) along with metadata required
 * for rendering pagination controls (current page, total records, total pages, page size).
 *
 * @param <T> the entity type contained in this page (e.g., AuditLog, Transaction, Customer, Account)
 */
public class PageResult<T> {

    private final List<T> records;
    private final int     currentPage;
    private final int     pageSize;
    private final long    totalRecords;
    private final int     totalPages;

    /**
     * Constructor computing totalPages from totalRecords and pageSize.
     *
     * @param records      the list of records for the current page
     * @param currentPage  the 1-indexed current page number
     * @param pageSize     the maximum number of records per page
     * @param totalRecords total count of matching records across all pages
     */
    public PageResult(List<T> records, int currentPage, int pageSize, long totalRecords) {
        this.records      = (records != null) ? records : Collections.emptyList();
        this.currentPage  = Math.max(1, currentPage);
        this.pageSize     = Math.max(1, pageSize);
        this.totalRecords = Math.max(0, totalRecords);
        this.totalPages   = (int) Math.ceil((double) this.totalRecords / (double) this.pageSize);
    }

    public List<T> getRecords() {
        return records;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasNext() {
        return currentPage < totalPages;
    }

    public boolean hasPrevious() {
        return currentPage > 1;
    }

    public long getStartRecordIndex() {
        if (totalRecords == 0) return 0;
        return (long) (currentPage - 1) * pageSize + 1;
    }

    public long getEndRecordIndex() {
        return Math.min(totalRecords, (long) currentPage * pageSize);
    }

    @Override
    public String toString() {
        return String.format(
            "PageResult { page=%d/%d, size=%d, totalRecords=%d, recordsCount=%d }",
            currentPage, Math.max(1, totalPages), pageSize, totalRecords, records.size()
        );
    }
}
