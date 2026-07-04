package com.ruoyi.workorder.domain;

import lombok.Data;

import java.util.List;

/**
 * 工单统计VO
 *
 * @author ruoyi
 */
@Data
public class WorkOrderStats {

    /** 当月工单总量 */
    private Long totalCount;

    /** 待处理工单数 */
    private Long pendingCount;

    /** 维修中工单数 */
    private Long inProgressCount;

    /** 已完成工单数 */
    private Long completedCount;

    /** 故障率Top设备 */
    private List<FaultTopDevice> faultTopDevices;

}