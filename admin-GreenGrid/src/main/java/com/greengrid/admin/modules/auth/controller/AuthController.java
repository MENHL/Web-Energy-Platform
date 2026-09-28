package com.greengrid.admin.modules.auth.controller;

import com.greengrid.admin.common.result.Result;
import com.greengrid.admin.common.util.IpUtil;
import com.greengrid.admin.modules.auth.dto.LoginRequest;
import com.greengrid.admin.modules.auth.service.AuthService;
import com.greengrid.admin.modules.auth.vo.LoginVO;
import com.greengrid.admin.modules.auth.vo.UserInfoVO;
import com.greengrid.admin.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录 / 获取当前用户 / 退出登录。
 * <p>注意：完整路径是 /api/auth/**（context-path 前缀由 server.servlet.context-path 统一加上）。</p>
 */
@Tag(name = "认证", description = "登录、获取当前用户、退出登录")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "账号登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request, IpUtil.getClientIp(httpRequest)));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.ok(authService.currentUser(UserContext.getUserId()));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，退出由前端清除本地 token 即可；此接口保留用于后续扩展（如退出日志、令牌黑名单）
        return Result.ok();
    }
}
