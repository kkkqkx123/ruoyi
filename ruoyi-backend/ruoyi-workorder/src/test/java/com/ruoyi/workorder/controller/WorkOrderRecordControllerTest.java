package com.ruoyi.workorder.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.workorder.domain.WorkOrderRecord;
import com.ruoyi.workorder.service.IWorkOrderRecordService;
import com.ruoyi.workorder.service.IWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkOrderRecordController 单元测试
 *
 * 覆盖维修记录 CRUD 以及"新增维修记录同时完成工单"的特殊逻辑
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderRecordControllerTest {

    @Mock
    private IWorkOrderRecordService workOrderRecordService;

    @Mock
    private IWorkOrderService workOrderService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private WorkOrderRecordController controller;

    private WorkOrderRecord sampleRecord;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("pageNum", "1");
        request.setParameter("pageSize", "10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        sampleRecord = new WorkOrderRecord();
        sampleRecord.setRecordId(1L);
        sampleRecord.setOrderId(1L);
        sampleRecord.setRepairSolution("更换零件");
        sampleRecord.setImageUrls("[\"http://example.com/img.jpg\"]");
    }

    // ==================== 列表查询 ====================

    @Nested
    @DisplayName("列表查询 /list")
    class ListEndpoint {

        @Test
        @DisplayName("查询维修记录列表 - 按工单筛选")
        void shouldListRecordsByOrder() {
            // Arrange
            WorkOrderRecord query = new WorkOrderRecord();
            query.setOrderId(1L);
            List<WorkOrderRecord> mockList = Arrays.asList(sampleRecord, new WorkOrderRecord());

            when(workOrderRecordService.selectWorkOrderRecordList(any())).thenReturn(mockList);

            // Act
            TableDataInfo result = controller.list(query);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getRows().size());
            verify(workOrderRecordService, times(1)).selectWorkOrderRecordList(query);
        }

        @Test
        @DisplayName("查询空记录列表")
        void shouldReturnEmptyList() {
            // Arrange
            when(workOrderRecordService.selectWorkOrderRecordList(any())).thenReturn(Collections.emptyList());

            // Act
            TableDataInfo result = controller.list(new WorkOrderRecord());

            // Assert
            assertNotNull(result);
            assertTrue(result.getRows().isEmpty());
            assertEquals(0L, result.getTotal());
        }
    }

    // ==================== 新增（完成工单） ====================

    @Nested
    @DisplayName("新增维修记录 POST /")
    class AddEndpoint {

        @Test
        @DisplayName("新增维修记录 - 自动设置维修人和维修时间，调用完成工单逻辑")
        void shouldAddRecordAndCompleteOrder() {
            // Arrange
            LoginUser loginUser = new LoginUser();
            SysUser sysUser = new SysUser();
            sysUser.setUserName("lisi");
            loginUser.setUser(sysUser);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            doNothing().when(workOrderService).completeWorkOrder(any(WorkOrderRecord.class));

            // Act
            AjaxResult result = controller.add(sampleRecord);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            assertEquals("lisi", sampleRecord.getRepairBy());
            assertNotNull(sampleRecord.getRepairTime());
            verify(workOrderService, times(1)).completeWorkOrder(sampleRecord);
        }
    }

    // ==================== 详情查询 ====================

    @Nested
    @DisplayName("详情查询 /{recordId}")
    class GetInfoEndpoint {

        @Test
        @DisplayName("获取记录详情")
        void shouldGetRecordInfo() {
            // Arrange
            when(workOrderRecordService.selectWorkOrderRecordById(1L)).thenReturn(sampleRecord);

            // Act
            AjaxResult result = controller.getInfo(1L);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            assertNotNull(result.get("data"));
        }
    }

    // ==================== 修改 ====================

    @Nested
    @DisplayName("修改维修记录 PUT /")
    class EditEndpoint {

        @Test
        @DisplayName("修改记录")
        void shouldEditRecord() {
            // Arrange
            when(workOrderRecordService.updateWorkOrderRecord(any())).thenReturn(1);

            // Act
            AjaxResult result = controller.edit(sampleRecord);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
        }
    }

    // ==================== 删除 ====================

    @Nested
    @DisplayName("删除维修记录 DELETE /{recordIds}")
    class RemoveEndpoint {

        @Test
        @DisplayName("删除记录")
        void shouldDeleteRecord() {
            // Arrange
            Long[] ids = {1L};
            when(workOrderRecordService.deleteWorkOrderRecordByIds(ids)).thenReturn(1);

            // Act
            AjaxResult result = controller.remove(ids);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
        }
    }
}