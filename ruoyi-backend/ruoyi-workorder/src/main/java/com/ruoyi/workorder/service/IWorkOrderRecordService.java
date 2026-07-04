package com.ruoyi.workorder.service;

import com.ruoyi.workorder.domain.WorkOrderRecord;

import java.util.List;

/**
 * 工单记录Service接口
 *
 * @author ruoyi
 */
public interface IWorkOrderRecordService {

    /**
     * 查询维修记录列表
     */
    List<WorkOrderRecord> selectWorkOrderRecordList(WorkOrderRecord workOrderRecord);

    /**
     * 根据ID查询维修记录
     */
    WorkOrderRecord selectWorkOrderRecordById(Long recordId);

    /**
     * 新增维修记录
     */
    int insertWorkOrderRecord(WorkOrderRecord workOrderRecord);

    /**
     * 修改维修记录
     */
    int updateWorkOrderRecord(WorkOrderRecord workOrderRecord);

    /**
     * 批量删除维修记录
     */
    int deleteWorkOrderRecordByIds(Long[] recordIds);
}