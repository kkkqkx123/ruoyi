package com.ruoyi.workorder.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 工单维修记录对象 work_order_record
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 工单ID */
    private Long orderId;

    /** 维修人 */
    private String repairBy;

    /** 维修时间 */
    private Date repairTime;

    /** 维修方案 */
    private String repairSolution;

    /** 配件消耗 */
    private String partConsumption;

    /** 图片附件（JSON数组字符串） */
    private String imageUrls;

    /** 维修结果（0已修复 1部分修复 2无法修复） */
    private String repairResult;

    /** 维修人姓名（非数据库字段） */
    private String repairName;

    /** 图片列表（非数据库字段，前端使用） */
    private List<String> imageList;

}