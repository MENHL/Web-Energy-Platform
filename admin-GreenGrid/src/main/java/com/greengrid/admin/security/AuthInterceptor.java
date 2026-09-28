package com.greengrid.admin.security;

import com.greengrid.admin.common.exception.BizException;
import com.greengrid.admin.common.result.ErrorCode;
import com.greengrid.admin.modules.system.entity.SysUser;
import com.greengrid.admin.modules.system.mapper.SysUserMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器：校验 Bearer token，解析出 userId 后实时查库，把登录用户写入 UserContext。
 * <p>实时查库的好处：用户被禁用（status=0）或删除后，旧 token 立即失效；角色变更也无需重新登录。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        // CORS 预检请求（OPTIONS）不携带 token，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        Long userId;
        try {
            userId = jwtUtil.parseUserId(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "该账号已被禁用，请联系管理员");
        }

        UserContext.set(toLoginUser(user));
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        // 请求结束清理 ThreadLocal，避免线程复用导致的数据串用
        UserContext.clear();
    }

    private LoginUser toLoginUser(SysUser user) {
        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole(),
                user.getRoleName(),
                user.getDept());
    }
}
