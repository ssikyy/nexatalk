package com.ttikss.nexatalk.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 通用分页响应对象
 * 所有需要分页的接口统一使用此对象返回，保持格式一致
 *
 * @param <T> 列表数据类型
 */
public class PageVO<T> {

    /** 当前页数据列表 */
    private List<T> list;

    /** 当前页码（从 1 开始） */
    private long page;

    /** 每页大小 */
    private long pageSize;

    /** 总记录数 */
    private long total;

    /** 总页数 */
    private long totalPages;

    public PageVO() {}

    /**
     * 从 MyBatis-Plus Page 对象构建（最常用方式）
     */
    public static <T> PageVO<T> of(Page<?> page, List<T> list) {
        PageVO<T> vo = new PageVO<>();
        vo.setList(list);
        vo.setPage(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setTotal(page.getTotal());
        vo.setTotalPages(page.getPages());
        return vo;
    }

    /**
     * 从 MyBatis-Plus IPage 对象构建（用于转换）
     */
    public static <T> PageVO<T> from(IPage<?> page) {
        PageVO<T> vo = new PageVO<>();
        vo.setPage(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setTotal(page.getTotal());
        vo.setTotalPages(page.getPages());
        vo.setList((List<T>) page.getRecords());
        return vo;
    }

    /**
     * 设置列表数据（用于 IPage 转换后）
     */
    public void setRecords(List<T> records) {
        this.list = records;
    }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }

    public long getPage() { return page; }
    public void setPage(long page) { this.page = page; }

    public long getPageSize() { return pageSize; }
    public void setPageSize(long pageSize) { this.pageSize = pageSize; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getTotalPages() { return totalPages; }
    public void setTotalPages(long totalPages) { this.totalPages = totalPages; }
}
