package com.ruoyi.workorder.service.impl;

import com.ruoyi.workorder.domain.WorkOrderRecord;
import com.ruoyi.workorder.mapper.WorkOrderRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * WorkOrderRecordServiceImpl 单元测试
 * <p>
 * 覆盖范围：工单维修记录的 CRUD 操作
 *
 * @author ruoyi
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderRecordServiceImplTest {

    @Mock
    private WorkOrderRecordMapper workOrderRecordMapper;

    @InjectMocks
    private WorkOrderRecordServiceImpl workOrderRecordService;

    private WorkOrderRecord validRecord;

    @BeforeEach
    void setUp() {
        validRecord = new WorkOrderRecord();
        validRecord.setRecordId(1L);
        validRecord.setOrderId(1L);
        validRecord.setRepairBy("zhangsan");
        validRecord.setRepairTime(new Date());
        validRecord.setRepairSolution("更换电源模块");
        validRecord.setPartConsumption("电源模块 x1");
        validRecord.setImageUrls("[\"http://example.com/img1.jpg\"]");
        validRecord.setRepairResult("0");
    }

    @Nested
    @DisplayName("查询维修记录列表 selectWorkOrderRecordList")
    class SelectWorkOrderRecordList {

        @Test
        @DisplayName("按工单ID查询 - 返回记录列表")
        void shouldReturnRecordsByOrderId() {
            // Arrange
            WorkOrderRecord query = new WorkOrderRecord();
            query.setOrderId(1L);
            List<WorkOrderRecord> mockList = Arrays.asList(validRecord);
            when(workOrderRecordMapper.selectWorkOrderRecordList(query)).thenReturn(mockList);

            // Act
            List<WorkOrderRecord> result = workOrderRecordService.selectWorkOrderRecordList(query);

            // Assert
            assertEquals(1, result.size());
            assertEquals("更换电源模块", result.get(0).getRepairSolution());
            verify(workOrderRecordMapper, times(1)).selectWorkOrderRecordList(query);
        }

        @Test
        @DisplayName("无记录 - 返回空列表")
        void shouldReturnEmptyListWhenNoRecords() {
            // Arrange
            when(workOrderRecordMapper.selectWorkOrderRecordList(any())).thenReturn(List.of());

            // Act
            List<WorkOrderRecord> result = workOrderRecordService.selectWorkOrderRecordList(new WorkOrderRecord());

            // Assert
            assertTrue(result.isEmpty());
            verify(workOrderRecordMapper, times(1)).selectWorkOrderRecordList(any());
        }
    }

    @Nested
    @DisplayName("按ID查询 selectWorkOrderRecordById")
    class SelectWorkOrderRecordById {

        @Test
        @DisplayName("记录存在 - 返回记录")
        void shouldReturnRecordWhenFound() {
            // Arrange
            when(workOrderRecordMapper.selectWorkOrderRecordById(1L)).thenReturn(validRecord);

            // Act
            WorkOrderRecord result = workOrderRecordService.selectWorkOrderRecordById(1L);

            // Assert
            assertNotNull(result);
            assertEquals("zhangsan", result.getRepairBy());
            assertEquals("更换电源模块", result.getRepairSolution());
            verify(workOrderRecordMapper, times(1)).selectWorkOrderRecordById(1L);
        }

        @Test
        @DisplayName("记录不存在 - 返回 null")
        void shouldReturnNullWhenNotFound() {
            // Arrange
            when(workOrderRecordMapper.selectWorkOrderRecordById(999L)).thenReturn(null);

            // Act
            WorkOrderRecord result = workOrderRecordService.selectWorkOrderRecordById(999L);

            // Assert
            assertNull(result);
            verify(workOrderRecordMapper, times(1)).selectWorkOrderRecordById(999L);
        }
    }

    @Nested
    @DisplayName("新增记录 insertWorkOrderRecord")
    class InsertWorkOrderRecord {

        @Test
        @DisplayName("正常新增 - 返回影响行数")
        void shouldInsertSuccessfully() {
            // Arrange
            when(workOrderRecordMapper.insertWorkOrderRecord(validRecord)).thenReturn(1);

            // Act
            int result = workOrderRecordService.insertWorkOrderRecord(validRecord);

            // Assert
            assertEquals(1, result);
            verify(workOrderRecordMapper, times(1)).insertWorkOrderRecord(validRecord);
        }

        @Test
        @DisplayName("新增失败 - 返回 0")
        void shouldReturnZeroWhenInsertFails() {
            // Arrange
            when(workOrderRecordMapper.insertWorkOrderRecord(any())).thenReturn(0);

            // Act
            int result = workOrderRecordService.insertWorkOrderRecord(new WorkOrderRecord());

            // Assert
            assertEquals(0, result);
            verify(workOrderRecordMapper, times(1)).insertWorkOrderRecord(any());
        }
    }

    @Nested
    @DisplayName("修改记录 updateWorkOrderRecord")
    class UpdateWorkOrderRecord {

        @Test
        @DisplayName("正常修改 - 返回影响行数")
        void shouldUpdateSuccessfully() {
            // Arrange
            validRecord.setRepairSolution("更换主板");
            when(workOrderRecordMapper.updateWorkOrderRecord(validRecord)).thenReturn(1);

            // Act
            int result = workOrderRecordService.updateWorkOrderRecord(validRecord);

            // Assert
            assertEquals(1, result);
            verify(workOrderRecordMapper, times(1)).updateWorkOrderRecord(validRecord);
        }
    }

    @Nested
    @DisplayName("删除记录 deleteWorkOrderRecordByIds")
    class DeleteWorkOrderRecordByIds {

        @Test
        @DisplayName("批量删除单个记录 - 返回影响行数")
        void shouldDeleteSingleRecord() {
            // Arrange
            Long[] ids = {1L};
            when(workOrderRecordMapper.deleteWorkOrderRecordByIds(ids)).thenReturn(1);

            // Act
            int result = workOrderRecordService.deleteWorkOrderRecordByIds(ids);

            // Assert
            assertEquals(1, result);
            verify(workOrderRecordMapper, times(1)).deleteWorkOrderRecordByIds(ids);
        }

        @Test
        @DisplayName("批量删除多个记录 - 返回影响行数")
        void shouldDeleteMultipleRecords() {
            // Arrange
            Long[] ids = {1L, 2L};
            when(workOrderRecordMapper.deleteWorkOrderRecordByIds(ids)).thenReturn(2);

            // Act
            int result = workOrderRecordService.deleteWorkOrderRecordByIds(ids);

            // Assert
            assertEquals(2, result);
            verify(workOrderRecordMapper, times(1)).deleteWorkOrderRecordByIds(ids);
        }
    }
}