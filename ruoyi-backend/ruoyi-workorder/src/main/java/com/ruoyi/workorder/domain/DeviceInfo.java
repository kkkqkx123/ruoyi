package com.ruoyi.workorder.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 设备信息对象 device_info
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long deviceId;

    /** 设备编码 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 设备型号 */
    private String deviceModel;

    /** 安装位置 */
    private String location;

    /** 状态（0正常 1维修中 2报废） */
    private String status;

    /** 采购时间 */
    private Date purchaseTime;

    /** 采购价格 */
    private BigDecimal price;

    /** 负责人 */
    private String responsibleBy;

}