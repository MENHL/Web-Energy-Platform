package com.greengrid.admin.modules.project.dto;

import com.greengrid.admin.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目分页查询参数。
 * <p>继承 PageQuery 的 page / pageSize / keyword，追加 status / region 筛选，
 * 字段名与前端 ProjectList.vue 的 query 完全一致。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectQuery extends PageQuery {

    /** 项目状态：进行中/已暂停/已完成 */
    private String status;

    /** 所属区域 */
    private String region;
}
