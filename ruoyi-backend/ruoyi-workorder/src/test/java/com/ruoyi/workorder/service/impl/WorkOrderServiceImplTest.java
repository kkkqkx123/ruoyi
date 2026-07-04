package com.ruoyi.workorder.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workorder.domain.FaultTopDevice;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.WorkOrderRecord;
import com.ruoyi.workorder.domain.WorkOrderStats;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import com.ruoyi.workorder.mapper.WorkOrderRecordMapper;
import com.ruoyi.system.service.ISysNoticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkOrderServiceImpl 单元测试
 *
 * 覆盖核心业务逻辑：
 * - 工单创建（雪花ID、状态初始化、紧急通知）
 * - 批量分配
 * - 完成工单（状态校验、必填校验、记录保存）
 * - 归档工单（状态校验）
 * - 统计查询
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceImplTest {

    @Mock
    private WorkOrderMapper workOrderMapper;

    @Mock
    private WorkOrderRecordMapper workOrderRecordMapper;

    @Mock
    private ISysNoticeService noticeService;

    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    private WorkOrder validWorkOrder;

    @BeforeEach
    void setUp() {
        validWorkOrder = new WorkOrder();
        validWorkOrder.setDeviceId(1L);
        validWorkOrder.setReporterBy("zhangsan");
        validWorkOrder.setFaultDesc("设备无法启动");
        validWorkOrder.setFaultType("0");
        validWorkOrder.setUrgencyLevel("1");
    }

    // ==================== 工单创建 ====================

    @Nested
    @DisplayName("工单创建 insertWorkOrder")
    class InsertWorkOrder {

        @Test
        @DisplayName("正常创建工单 - 生成雪花ID编号，状态初始化为未派单")
        void shouldCreateOrderSuccessfully() {
            // Arrange
            when(workOrderMapper.insertWorkOrder(any(WorkOrder.class))).thenReturn(1);

            // Act
            int result = workOrderService.insertWorkOrder(validWorkOrder);

            // Assert
            assertEquals(1, result);
            assertNotNull(validWorkOrder.getOrderNo());
            assertTrue(validWorkOrder.getOrderNo().startsWith("WO" + DateUtil.format(new Date(), "yyyyMMdd")));
            assertEquals("0", validWorkOrder.getOrderStatus());
            verify(workOrderMapper, times(1)).insertWorkOrder(validWorkOrder);
            verify(noticeService, never()).insertNotice(any());
        }

        @Test
        @DisplayName("紧急工单 - 自动推送通知")
        void shouldPushNoticeForUrgentOrder() {
            // Arrange
            validWorkOrder.setUrgencyLevel("2"); // 紧急
            when(workOrderMapper.insertWorkOrder(any(WorkOrder.class))).thenReturn(1);
            when(noticeService.insertNotice(any(SysNotice.class))).thenReturn(1);

            // Act
            workOrderService.insertWorkOrder(validWorkOrder);

            // Assert
            ArgumentCaptor<SysNotice> noticeCaptor = ArgumentCaptor.forClass(SysNotice.class);
            verify(noticeService, times(1)).insertNotice(noticeCaptor.capture());

            SysNotice pushedNotice = noticeCaptor.getValue();
            assertTrue(pushedNotice.getNoticeTitle().contains(validWorkOrder.getOrderNo()));
            assertEquals("1", pushedNotice.getNoticeType());
            assertTrue(pushedNotice.getNoticeContent().contains(validWorkOrder.getFaultDesc()));
        }

        @Test
        @DisplayName("特急工单 - 自动推送通知")
        void shouldPushNoticeForUrgentLevel3Order() {
            // Arrange
            validWorkOrder.setUrgencyLevel("3"); // 特急
            when(workOrderMapper.insertWorkOrder(any(WorkOrder.class))).thenReturn(1);
            when(noticeService.insertNotice(any(SysNotice.class))).thenReturn(1);

            // Act
            workOrderService.insertWorkOrder(validWorkOrder);

            // Assert
            verify(noticeService, times(1)).insertNotice(any(SysNotice.class));
        }

        @Test
        @DisplayName("普通工单 - 不推送通知")
        void shouldNotPushNoticeForNormalOrder() {
            // Arrange
            validWorkOrder.setUrgencyLevel("1"); // 普通
            when(workOrderMapper.insertWorkOrder(any(WorkOrder.class))).thenReturn(1);

            // Act
            workOrderService.insertWorkOrder(validWorkOrder);

            // Assert
            verify(noticeService, never()).insertNotice(any());
        }
    }

    // ==================== 批量分配 ====================

    @Nested
    @DisplayName("批量分配 batchAssign")
    class BatchAssign {

        @Test
        @DisplayName("批量分配工单 - 更新状态为已派单、设置维修员和派单时间")
        void shouldBatchAssignSuccessfully() {
            // Arrange
            Long[] orderIds = {1L, 2L, 3L};
            String assignTo = "lisi";
            when(workOrderMapper.selectWorkOrderById(anyLong())).thenReturn(new WorkOrder());
            when(workOrderMapper.updateWorkOrder(any(WorkOrder.class))).thenReturn(1);

            // Act
            int count = workOrderService.batchAssign(orderIds, assignTo);

            // Assert
            assertEquals(3, count);
            ArgumentCaptor<WorkOrder> captor = ArgumentCaptor.forClass(WorkOrder.class);
            verify(workOrderMapper, times(3)).updateWorkOrder(captor.capture());

            List<WorkOrder> capturedOrders = captor.getAllValues();
            for (WorkOrder order : capturedOrders) {
                assertEquals("1", order.getOrderStatus());
                assertEquals(assignTo, order.getAssignTo());
                assertNotNull(order.getAssignTime());
            }
        }

        @Test
        @DisplayName("批量分配空数组 - 不执行任何更新")
        void shouldHandleEmptyOrderIds() {
            // Arrange
            Long[] orderIds = {};
            String assignTo = "lisi";

            // Act
            int count = workOrderService.batchAssign(orderIds, assignTo);

            // Assert
            assertEquals(0, count);
            verify(workOrderMapper, never()).updateWorkOrder(any());
        }
    }

    // ==================== 完成工单 ====================

    @Nested
    @DisplayName("完成工单 completeWorkOrder")
    class CompleteWorkOrder {

        private WorkOrderRecord validRecord;
        private WorkOrder existingOrder;

        @BeforeEach
        void setUp() {
            existingOrder = new WorkOrder();
            existingOrder.setOrderId(1L);
            existingOrder.setOrderStatus("2"); // 维修中

            validRecord = new WorkOrderRecord();
            validRecord.setOrderId(1L);
            validRecord.setRepairSolution("更换电源模块");
            validRecord.setImageUrls("[\"http://example.com/img1.jpg\"]");
        }

        @Test
        @DisplayName("正常完成工单 - 校验通过，保存记录并更新状态")
        void shouldCompleteWorkOrderSuccessfully() {
            // Arrange
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(existingOrder);
            when(workOrderRecordMapper.insertWorkOrderRecord(any(WorkOrderRecord.class))).thenReturn(1);
            when(workOrderMapper.updateWorkOrder(any(WorkOrder.class))).thenReturn(1);

            // Act
            workOrderService.completeWorkOrder(validRecord);

            // Assert
            assertEquals("3", existingOrder.getOrderStatus());
            assertNotNull(existingOrder.getFinishTime());
            verify(workOrderRecordMapper, times(1)).insertWorkOrderRecord(validRecord);
            verify(workOrderMapper, times(1)).updateWorkOrder(existingOrder);
        }

        @Test
        @DisplayName("工单不存在 - 抛出异常")
        void shouldThrowWhenOrderNotFound() {
            // Arrange
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(null);

            // Act & Assert
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> workOrderService.completeWorkOrder(validRecord));
            assertEquals("工单不存在", ex.getMessage());
            verify(workOrderRecordMapper, never()).insertWorkOrderRecord(any());
            verify(workOrderMapper, never()).updateWorkOrder(any());
        }

        @Test
        @DisplayName("工单状态不是维修中 - 抛出异常")
        void shouldThrowWhenStatusNotInProgress() {
            // Arrange
            existingOrder.setOrderStatus("0"); // 未派单
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(existingOrder);

            // Act & Assert
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> workOrderService.completeWorkOrder(validRecord));
            assertEquals("仅维修中的工单可以完成", ex.getMessage());
        }

        @Test
        @DisplayName("维修方案为空 - 抛出异常")
        void shouldThrowWhenRepairSolutionEmpty() {
            // Arrange
            validRecord.setRepairSolution("");
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(existingOrder);

            // Act & Assert
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> workOrderService.completeWorkOrder(validRecord));
            assertEquals("请填写维修方案", ex.getMessage());
        }

        @Test
        @DisplayName("维修图片为空 - 抛出异常")
        void shouldThrowWhenImageUrlsEmpty() {
            // Arrange
            validRecord.setImageUrls("");
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(existingOrder);

            // Act & Assert
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> workOrderService.completeWorkOrder(validRecord));
            assertEquals("请上传至少一张维修图片", ex.getMessage());
        }

        @Test
        @DisplayName("维修方案为null - 抛出异常")
        void shouldThrowWhenRepairSolutionNull() {
            // Arrange
            validRecord.setRepairSolution(null);
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(existingOrder);

            // Act & Assert
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> workOrderService.completeWorkOrder(validRecord));
            assertEquals("请填写维修方案", ex.getMessage());
        }

        @Test
        @DisplayName("维修图片为null - 抛出异常")
        void shouldThrowWhenImageUrlsNull() {
            // Arrange
            validRecord.setImageUrls(null);
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(existingOrder);

            // Act & Assert
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> workOrderService.completeWorkOrder(validRecord));
            assertEquals("请上传至少一张维修图片", ex.getMessage());
        }
    }

    // ==================== 归档工单 ====================

    @Nested
    @DisplayName("归档工单 archiveWorkOrder")
    class ArchiveWorkOrder {

        private WorkOrder completedOrder;

        @BeforeEach
        void setUp() {
            completedOrder = new WorkOrder();
            completedOrder.setOrderId(1L);
            completedOrder.setOrderStatus("3"); // 已完成
        }

        @Test
        @DisplayName("正常归档工单 - 状态更新为已归档")
        void shouldArchiveSuccessfully() {
            // Arrange
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(completedOrder);
            when(workOrderMapper.updateWorkOrder(any(WorkOrder.class))).thenReturn(1);

            // Act
            workOrderService.archiveWorkOrder(1L, "admin", "验收通过");

            // Assert
            assertEquals("4", completedOrder.getOrderStatus());
            assertNotNull(completedOrder.getArchiveTime());
            assertEquals("admin", completedOrder.getArchiveBy());
            assertEquals("验收通过", completedOrder.getArchiveRemark());
            verify(workOrderMapper, times(1)).updateWorkOrder(completedOrder);
        }

        @Test
        @DisplayName("工单不存在 - 抛出异常")
        void shouldThrowWhenOrderNotFound() {
            // Arrange
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(null);

            // Act & Assert
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> workOrderService.archiveWorkOrder(1L, "admin", "验收通过"));
            assertEquals("工单不存在", ex.getMessage());
            verify(workOrderMapper, never()).updateWorkOrder(any());
        }

        @Test
        @DisplayName("工单状态不是已完成 - 抛出异常")
        void shouldThrowWhenStatusNotCompleted() {
            // Arrange
            completedOrder.setOrderStatus("2"); // 维修中
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(completedOrder);

            // Act & Assert
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> workOrderService.archiveWorkOrder(1L, "admin", "验收通过"));
            assertEquals("仅已完成的工单可以归档", ex.getMessage());
        }

        @Test
        @DisplayName("归档备注为空 - 归档成功（备注可为空）")
        void shouldArchiveWithEmptyRemark() {
            // Arrange
            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(completedOrder);
            when(workOrderMapper.updateWorkOrder(any(WorkOrder.class))).thenReturn(1);

            // Act
            workOrderService.archiveWorkOrder(1L, "admin", "");

            // Assert
            assertEquals("4", completedOrder.getOrderStatus());
            assertEquals("", completedOrder.getArchiveRemark());
        }
    }

    // ==================== 查询方法 ====================

    @Nested
    @DisplayName("查询方法")
    class QueryMethods {

        @Test
        @DisplayName("selectWorkOrderList - 委托给 Mapper")
        void shouldDelegateSelectList() {
            // Arrange
            WorkOrder query = new WorkOrder();
            List<WorkOrder> mockList = Arrays.asList(new WorkOrder(), new WorkOrder());
            when(workOrderMapper.selectWorkOrderList(query)).thenReturn(mockList);

            // Act
            List<WorkOrder> result = workOrderService.selectWorkOrderList(query);

            // Assert
            assertEquals(2, result.size());
            verify(workOrderMapper, times(1)).selectWorkOrderList(query);
        }

        @Test
        @DisplayName("selectWorkOrderStats - 委托给 Mapper")
        void shouldDelegateSelectStats() {
            // Arrange
            WorkOrderStats mockStats = new WorkOrderStats();
            mockStats.setTotalCount(100L);
            when(workOrderMapper.selectWorkOrderStats()).thenReturn(mockStats);

            // Act
            WorkOrderStats result = workOrderService.selectWorkOrderStats();

            // Assert
            assertEquals(100L, result.getTotalCount());
            verify(workOrderMapper, times(1)).selectWorkOrderStats();
        }

        @Test
        @DisplayName("selectFaultTopDevices - 委托给 Mapper")
        void shouldDelegateSelectTopDevices() {
            // Arrange
            FaultTopDevice device = new FaultTopDevice();
            device.setDeviceId(1L);
            device.setDeviceName("测试设备");
            device.setFaultCount(5);
            when(workOrderMapper.selectFaultTopDevices()).thenReturn(Arrays.asList(device));

            // Act
            List<FaultTopDevice> result = workOrderService.selectFaultTopDevices();

            // Assert
            assertEquals(1, result.size());
            assertEquals("测试设备", result.get(0).getDeviceName());
            verify(workOrderMapper, times(1)).selectFaultTopDevices();
        }
    }

    // ==================== 状态流转完整性验证 ====================

    @Nested
    @DisplayName("状态流转完整性验证")
    class StatusTransitionValidation {

        @Test
        @DisplayName("完整状态流转：未派单→已派单→维修中→已完成→已归档")
        void shouldFlowThroughAllStatus() {
            // 此测试验证整体业务逻辑的完整性

            // Step 1: 创建工单 → 未派单(0)
            WorkOrder order = new WorkOrder();
            order.setDeviceId(1L);
            order.setReporterBy("zhangsan");
            order.setFaultDesc("设备故障");
            order.setUrgencyLevel("1");
            when(workOrderMapper.insertWorkOrder(any(WorkOrder.class))).thenReturn(1);
            workOrderService.insertWorkOrder(order);
            assertEquals("0", order.getOrderStatus());

            // Step 2: 批量分配 → 已派单(1)
            Long[] orderIds = {order.getOrderId()};
            when(workOrderMapper.selectWorkOrderById(order.getOrderId())).thenReturn(order);
            when(workOrderMapper.updateWorkOrder(any(WorkOrder.class))).thenReturn(1);
            workOrderService.batchAssign(orderIds, "lisi");
            assertEquals("1", order.getOrderStatus());

            // Step 3: 维修员接单 - 这里更新状态到"2"由前端调用 updateWorkOrder
            // 模拟维修员接单
            order.setOrderStatus("2");
            when(workOrderMapper.updateWorkOrder(order)).thenReturn(1);
            workOrderMapper.updateWorkOrder(order);
            assertEquals("2", order.getOrderStatus());

            // Step 4: 完成工单 → 已完成(3)
            WorkOrderRecord record = new WorkOrderRecord();
            record.setOrderId(order.getOrderId());
            record.setRepairSolution("已修复");
            record.setImageUrls("[\"img.jpg\"]");
            when(workOrderMapper.selectWorkOrderById(order.getOrderId())).thenReturn(order);
            when(workOrderRecordMapper.insertWorkOrderRecord(any())).thenReturn(1);
            when(workOrderMapper.updateWorkOrder(order)).thenReturn(1);
            workOrderService.completeWorkOrder(record);
            assertEquals("3", order.getOrderStatus());

            // Step 5: 归档 → 已归档(4)
            when(workOrderMapper.selectWorkOrderById(order.getOrderId())).thenReturn(order);
            when(workOrderMapper.updateWorkOrder(order)).thenReturn(1);
            workOrderService.archiveWorkOrder(order.getOrderId(), "admin", "归档");
            assertEquals("4", order.getOrderStatus());
        }

        @Test
        @DisplayName("状态不可逆：已完成工单不能再完成")
        void shouldNotAllowRedundantComplete() {
            // Arrange
            WorkOrder order = new WorkOrder();
            order.setOrderId(1L);
            order.setOrderStatus("3"); // 已完成

            WorkOrderRecord record = new WorkOrderRecord();
            record.setOrderId(1L);
            record.setRepairSolution("方案");
            record.setImageUrls("[\"img.jpg\"]");

            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(order);

            // Act & Assert
            assertThrows(ServiceException.class,
                    () -> workOrderService.completeWorkOrder(record));
        }

        @Test
        @DisplayName("状态不可逆：已归档工单不能再完成或归档")
        void shouldNotAllowArchiveAgain() {
            // Arrange
            WorkOrder order = new WorkOrder();
            order.setOrderId(1L);
            order.setOrderStatus("4"); // 已归档

            when(workOrderMapper.selectWorkOrderById(1L)).thenReturn(order);

            // Act & Assert
            assertThrows(ServiceException.class,
                    () -> workOrderService.archiveWorkOrder(1L, "admin", ""));
        }
    }
}