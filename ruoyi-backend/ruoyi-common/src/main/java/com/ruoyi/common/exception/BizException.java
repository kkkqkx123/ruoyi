package com.ruoyi.common.exception;

import com.ruoyi.common.enums.BizErrorCode;

/**
 * 统一业务异常（使用 {@link BizErrorCode} 枚举构造）
 * <p>
 * 与 {@link ServiceException} 并存，新代码优先使用此类，
 * 旧代码可逐步迁移。
 *
 * @author ruoyi
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final BizErrorCode errorCode;

    public BizException(BizErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BizException(BizErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public int getCode() {
        return code;
    }

    public BizErrorCode getErrorCode() {
        return errorCode;
    }
}