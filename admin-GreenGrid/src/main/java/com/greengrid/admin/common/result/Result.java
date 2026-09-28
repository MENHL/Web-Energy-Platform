package com.greengrid.admin.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体。
 * <p>约定（与前端 request.ts 响应拦截器一致）：</p>
 * <ul>
 *   <li>成功：{@code { code: 0, data: ... }}</li>
 *   <li>业务失败：{@code { code: 1, message: "..." }}</li>
 * </ul>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：0 成功，1 业务失败，401 未登录，403 无权限，400 参数错误，500 服务异常 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（无数据） */
    public static <T> Result<T> ok() {
        return new Result<>(ErrorCode.SUCCESS, "success", null);
    }

    /** 成功（携带数据） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS, "success", data);
    }

    /** 业务失败（code = 1） */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ErrorCode.BIZ_ERROR, message, null);
    }

    /** 失败（指定 code，如 401/403/400/500） */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
