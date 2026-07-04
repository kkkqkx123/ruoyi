package com.ruoyi.workorder.mapper;

import com.ruoyi.workorder.domain.WorkOrderRecord;

import java.util.List;

/**
 * 工单记录Mapper接口
 *
 * @author ruoyi
 */
public interface WorkOrderRecordMapper {

    /**
     * 查询维修记录列表
     */
    List<WorkOrderRecord> selectWorkOrderRecordList(WorkOrderRecord workOrderRecord);

    /**
     * 根据ID查询维修记录
     */
    WorkOrderRecord selectWorkOrderRecordById(Long recordId);

    /**
     * 根据工单ID查询维修记录
     */
    List<WorkOrderRecord> selectWorkOrderRecordByOrderId(Long orderId);

    /**
     * 新增维修记录
     */
    int insertWorkOrderRecord(WorkOrderRecord workOrderRecord);

    /**
     * 修改维修记录
     */
    int updateWorkOrderRecord(WorkOrderRecord workOrderRecord);

    /**
     * 删除维修记录
     */
    int deleteWorkOrderRecordById(Long recordId);

    /**
     * 批量删除维修记录
     */
    int deleteWorkOrderRecordByIds(Long[] recordIds);

    /**
     * 根据工单ID删除维修记录
     */
    int deleteWorkOrderRecordByOrderId(Long orderId);
}