package com.ruoyi.workorder.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.workorder.domain.DeviceInfo;
import com.ruoyi.workorder.service.IDeviceInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DeviceInfoController 单元测试
 *
 * 覆盖设备管理完整 CRUD
 */
@ExtendWith(MockitoExtension.class)
class DeviceInfoControllerTest {

    @Mock
    private IDeviceInfoService deviceInfoService;

    @InjectMocks
    private DeviceInfoController controller;

    private DeviceInfo sampleDevice;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("pageNum", "1");
        request.setParameter("pageSize", "10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        sampleDevice = new DeviceInfo();
        sampleDevice.setDeviceId(1L);
        sampleDevice.setDeviceCode("DEV-001");
        sampleDevice.setDeviceName("测试设备");
        sampleDevice.setDeviceModel("T-100");
        sampleDevice.setLocation("A栋3楼");
        sampleDevice.setStatus("0");
        sampleDevice.setResponsibleBy("zhangsan");
        sampleDevice.setPrice(new BigDecimal("9999.99"));
    }

    // ==================== 列表查询 ====================

    @Nested
    @DisplayName("列表查询 /list")
    class ListEndpoint {

        @Test
        @DisplayName("查询设备列表 - 返回分页数据")
        void shouldReturnDeviceList() {
            // Arrange
            when(deviceInfoService.selectDeviceInfoList(any())).thenReturn(Arrays.asList(sampleDevice));

            // Act
            TableDataInfo result = controller.list(new DeviceInfo());

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getRows().size());
            verify(deviceInfoService, times(1)).selectDeviceInfoList(any());
        }

        @Test
        @DisplayName("查询空设备列表")
        void shouldReturnEmptyList() {
            // Arrange
            when(deviceInfoService.selectDeviceInfoList(any())).thenReturn(Collections.emptyList());

            // Act
            TableDataInfo result = controller.list(new DeviceInfo());

            // Assert
            assertNotNull(result);
            assertTrue(result.getRows().isEmpty());
            assertEquals(0L, result.getTotal());
        }
    }

    // ==================== 详情查询 ====================

    @Nested
    @DisplayName("详情查询 /{deviceId}")
    class GetInfoEndpoint {

        @Test
        @DisplayName("获取设备详情")
        void shouldGetDeviceInfo() {
            // Arrange
            when(deviceInfoService.selectDeviceInfoById(1L)).thenReturn(sampleDevice);

            // Act
            AjaxResult result = controller.getInfo(1L);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            assertNotNull(result.get("data"));
            DeviceInfo data = (DeviceInfo) result.get("data");
            assertEquals("DEV-001", data.getDeviceCode());
        }

        @Test
        @DisplayName("获取不存在的设备")
        void shouldGetNullForNonExistent() {
            // Arrange
            when(deviceInfoService.selectDeviceInfoById(999L)).thenReturn(null);

            // Act
            AjaxResult result = controller.getInfo(999L);

            // Assert
            assertNull(result.get("data"));
        }
    }

    // ==================== 新增 ====================

    @Nested
    @DisplayName("新增设备 POST /")
    class AddEndpoint {

        @Test
        @DisplayName("新增设备成功")
        void shouldAddDeviceSuccessfully() {
            // Arrange
            when(deviceInfoService.insertDeviceInfo(any())).thenReturn(1);

            // Act
            AjaxResult result = controller.add(sampleDevice);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            verify(deviceInfoService, times(1)).insertDeviceInfo(sampleDevice);
        }

        @Test
        @DisplayName("新增设备失败 - 返回错误")
        void shouldReturnErrorWhenInsertFails() {
            // Arrange
            when(deviceInfoService.insertDeviceInfo(any())).thenReturn(0);

            // Act
            AjaxResult result = controller.add(sampleDevice);

            // Assert
            assertEquals(HttpStatus.ERROR, result.get("code"));
        }
    }

    // ==================== 修改 ====================

    @Nested
    @DisplayName("修改设备 PUT /")
    class EditEndpoint {

        @Test
        @DisplayName("修改设备成功")
        void shouldEditDeviceSuccessfully() {
            // Arrange
            when(deviceInfoService.updateDeviceInfo(any())).thenReturn(1);

            // Act
            AjaxResult result = controller.edit(sampleDevice);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            verify(deviceInfoService, times(1)).updateDeviceInfo(sampleDevice);
        }
    }

    // ==================== 删除 ====================

    @Nested
    @DisplayName("删除设备 DELETE /{deviceIds}")
    class RemoveEndpoint {

        @Test
        @DisplayName("删除单个设备")
        void shouldDeleteSingleDevice() {
            // Arrange
            Long[] ids = {1L};
            when(deviceInfoService.deleteDeviceInfoByIds(ids)).thenReturn(1);

            // Act
            AjaxResult result = controller.remove(ids);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
            verify(deviceInfoService, times(1)).deleteDeviceInfoByIds(ids);
        }

        @Test
        @DisplayName("批量删除设备")
        void shouldDeleteMultipleDevices() {
            // Arrange
            Long[] ids = {1L, 2L};
            when(deviceInfoService.deleteDeviceInfoByIds(ids)).thenReturn(2);

            // Act
            AjaxResult result = controller.remove(ids);

            // Assert
            assertEquals(HttpStatus.SUCCESS, result.get("code"));
        }
    }
}