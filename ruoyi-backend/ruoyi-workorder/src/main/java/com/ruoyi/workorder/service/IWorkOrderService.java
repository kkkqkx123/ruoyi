package com.ruoyi.workorder.service;

import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.WorkOrderRecord;
import com.ruoyi.workorder.domain.WorkOrderStats;
import com.ruoyi.workorder.domain.FaultTopDevice;

import java.util.List;

/**
 * 工单Service接口
 *
 * @author ruoyi
 */
public interface IWorkOrderService {

    /**
     * 多表联查工单列表
     */
    List<WorkOrder> selectWorkOrderList(WorkOrder workOrder);

    /**
     * 根据ID查询工单
     */
    WorkOrder selectWorkOrderById(Long orderId);

    /**
     * 新增工单
     */
    int insertWorkOrder(WorkOrder workOrder);

    /**
     * 修改工单
     */
    int updateWorkOrder(WorkOrder workOrder);

    /**
     * 批量删除工单
     */
    int deleteWorkOrderByIds(Long[] orderIds);

    /**
     * 统计工单数据
     */
    WorkOrderStats selectWorkOrderStats();

    /**
     * 查询故障率最高的设备Top
     */
    List<FaultTopDevice> selectFaultTopDevices();

    /**
     * 批量分配工单
     */
    int batchAssign(Long[] orderIds, String assignTo);

    /**
     * 完成工单
     */
    void completeWorkOrder(WorkOrderRecord record);

    /**
     * 归档工单
     */
    void archiveWorkOrder(Long orderId, String archiveBy, String archiveRemark);
}