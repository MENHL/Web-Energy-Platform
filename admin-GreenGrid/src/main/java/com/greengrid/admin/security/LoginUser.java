package com.greengrid.admin.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户信息（由 AuthInterceptor 从数据库加载后放入 UserContext）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long id;

    private String username;

    private String name;

    private String role;

    private String roleName;

    private String dept;
}
