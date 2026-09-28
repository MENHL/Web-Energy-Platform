package com.greengrid.admin.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应体，字段固定为 list / total（前端只认这两个字段，禁止 records/rows/items）。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> list;

    /** 总条数 */
    private long total;

    public PageResult() {
    }

    public PageResult(List<T> list, long total) {
        this.list = list;
        this.total = total;
    }

    /** 由 MyBatis-Plus 分页结果转换（自动剥离 records/total 为 list/total） */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    /** 由列表与总数直接构造 */
    public static <T> PageResult<T> of(List<T> list, long total) {
        return new PageResult<>(list, total);
    }

    /** 空结果 */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0);
    }
}
