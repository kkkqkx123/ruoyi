package com.ruoyi.workorder.service.impl;

import com.ruoyi.common.annotation.RedisCache;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.workorder.domain.DeviceInfo;
import com.ruoyi.workorder.mapper.DeviceInfoMapper;
import com.ruoyi.workorder.service.IDeviceInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备信息Service实现
 */
@Service
public class DeviceInfoServiceImpl implements IDeviceInfoService {

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Override
    public List<DeviceInfo> selectDeviceInfoList(DeviceInfo deviceInfo) {
        return deviceInfoMapper.selectDeviceInfoList(deviceInfo);
    }

    @Override
    @RedisCache(key = CacheConstants.DEVICE_INFO_KEY, keySuffix = "#deviceId", expire = 1800)
    public DeviceInfo selectDeviceInfoById(Long deviceId) {
        return deviceInfoMapper.selectDeviceInfoById(deviceId);
    }

    @Override
    @RedisCache(key = CacheConstants.DEVICE_INFO_KEY, keySuffix = "#deviceInfo.deviceId", action = RedisCache.Action.EVICT)
    public int insertDeviceInfo(DeviceInfo deviceInfo) {
        return deviceInfoMapper.insertDeviceInfo(deviceInfo);
    }

    @Override
    @RedisCache(key = CacheConstants.DEVICE_INFO_KEY, keySuffix = "#deviceInfo.deviceId", action = RedisCache.Action.EVICT)
    public int updateDeviceInfo(DeviceInfo deviceInfo) {
        return deviceInfoMapper.updateDeviceInfo(deviceInfo);
    }

    @Override
    @RedisCache(key = CacheConstants.DEVICE_INFO_KEY, keySuffix = "#deviceIds", action = RedisCache.Action.EVICT)
    public int deleteDeviceInfoByIds(Long[] deviceIds) {
        return deviceInfoMapper.deleteDeviceInfoByIds(deviceIds);
    }
}