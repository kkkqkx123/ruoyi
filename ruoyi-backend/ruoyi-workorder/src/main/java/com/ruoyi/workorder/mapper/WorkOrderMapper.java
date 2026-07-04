package com.ruoyi.workorder.mapper;

import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.WorkOrderStats;
import com.ruoyi.workorder.domain.FaultTopDevice;

import java.util.List;

/**
 * 工单Mapper接口
 *
 * @author ruoyi
 */
public interface WorkOrderMapper {

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
     * 删除工单
     */
    int deleteWorkOrderById(Long orderId);

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
}