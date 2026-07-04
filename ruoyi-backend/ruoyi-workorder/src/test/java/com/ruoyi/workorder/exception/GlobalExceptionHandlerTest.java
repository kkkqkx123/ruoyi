package com.ruoyi.workorder.exception;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BizErrorCode;
import com.ruoyi.common.exception.BizException;
import com.ruoyi.framework.web.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 全局异常处理器 单元测试
 * <p>
 * 覆盖范围：BizException 处理、Exception 兜底
 *
 * @author ruoyi
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("BizException 处理 - 返回正确错误码和消息")
    void shouldHandleBizException() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BizException ex = new BizException(BizErrorCode.WORK_ORDER_NOT_FOUND);
        when(request.getRequestURI()).thenReturn("/api/workorder/1");

        // Act
        AjaxResult result = handler.handleBizException(ex, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.get("code") instanceof Integer);
        assertEquals(1001, result.get("code"));
        assertTrue(result.get("msg") instanceof String);
        assertEquals("工单不存在", result.get("msg"));
    }

    @Test
    @DisplayName("BizException 处理 - 工单状态无效")
    void shouldHandleBizExceptionWithStatusInvalid() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BizException ex = new BizException(BizErrorCode.WORK_ORDER_STATUS_INVALID);
        when(request.getRequestURI()).thenReturn("/api/workorder/complete");

        // Act
        AjaxResult result = handler.handleBizException(ex, request);

        // Assert
        assertNotNull(result);
        assertEquals(1002, result.get("code"));
        assertEquals("工单当前状态不允许此操作", result.get("msg"));
    }

    @Test
    @DisplayName("BizException 处理 - 文件安全校验失败")
    void shouldHandleBizExceptionWithFileSecurity() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BizException ex = new BizException(BizErrorCode.FILE_SECURITY_CHECK_FAILED);
        when(request.getRequestURI()).thenReturn("/api/common/upload");

        // Act
        AjaxResult result = handler.handleBizException(ex, request);

        // Assert
        assertNotNull(result);
        assertEquals(4002, result.get("code"));
        assertEquals("文件安全校验未通过", result.get("msg"));
    }

    @Test
    @DisplayName("Exception 兜底 - 返回异常消息")
    void shouldHandleGenericException() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception ex = new RuntimeException("未知系统错误");
        when(request.getRequestURI()).thenReturn("/api/test");

        // Act
        AjaxResult result = handler.handleException(ex, request);

        // Assert
        assertNotNull(result);
        assertEquals("未知系统错误", result.get("msg"));
    }
}