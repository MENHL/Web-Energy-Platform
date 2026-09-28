package com.greengrid.admin.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 是否记住登录（前端决定是否持久化 token，后端目前不依赖此字段） */
    private Boolean remember;
}
