package com.greengrid.admin.modules.system.dto;

import com.greengrid.admin.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志分页查询参数，字段与前端 LogList.vue 的 query 一致。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LogQuery extends PageQuery {

    /** 所属模块 */
    private String module;

    /** 操作类型：新增/更新/删除/维护/登录 */
    private String action;
}
