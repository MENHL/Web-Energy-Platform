package com.greengrid.admin.modules.system.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 编辑用户请求参数（部分更新，只更新非空字段）。
 * <p>前端两处使用：编辑弹窗（name/role/dept）、禁用/启用（仅 status）。</p>
 */
@Data
public class UserUpdateRequest {

    private String name;

    @Pattern(regexp = "super|ops|warehouse|project|guest", message = "角色不合法")
    private String role;

    private String dept;

    /** 1 启用 / 0 禁用 */
    private Integer status;
}
