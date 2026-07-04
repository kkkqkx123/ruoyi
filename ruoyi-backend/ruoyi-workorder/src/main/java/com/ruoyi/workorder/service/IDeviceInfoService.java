package com.ruoyi.workorder.service;

import com.ruoyi.workorder.domain.DeviceInfo;

import java.util.List;

/**
 * 设备信息Service接口
 *
 * @author ruoyi
 */
public interface IDeviceInfoService {

    /**
     * 查询设备信息列表
     */
    List<DeviceInfo> selectDeviceInfoList(DeviceInfo deviceInfo);

    /**
     * 根据ID查询设备信息
     */
    DeviceInfo selectDeviceInfoById(Long deviceId);

    /**
     * 新增设备信息
     */
    int insertDeviceInfo(DeviceInfo deviceInfo);

    /**
     * 修改设备信息
     */
    int updateDeviceInfo(DeviceInfo deviceInfo);

    /**
     * 批量删除设备信息
     */
    int deleteDeviceInfoByIds(Long[] deviceIds);
}