package com.greengrid.admin.modules.auth.vo;

import lombok.Data;

/**
 * 登录结果出参：token + 用户信息。
 */
@Data
public class LoginVO {

    private String token;

    private UserInfoVO user;
}
