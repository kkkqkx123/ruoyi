package com.ruoyi.workorder.service.impl;

import com.ruoyi.workorder.domain.DeviceInfo;
import com.ruoyi.workorder.mapper.DeviceInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * DeviceInfoServiceImpl 单元测试
 * <p>
 * 覆盖范围：CRUD 操作、@RedisCache 注解交互（通过 Mock 验证 Mapper 委托）
 *
 * @author ruoyi
 */
@ExtendWith(MockitoExtension.class)
class DeviceInfoServiceImplTest {

    @Mock
    private DeviceInfoMapper deviceInfoMapper;

    @InjectMocks
    private DeviceInfoServiceImpl deviceInfoService;

    private DeviceInfo validDevice;

    @BeforeEach
    void setUp() {
        validDevice = new DeviceInfo();
        validDevice.setDeviceId(1L);
        validDevice.setDeviceCode("DEV-001");
        validDevice.setDeviceName("服务器A");
        validDevice.setDeviceModel("PowerEdge R740");
        validDevice.setLocation("机房A-01");
        validDevice.setStatus("0");
        validDevice.setPrice(new BigDecimal("50000.00"));
        validDevice.setResponsibleBy("admin");
        validDevice.setPurchaseTime(new Date());
    }

    @Nested
    @DisplayName("查询方法 selectDeviceInfoList")
    class SelectDeviceInfoList {

        @Test
        @DisplayName("无条件查询 - 返回全部设备列表")
        void shouldReturnAllDevicesWhenQueryEmpty() {
            // Arrange
            DeviceInfo query = new DeviceInfo();
            List<DeviceInfo> mockList = Arrays.asList(validDevice, new DeviceInfo());
            when(deviceInfoMapper.selectDeviceInfoList(query)).thenReturn(mockList);

            // Act
            List<DeviceInfo> result = deviceInfoService.selectDeviceInfoList(query);

            // Assert
            assertEquals(2, result.size());
            verify(deviceInfoMapper, times(1)).selectDeviceInfoList(query);
        }

        @Test
        @DisplayName("带设备名称条件查询 - 返回过滤后列表")
        void shouldReturnFilteredListWhenNameProvided() {
            // Arrange
            DeviceInfo query = new DeviceInfo();
            query.setDeviceName("服务器");
            when(deviceInfoMapper.selectDeviceInfoList(query)).thenReturn(Arrays.asList(validDevice));

            // Act
            List<DeviceInfo> result = deviceInfoService.selectDeviceInfoList(query);

            // Assert
            assertEquals(1, result.size());
            assertEquals("服务器A", result.get(0).getDeviceName());
            verify(deviceInfoMapper, times(1)).selectDeviceInfoList(query);
        }

        @Test
        @DisplayName("无数据 - 返回空列表")
        void shouldReturnEmptyListWhenNoData() {
            // Arrange
            when(deviceInfoMapper.selectDeviceInfoList(any())).thenReturn(List.of());

            // Act
            List<DeviceInfo> result = deviceInfoService.selectDeviceInfoList(new DeviceInfo());

            // Assert
            assertTrue(result.isEmpty());
            verify(deviceInfoMapper, times(1)).selectDeviceInfoList(any());
        }
    }

    @Nested
    @DisplayName("按ID查询 selectDeviceInfoById")
    class SelectDeviceInfoById {

        @Test
        @DisplayName("设备存在 - 返回设备信息（走 @RedisCache）")
        void shouldReturnDeviceWhenFound() {
            // Arrange
            when(deviceInfoMapper.selectDeviceInfoById(1L)).thenReturn(validDevice);

            // Act
            DeviceInfo result = deviceInfoService.selectDeviceInfoById(1L);

            // Assert
            assertNotNull(result);
            assertEquals("DEV-001", result.getDeviceCode());
            assertEquals("服务器A", result.getDeviceName());
            verify(deviceInfoMapper, times(1)).selectDeviceInfoById(1L);
        }

        @Test
        @DisplayName("设备不存在 - 返回 null")
        void shouldReturnNullWhenNotFound() {
            // Arrange
            when(deviceInfoMapper.selectDeviceInfoById(999L)).thenReturn(null);

            // Act
            DeviceInfo result = deviceInfoService.selectDeviceInfoById(999L);

            // Assert
            assertNull(result);
            verify(deviceInfoMapper, times(1)).selectDeviceInfoById(999L);
        }
    }

    @Nested
    @DisplayName("新增设备 insertDeviceInfo")
    class InsertDeviceInfo {

        @Test
        @DisplayName("正常新增 - 返回影响行数（触发表决缓存清除）")
        void shouldInsertSuccessfully() {
            // Arrange
            when(deviceInfoMapper.insertDeviceInfo(validDevice)).thenReturn(1);

            // Act
            int result = deviceInfoService.insertDeviceInfo(validDevice);

            // Assert
            assertEquals(1, result);
            verify(deviceInfoMapper, times(1)).insertDeviceInfo(validDevice);
        }

        @Test
        @DisplayName("新增失败 - 返回 0")
        void shouldReturnZeroWhenInsertFails() {
            // Arrange
            when(deviceInfoMapper.insertDeviceInfo(any())).thenReturn(0);

            // Act
            int result = deviceInfoService.insertDeviceInfo(new DeviceInfo());

            // Assert
            assertEquals(0, result);
            verify(deviceInfoMapper, times(1)).insertDeviceInfo(any());
        }
    }

    @Nested
    @DisplayName("修改设备 updateDeviceInfo")
    class UpdateDeviceInfo {

        @Test
        @DisplayName("正常修改 - 返回影响行数（触发表决缓存清除）")
        void shouldUpdateSuccessfully() {
            // Arrange
            validDevice.setDeviceName("服务器A-升级版");
            when(deviceInfoMapper.updateDeviceInfo(validDevice)).thenReturn(1);

            // Act
            int result = deviceInfoService.updateDeviceInfo(validDevice);

            // Assert
            assertEquals(1, result);
            verify(deviceInfoMapper, times(1)).updateDeviceInfo(validDevice);
        }

        @Test
        @DisplayName("修改不存在设备 - 返回 0")
        void shouldReturnZeroWhenUpdatingNonExistent() {
            // Arrange
            DeviceInfo nonExistent = new DeviceInfo();
            nonExistent.setDeviceId(999L);
            when(deviceInfoMapper.updateDeviceInfo(nonExistent)).thenReturn(0);

            // Act
            int result = deviceInfoService.updateDeviceInfo(nonExistent);

            // Assert
            assertEquals(0, result);
            verify(deviceInfoMapper, times(1)).updateDeviceInfo(nonExistent);
        }
    }

    @Nested
    @DisplayName("删除设备 deleteDeviceInfoByIds")
    class DeleteDeviceInfoByIds {

        @Test
        @DisplayName("批量删除单个设备 - 返回影响行数（触发缓存清除）")
        void shouldDeleteSingleDevice() {
            // Arrange
            Long[] ids = {1L};
            when(deviceInfoMapper.deleteDeviceInfoByIds(ids)).thenReturn(1);

            // Act
            int result = deviceInfoService.deleteDeviceInfoByIds(ids);

            // Assert
            assertEquals(1, result);
            verify(deviceInfoMapper, times(1)).deleteDeviceInfoByIds(ids);
        }

        @Test
        @DisplayName("批量删除多个设备 - 返回影响行数")
        void shouldDeleteMultipleDevices() {
            // Arrange
            Long[] ids = {1L, 2L, 3L};
            when(deviceInfoMapper.deleteDeviceInfoByIds(ids)).thenReturn(3);

            // Act
            int result = deviceInfoService.deleteDeviceInfoByIds(ids);

            // Assert
            assertEquals(3, result);
            verify(deviceInfoMapper, times(1)).deleteDeviceInfoByIds(ids);
        }

        @Test
        @DisplayName("删除空数组 - 返回 0")
        void shouldReturnZeroWhenDeletingEmptyArray() {
            // Arrange
            Long[] ids = {};
            when(deviceInfoMapper.deleteDeviceInfoByIds(ids)).thenReturn(0);

            // Act
            int result = deviceInfoService.deleteDeviceInfoByIds(ids);

            // Assert
            assertEquals(0, result);
            verify(deviceInfoMapper, times(1)).deleteDeviceInfoByIds(ids);
        }
    }
}