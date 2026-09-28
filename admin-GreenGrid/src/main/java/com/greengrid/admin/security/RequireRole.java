package com.greengrid.admin.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解：标注在 Controller 方法或类上，声明允许访问的角色。
 * <p>用法：{@code @RequireRole({"super", "ops"})}</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 允许访问的角色编码列表 */
    String[] value() default {};
}
