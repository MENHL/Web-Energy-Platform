package com.greengrid.admin.security;

import com.greengrid.admin.common.exception.BizException;
import com.greengrid.admin.common.result.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色拦截器：校验 {@link RequireRole} 注解声明的角色。
 * <p>必须在 AuthInterceptor 之后执行（WebConfig 中的注册顺序已保证）。</p>
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null || requireRole.value().length == 0) {
            return true;
        }

        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        for (String role : requireRole.value()) {
            if (role.equals(user.getRole())) {
                return true;
            }
        }
        throw new BizException(ErrorCode.FORBIDDEN, "暂无该操作权限");
    }
}
