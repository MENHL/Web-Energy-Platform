package com.greengrid.admin.modules.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户出参，字段与前端 UserInfo 类型（src/types/index.ts）一一对应。
 * <p>绝不包含 password。</p>
 */
@Data
public class UserVO {

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
