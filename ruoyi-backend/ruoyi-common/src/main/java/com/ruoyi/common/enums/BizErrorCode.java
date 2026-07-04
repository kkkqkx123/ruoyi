package com.ruoyi.common.enums;

/**
 * 业务错误码枚举
 * <p>
 * 错误码分段规则：
 * 1xxx - 工单相关错误
 * 2xxx - 设备相关错误
 * 3xxx - 库存相关错误
 * 4xxx - 文件上传相关错误
 * 5xxx - 通用业务错误
 *
 * @author ruoyi
 */
public enum BizErrorCode {

    // ========== 1xxx 工单相关 ==========
    WORK_ORDER_NOT_FOUND(1001, "工单不存在"),
    WORK_ORDER_STATUS_INVALID(1002, "工单当前状态不允许此操作"),
    WORK_ORDER_REPAIR_SOLUTION_EMPTY(1003, "请填写维修方案"),
    WORK_ORDER_IMAGE_EMPTY(1004, "请上传至少一张维修图片"),
    WORK_ORDER_NOT_COMPLETED(1005, "仅已完成的工单可以归档"),

    // ========== 2xxx 设备相关 ==========
    DEVICE_NOT_FOUND(2001, "设备不存在"),
    DEVICE_CODE_DUPLICATE(2002, "设备编号已存在"),

    // ========== 3xxx 库存相关 ==========
    STOCK_NOT_ENOUGH(3001, "库存不足"),
    STOCK_ITEM_NOT_FOUND(3002, "库存物料不存在"),

    // ========== 4xxx 文件上传 ==========
    FILE_UPLOAD_FAILED(4001, "文件上传失败"),
    FILE_SECURITY_CHECK_FAILED(4002, "文件安全校验未通过"),

    // ========== 5xxx 通用 ==========
    PARAMETER_INVALID(5001, "参数不合法"),
    OPERATION_DENIED(5002, "操作被拒绝"),
    ;

    private final int code;
    private final String message;

    BizErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}