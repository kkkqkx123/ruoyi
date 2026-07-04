package com.ruoyi.workorder.service.impl;

import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.ISysNoticeService;
import com.ruoyi.workorder.domain.WorkOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AsyncWorkOrderService 异步工单通知服务 单元测试
 * <p>
 * 覆盖范围：紧急工单通知推送、异常处理
 *
 * @author ruoyi
 */
@ExtendWith(MockitoExtension.class)
class AsyncWorkOrderServiceTest {

    @Mock
    private ISysNoticeService noticeService;

    @InjectMocks
    private AsyncWorkOrderService asyncWorkOrderService;

    private WorkOrder urgentWorkOrder;

    @BeforeEach
    void setUp() {
        urgentWorkOrder = new WorkOrder();
        urgentWorkOrder.setOrderId(1L);
        urgentWorkOrder.setOrderNo("WO202401010001");
        urgentWorkOrder.setFaultDesc("设备无法启动");
        urgentWorkOrder.setUrgencyLevel("2");
    }

    @Test
    @DisplayName("推送紧急工单通知 - 创建并发送通知")
    void shouldPushUrgentNoticeSuccessfully() {
        // Arrange
        when(noticeService.insertNotice(any(SysNotice.class))).thenReturn(1);

        // Act
        asyncWorkOrderService.pushUrgentNotice(urgentWorkOrder);

        // Assert
        ArgumentCaptor<SysNotice> noticeCaptor = ArgumentCaptor.forClass(SysNotice.class);
        verify(noticeService, times(1)).insertNotice(noticeCaptor.capture());

        SysNotice pushedNotice = noticeCaptor.getValue();
        assertTrue(pushedNotice.getNoticeTitle().contains("WO202401010001"));
        assertEquals("1", pushedNotice.getNoticeType());
        assertEquals("0", pushedNotice.getStatus());
        assertTrue(pushedNotice.getNoticeContent().contains("设备无法启动"));
        assertTrue(pushedNotice.getNoticeContent().contains("WO202401010001"));
        assertEquals("system", pushedNotice.getCreateBy());
    }

    @Test
    @DisplayName("推送通知失败 - 不抛出异常（内部 catch）")
    void shouldNotThrowExceptionWhenNoticeFails() {
        // Arrange
        when(noticeService.insertNotice(any(SysNotice.class))).thenThrow(new RuntimeException("DB error"));

        // Act & Assert
        assertDoesNotThrow(() -> asyncWorkOrderService.pushUrgentNotice(urgentWorkOrder));
        verify(noticeService, times(1)).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("推送通知 - 紧急工单包含完整工单号信息")
    void shouldIncludeOrderNoInNotice() {
        // Arrange
        when(noticeService.insertNotice(any(SysNotice.class))).thenReturn(1);

        // Act
        asyncWorkOrderService.pushUrgentNotice(urgentWorkOrder);

        // Assert
        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(noticeService).insertNotice(captor.capture());
        SysNotice notice = captor.getValue();
        assertTrue(notice.getNoticeTitle().contains(urgentWorkOrder.getOrderNo()));
        assertTrue(notice.getNoticeContent().contains(urgentWorkOrder.getOrderNo()));
        assertTrue(notice.getNoticeContent().contains(urgentWorkOrder.getFaultDesc()));
    }
}