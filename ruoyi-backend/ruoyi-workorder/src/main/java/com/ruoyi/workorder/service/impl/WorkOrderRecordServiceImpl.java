package com.ruoyi.workorder.service.impl;

import com.ruoyi.workorder.domain.WorkOrderRecord;
import com.ruoyi.workorder.mapper.WorkOrderRecordMapper;
import com.ruoyi.workorder.service.IWorkOrderRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工单维修记录Service实现
 */
@Service
public class WorkOrderRecordServiceImpl implements IWorkOrderRecordService {

    @Autowired
    private WorkOrderRecordMapper workOrderRecordMapper;

    @Override
    public List<WorkOrderRecord> selectWorkOrderRecordList(WorkOrderRecord workOrderRecord) {
        return workOrderRecordMapper.selectWorkOrderRecordList(workOrderRecord);
    }

    @Override
    public WorkOrderRecord selectWorkOrderRecordById(Long recordId) {
        return workOrderRecordMapper.selectWorkOrderRecordById(recordId);
    }

    @Override
    public int insertWorkOrderRecord(WorkOrderRecord workOrderRecord) {
        return workOrderRecordMapper.insertWorkOrderRecord(workOrderRecord);
    }

    @Override
    public int updateWorkOrderRecord(WorkOrderRecord workOrderRecord) {
        return workOrderRecordMapper.updateWorkOrderRecord(workOrderRecord);
    }

    @Override
    public int deleteWorkOrderRecordByIds(Long[] recordIds) {
        return workOrderRecordMapper.deleteWorkOrderRecordByIds(recordIds);
    }
}