package com.greengrid.admin.common.exception;

import lombok.Getter;

/**
 * 业务异常。抛出的异常由 GlobalExceptionHandler 统一转换为规范响应。
 * <p>默认 code = 1（业务失败）；也可指定 401/403 等让 HTTP 状态码同步变化。</p>
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码，见 ErrorCode */
    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 1;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
