package com.ruoyi.workorder.exception;

import com.ruoyi.common.enums.BizErrorCode;
import com.ruoyi.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BizException 业务异常 单元测试
 * <p>
 * 覆盖范围：构造函数、错误码/消息/getter
 *
 * @author ruoyi
 */
@DisplayName("BizException 业务异常测试")
class BizExceptionTest {

    @Test
    @DisplayName("通过 BizErrorCode 构造 - 正确设置 code 和 message")
    void shouldCreateFromBizErrorCode() {
        // Act
        BizException ex = new BizException(BizErrorCode.WORK_ORDER_NOT_FOUND);

        // Assert
        assertEquals(1001, ex.getCode());
        assertEquals("工单不存在", ex.getMessage());
    }

    @Test
    @DisplayName("通过 BizErrorCode 构造 - 不同错误码")
    void shouldCreateFromDifferentErrorCodes() {
        // Act
        BizException ex1 = new BizException(BizErrorCode.DEVICE_NOT_FOUND);
        BizException ex2 = new BizException(BizErrorCode.FILE_UPLOAD_FAILED);
        BizException ex3 = new BizException(BizErrorCode.OPERATION_DENIED);

        // Assert
        assertEquals(2001, ex1.getCode());
        assertEquals("设备不存在", ex1.getMessage());

        assertEquals(4001, ex2.getCode());
        assertEquals("文件上传失败", ex2.getMessage());

        assertEquals(5002, ex3.getCode());
        assertEquals("操作被拒绝", ex3.getMessage());
    }

    @Test
    @DisplayName("通过 BizErrorCode + 自定义消息构造 - 覆盖默认消息")
    void shouldCreateWithCustomMessage() {
        // Act
        BizException ex = new BizException(BizErrorCode.WORK_ORDER_NOT_FOUND, "自定义业务异常消息");

        // Assert
        assertEquals(1001, ex.getCode());
        assertEquals("自定义业务异常消息", ex.getMessage());
    }

    @Test
    @DisplayName("BizException 是 RuntimeException 子类")
    void shouldBeRuntimeExceptionSubclass() {
        // Assert
        BizException ex = new BizException(BizErrorCode.PARAMETER_INVALID);
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("getCode 返回正确错误码")
    void shouldReturnCorrectCode() {
        // Arrange
        BizException ex = new BizException(BizErrorCode.WORK_ORDER_STATUS_INVALID);

        // Assert
        assertEquals(1002, ex.getCode());
    }
}