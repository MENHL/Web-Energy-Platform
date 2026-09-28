package com.greengrid.admin.common.query;

/**
 * 分页查询基类，各模块查询对象继承它并追加模块专用筛选字段。
 * <p>page 从 1 开始；pageSize 默认 10，上限 100（防止一次查全表拖垮服务）。</p>
 */
public class PageQuery {

    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    /** 页码，从 1 开始 */
    private long page = 1;

    /** 每页条数 */
    private long pageSize = DEFAULT_PAGE_SIZE;

    /** 关键字（模糊匹配） */
    private String keyword;

    public long getPage() {
        return page < 1 ? 1 : page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getPageSize() {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /** 是否为空关键字 */
    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
