package com.greengrid.admin.modules.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息出参，字段与前端 UserInfo 类型（src/types/index.ts）一一对应。
 */
@Data
public class UserInfoVO {

    private Long id;

    private String username;

    private String name;

    /** 角色编码：super/ops/warehouse/project/guest */
    private String role;

    private String roleName;

    private String dept;

    /** 1 启用 / 0 禁用 */
    private Integer status;

    private LocalDateTime lastLoginAt;
}
