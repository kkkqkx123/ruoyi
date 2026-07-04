package com.ruoyi.workorder.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.ruoyi.common.annotation.RedisCache;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.enums.BizErrorCode;
import com.ruoyi.common.exception.BizException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workorder.domain.FaultTopDevice;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.WorkOrderRecord;
import com.ruoyi.workorder.domain.WorkOrderStats;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import com.ruoyi.workorder.mapper.WorkOrderRecordMapper;
import com.ruoyi.workorder.service.IWorkOrderService;
import com.ruoyi.workorder.service.impl.AsyncWorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 工单主表Service实现
 */
@Service
public class WorkOrderServiceImpl implements IWorkOrderService {

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private WorkOrderRecordMapper workOrderRecordMapper;

    @Autowired
    private AsyncWorkOrderService asyncWorkOrderService;

    @Override
    public List<WorkOrder> selectWorkOrderList(WorkOrder workOrder) {
        return workOrderMapper.selectWorkOrderList(workOrder);
    }

    @Override
    public WorkOrder selectWorkOrderById(Long orderId) {
        return workOrderMapper.selectWorkOrderById(orderId);
    }

    @Override
    @RedisCache(key = CacheConstants.WORKORDER_STATS_KEY, keySuffix = "'dashboard'", expire = 600)
    public WorkOrderStats selectWorkOrderStats() {
        return workOrderMapper.selectWorkOrderStats();
    }

    @Override
    public List<FaultTopDevice> selectFaultTopDevices() {
        return workOrderMapper.selectFaultTopDevices();
    }

    @Override
    @RedisCache(key = CacheConstants.WORKORDER_STATS_KEY, keySuffix = "'dashboard'", action = RedisCache.Action.EVICT)
    public int updateWorkOrder(WorkOrder workOrder) {
        return workOrderMapper.updateWorkOrder(workOrder);
    }

    @Override
    public int deleteWorkOrderByIds(Long[] orderIds) {
        return workOrderMapper.deleteWorkOrderByIds(orderIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWorkOrder(WorkOrder workOrder) {
        // 生成工单编号：WO + yyyyMMdd + 雪花ID
        String orderNo = "WO" + DateUtil.format(new Date(), "yyyyMMdd") + IdUtil.getSnowflakeNextIdStr();
        workOrder.setOrderNo(orderNo);
        workOrder.setOrderStatus("0");

        int result = workOrderMapper.insertWorkOrder(workOrder);

        // 紧急程度为2(紧急)或3(特急)时，异步推送通知（不阻塞主流程）
        if ("2".equals(workOrder.getUrgencyLevel()) || "3".equals(workOrder.getUrgencyLevel())) {
            asyncWorkOrderService.pushUrgentNotice(workOrder);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAssign(Long[] orderIds, String assignTo) {
        int count = 0;
        for (Long orderId : orderIds) {
            WorkOrder workOrder = workOrderMapper.selectWorkOrderById(orderId);
            if (workOrder != null) {
                workOrder.setAssignTo(assignTo);
                workOrder.setAssignTime(new Date());
                workOrder.setOrderStatus("1");
                count += workOrderMapper.updateWorkOrder(workOrder);
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeWorkOrder(WorkOrderRecord record) {
        WorkOrder order = workOrderMapper.selectWorkOrderById(record.getOrderId());
        if (order == null) {
            throw new BizException(BizErrorCode.WORK_ORDER_NOT_FOUND);
        }
        // 校验工单状态必须为"2"(维修中)
        if (!"2".equals(order.getOrderStatus())) {
            throw new BizException(BizErrorCode.WORK_ORDER_STATUS_INVALID);
        }
        // 校验维修方案不为空
        if (StringUtils.isEmpty(record.getRepairSolution())) {
            throw new BizException(BizErrorCode.WORK_ORDER_REPAIR_SOLUTION_EMPTY);
        }
        // 校验图片不为空
        if (StringUtils.isEmpty(record.getImageUrls())) {
            throw new BizException(BizErrorCode.WORK_ORDER_IMAGE_EMPTY);
        }

        // 保存维修记录
        workOrderRecordMapper.insertWorkOrderRecord(record);

        // 更新工单状态为"3"(已完成)，设置完成时间
        order.setOrderStatus("3");
        order.setFinishTime(new Date());
        workOrderMapper.updateWorkOrder(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveWorkOrder(Long orderId, String archiveBy, String archiveRemark) {
        WorkOrder order = workOrderMapper.selectWorkOrderById(orderId);
        if (order == null) {
            throw new BizException(BizErrorCode.WORK_ORDER_NOT_FOUND);
        }
        // 校验状态必须为"3"(已完成)
        if (!"3".equals(order.getOrderStatus())) {
            throw new BizException(BizErrorCode.WORK_ORDER_NOT_COMPLETED);
        }

        order.setOrderStatus("4");
        order.setArchiveTime(new Date());
        order.setArchiveBy(archiveBy);
        order.setArchiveRemark(archiveRemark);
        workOrderMapper.updateWorkOrder(order);
    }
}