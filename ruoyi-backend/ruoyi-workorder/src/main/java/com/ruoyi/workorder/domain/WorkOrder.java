package com.ruoyi.workorder.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 工单主表对象 work_order
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 设备ID */
    private Long deviceId;

    /** 报修人 */
    private String reporterBy;

    /** 故障描述 */
    private String faultDesc;

    /** 故障类型 */
    private String faultType;

    /** 紧急程度（1普通 2紧急 3特急） */
    private String urgencyLevel;

    /** 工单状态（0未派单 1已派单 2维修中 3已完成 4已归档） */
    private String orderStatus;

    /** 维修员 */
    private String assignTo;

    /** 派单时间 */
    private Date assignTime;

    /** 完成时间 */
    private Date finishTime;

    /** 归档时间 */
    private Date archiveTime;

    /** 归档人 */
    private String archiveBy;

    /** 归档备注 */
    private String archiveRemark;

    /** 设备名称（非数据库字段） */
    private String deviceName;

    /** 设备编码（非数据库字段） */
    private String deviceCode;

    /** 报修人姓名（非数据库字段） */
    private String reporterName;

    /** 维修员姓名（非数据库字段） */
    private String assignName;

    /** 维修记录数（非数据库字段） */
    private Integer recordCount;

}