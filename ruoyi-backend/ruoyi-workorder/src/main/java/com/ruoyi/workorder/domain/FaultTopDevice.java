package com.ruoyi.workorder.domain;

import lombok.Data;

/**
 * 故障率设备VO
 *
 * @author ruoyi
 */
@Data
public class FaultTopDevice {

    /** 设备ID */
    private Long deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 故障次数 */
    private Integer faultCount;

}