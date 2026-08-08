package com.careerpilot.common.dto;

import java.util.List;

/**
 * Mirrors CareerPilot.Shared.Models.PagedResult<T>.
 */
public class PagedResult<T> {

    private List<T> items;
    private int page;
    private int pageSize;
    private long totalCount;
    private int totalPages;

    public PagedResult() {
    }

    public PagedResult(List<T> items, int page, int pageSize, long totalCount) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
