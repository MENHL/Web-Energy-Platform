package com.greengrid.admin.modules.report.dto;

import com.greengrid.admin.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报表分页查询参数，字段与前端 ReportList.vue 的 query 一致。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportQuery extends PageQuery {

    /** 报表类型 */
    private String type;
}
