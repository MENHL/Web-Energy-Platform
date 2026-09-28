package com.greengrid.admin.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 新增用户请求参数。前端不传密码，后端默认 123456；roleName 由后端按 role 映射。
 */
@Data
public class UserSaveRequest {

    @NotBlank(message = "账号 / 工号不能为空")
    private String username;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "super|ops|warehouse|project|guest", message = "角色不合法")
    private String role;

    @NotBlank(message = "所属部门不能为空")
    private String dept;
}
