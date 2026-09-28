package com.greengrid.admin.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一错误码常量。
 */
public final class ErrorCode {

    /** 成功 */
    public static final int SUCCESS = 0;

    /** 业务失败（前端直接弹出 message） */
    public static final int BIZ_ERROR = 1;

    /** 参数校验失败 */
    public static final int PARAM_ERROR = 400;

    /** 未登录 / token 失效 */
    public static final int UNAUTHORIZED = 401;

    /** 无权限 */
    public static final int FORBIDDEN = 403;

    /** 资源不存在 */
    public static final int NOT_FOUND = 404;

    /** 服务内部错误 */
    public static final int SERVER_ERROR = 500;

    private ErrorCode() {
    }
}
