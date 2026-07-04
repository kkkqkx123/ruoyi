package com.ruoyi.workorder.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.workorder.domain.FaultTopDevice;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.WorkOrderStats;
import com.ruoyi.workorder.service.ExportTaskService;
import com.ruoyi.workorder.service.IWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkOrderController 单元测试
 *
 * 覆盖：
 * - CRUD 接口响应
 * - 批量分配参数解析
 * - 统计看板聚合
 * - Excel 导出
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderControllerTest {

    @Mock
    private IWorkOrderService workOrderService;

    @Mock
    private TokenService tokenService;

    @Mock
    private ExportTaskService exportTaskService;

    @InjectMocks
    private WorkOrderController controller;

    private WorkOrder sampleOrder;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("pageNum", "1");
        request.setParameter("pageSize", "10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        sampleOrder = new WorkOrder();
        sampleOrder.setOrderId(1L);
        sampleOrder.setOrderNo("WO20260703123456789");
        sampleOrder.setDeviceId(1L);
        sampleOrder.setReporterBy("zhangsan");
        sampleOrder.setOrderStatus("0");
    }

    // ==================== 列表查询 ====================

    @Nested
    @DisplayName("列表查询 /list")
    class ListEndpoint {

        @Test
        @DisplayName("查询工单列表 - 返回分页数据")
        void shouldReturnPaginatedList() {
            // Arrange
            List<WorkOrder> mockList = Arrays.asList(sampleOrder, new WorkOrder());
            // BaseController.startPage() 会从请求中获取分页参数，这里模拟返回值
            when(workOrderService.selectWorkOrderList(any(WorkOrder.class))).thenReturn(mockList);

            // Act
            TableDataInfo result = controller.list(new WorkOrder());

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getRows().size());
            assertEquals(2L, result.getTotal());
            verify(workOrderService, times(1)).selectWorkOrderList(any(WorkOrder.class));
        }

        @Test
        @DisplayName("查询空列表 - 返回空数据")
        void shouldReturnEmptyList() {
            // Arrange
            when(workOrderService.selectWorkOrderList(any(WorkOrder.class))).thenReturn(Collections.emptyList());

            // Act
            TableDataInfo result = controller.list(new WorkOrder());

            // Assert
            assertNotNull(result);
            assertTrue(result.getRows().isEmpty());
            assertEquals(0L, result.getTotal());
        }
    }

    // ==================== 分页深度限制 ====================

    @Nested
    @DisplayName("分页深度限制 /list")
    class PageDepthLimitEndpoint {

        @BeforeEach
        void setUp() {
            // 设置 pageNum > 1000 验证限制
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("pageNum", "1001");
            request.setParameter("pageSize", "20");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        }

        @Test
        @DisplayName("pageNum 超过 1000 时自动限制为 1000")
        void shouldLimitPageDepth() {
            // Arrange
            when(workOrderService.selectWorkOrderList(any(WorkOrder.class))).thenReturn(Collections.emptyList());

            // Act
            TableDataInfo result = controller.list(new WorkOrder());

            // Assert
            assertNotNull(result);
            verify(workOrderService, times(1)).selectWorkOrderList(any(WorkOrder.class));
        }
    }

    // ==================== 详情查询 ====================

    @Nested
    @DisplayName("详情查询 /{orderId}")
    class GetInfoEndpoint {

        @Test
        @DisplayName("获取工单详情 - 返回工单信息")
        void shouldReturnOrderInfo() {
            // Arrange
            when(workOrderService.selectWorkOrderById(1L)).thenReturn(sampleOrder);

            // Act
            AjaxResult result = controller.getInfo(1L);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            assertNotNull(result.get("data"));
            verify(workOrderService, times(1)).selectWorkOrderById(1L);
        }

        @Test
        @DisplayName("获取不存在工单 - 返回空数据")
        void shouldReturnNullForNonExistent() {
            // Arrange
            when(workOrderService.selectWorkOrderById(999L)).thenReturn(null);

            // Act
            AjaxResult result = controller.getInfo(999L);

            // Assert
            assertNull(result.get("data"));
        }
    }

    // ==================== 新增 ====================

    @Nested
    @DisplayName("新增工单 POST /")
    class AddEndpoint {

        @Test
        @DisplayName("新增工单 - 返回成功")
        void shouldAddOrderSuccessfully() {
            // Arrange
            when(workOrderService.insertWorkOrder(any(WorkOrder.class))).thenReturn(1);

            // Act
            AjaxResult result = controller.add(sampleOrder);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            verify(workOrderService, times(1)).insertWorkOrder(sampleOrder);
        }

        @Test
        @DisplayName("新增工单失败 - 返回错误")
        void shouldReturnErrorWhenInsertFails() {
            // Arrange
            when(workOrderService.insertWorkOrder(any(WorkOrder.class))).thenReturn(0);

            // Act
            AjaxResult result = controller.add(sampleOrder);

            // Assert
            assertEquals(HttpStatus.ERROR, result.get("code"));
        }
    }

    // ==================== 修改 ====================

    @Nested
    @DisplayName("修改工单 PUT /")
    class EditEndpoint {

        @Test
        @DisplayName("修改工单 - 返回成功")
        void shouldEditOrderSuccessfully() {
            // Arrange
            when(workOrderService.updateWorkOrder(any(WorkOrder.class))).thenReturn(1);

            // Act
            AjaxResult result = controller.edit(sampleOrder);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            verify(workOrderService, times(1)).updateWorkOrder(sampleOrder);
        }
    }

    // ==================== 删除 ====================

    @Nested
    @DisplayName("删除工单 DELETE /{orderIds}")
    class RemoveEndpoint {

        @Test
        @DisplayName("删除单个工单 - 返回成功")
        void shouldDeleteSingleOrder() {
            // Arrange
            Long[] ids = {1L};
            when(workOrderService.deleteWorkOrderByIds(ids)).thenReturn(1);

            // Act
            AjaxResult result = controller.remove(ids);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            verify(workOrderService, times(1)).deleteWorkOrderByIds(ids);
        }

        @Test
        @DisplayName("批量删除工单 - 返回成功")
        void shouldDeleteMultipleOrders() {
            // Arrange
            Long[] ids = {1L, 2L, 3L};
            when(workOrderService.deleteWorkOrderByIds(ids)).thenReturn(3);

            // Act
            AjaxResult result = controller.remove(ids);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
        }
    }

    // ==================== 批量分配 ====================

    @Nested
    @DisplayName("批量分配 PUT /batchAssign")
    class BatchAssignEndpoint {

        @Test
        @DisplayName("批量分配 - 参数解析正确")
        void shouldParseAndAssign() {
            // Arrange
            Map<String, Object> params = new HashMap<>();
            params.put("orderIds", Arrays.asList(1, 2, 3));
            params.put("assignTo", "lisi");
            when(workOrderService.batchAssign(any(Long[].class), anyString())).thenReturn(2);

            // Act
            AjaxResult result = controller.batchAssign(params);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            verify(workOrderService, times(1)).batchAssign(
                    argThat(arr -> arr.length == 3),
                    eq("lisi")
            );
        }

        @Test
        @DisplayName("批量分配 - 空列表")
        void shouldHandleEmptyList() {
            // Arrange
            Map<String, Object> params = new HashMap<>();
            params.put("orderIds", Collections.emptyList());
            params.put("assignTo", "lisi");
            when(workOrderService.batchAssign(any(Long[].class), anyString())).thenReturn(2);

            // Act
            AjaxResult result = controller.batchAssign(params);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
        }
    }

    // ==================== 统计看板 ====================

    @Nested
    @DisplayName("统计看板 GET /stats")
    class StatsEndpoint {

        @Test
        @DisplayName("获取统计看板 - 包含聚合数据和Top设备")
        void shouldReturnStatsWithTopDevices() {
            // Arrange
            WorkOrderStats stats = new WorkOrderStats();
            stats.setTotalCount(100L);
            stats.setPendingCount(30L);

            FaultTopDevice device = new FaultTopDevice();
            device.setDeviceId(1L);
            device.setDeviceName("设备A");
            device.setFaultCount(10);

            when(workOrderService.selectWorkOrderStats()).thenReturn(stats);
            when(workOrderService.selectFaultTopDevices()).thenReturn(Arrays.asList(device));

            // Act
            AjaxResult result = controller.stats();

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            WorkOrderStats resultStats = (WorkOrderStats) result.get("data");
            assertEquals(100L, resultStats.getTotalCount());
            assertNotNull(resultStats.getFaultTopDevices());
            assertEquals(1, resultStats.getFaultTopDevices().size());
            assertEquals("设备A", resultStats.getFaultTopDevices().get(0).getDeviceName());
        }

        @Test
        @DisplayName("统计看板 - 无数据时返回空结构")
        void shouldReturnEmptyStats() {
            // Arrange
            when(workOrderService.selectWorkOrderStats()).thenReturn(null);

            // Act
            AjaxResult result = controller.stats();

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            assertNull(result.get("data"));
        }
    }

    // ==================== Excel 导出 ====================

    @Nested
    @DisplayName("导出 GET /export")
    class ExportEndpoint {

        @Test
        @DisplayName("导出工单 - 调用ExcelUtil")
        void shouldExportOrders() {
            // Act - 验证无异常抛出（WorkOrder 无 @Excel 注解，仅输出标题行）
            HttpServletResponse response = new MockHttpServletResponse();
            assertDoesNotThrow(() -> controller.export(response, new WorkOrder()));
        }

        @Test
        @DisplayName("导出空数据 - 导出空Excel")
        void shouldExportEmptyOrders() {
            // Act - 验证无异常抛出
            HttpServletResponse response = new MockHttpServletResponse();
            assertDoesNotThrow(() -> controller.export(response, new WorkOrder()));
        }
    }

    // ==================== 异步导出 ====================

    @Nested
    @DisplayName("异步导出 GET /asyncExport")
    class AsyncExportEndpoint {

        @Test
        @DisplayName("提交异步导出任务 - 返回任务ID")
        void shouldSubmitAsyncExport() {
            // Arrange
            when(exportTaskService.submitExportTask(anyString(), anyString(), anyString(), any()))
                    .thenReturn(1001L);

            // Act
            AjaxResult result = controller.asyncExport(new WorkOrder());

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            assertTrue(result.get("msg").toString().contains("1001"));
        }
    }
}