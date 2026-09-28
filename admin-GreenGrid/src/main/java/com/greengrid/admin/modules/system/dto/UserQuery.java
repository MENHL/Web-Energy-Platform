package com.greengrid.admin.modules.system.dto;

import com.greengrid.admin.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询参数，字段与前端 UserManage.vue 的 query 一致。
 * <p>status 为数字 0（禁用）/ 1（启用）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageQuery {

    /** 角色编码 */
    private String role;

    /** 账号状态：0 禁用 / 1 启用 */
    private Integer status;
}
