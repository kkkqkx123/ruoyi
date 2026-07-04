package com.ruoyi.workorder.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workorder.domain.DeviceInfo;
import com.ruoyi.workorder.service.IDeviceInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备信息Controller
 */
@RestController
@RequestMapping("/device/info")
public class DeviceInfoController extends BaseController {

    @Autowired
    private IDeviceInfoService deviceInfoService;

    /**
     * 查询设备信息列表
     */
    @PreAuthorize("@ss.hasPermi('device:info:list')")
    @GetMapping("/list")
    public TableDataInfo list(DeviceInfo deviceInfo) {
        startPage();
        List<DeviceInfo> list = deviceInfoService.selectDeviceInfoList(deviceInfo);
        return getDataTable(list);
    }

    /**
     * 获取设备信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('device:info:query')")
    @GetMapping("/{deviceId}")
    public AjaxResult getInfo(@PathVariable Long deviceId) {
        return success(deviceInfoService.selectDeviceInfoById(deviceId));
    }

    /**
     * 新增设备信息
     */
    @PreAuthorize("@ss.hasPermi('device:info:add')")
    @PostMapping
    public AjaxResult add(@RequestBody DeviceInfo deviceInfo) {
        return toAjax(deviceInfoService.insertDeviceInfo(deviceInfo));
    }

    /**
     * 修改设备信息
     */
    @PreAuthorize("@ss.hasPermi('device:info:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody DeviceInfo deviceInfo) {
        return toAjax(deviceInfoService.updateDeviceInfo(deviceInfo));
    }

    /**
     * 删除设备信息
     */
    @PreAuthorize("@ss.hasPermi('device:info:remove')")
    @DeleteMapping("/{deviceIds}")
    public AjaxResult remove(@PathVariable Long[] deviceIds) {
        return toAjax(deviceInfoService.deleteDeviceInfoByIds(deviceIds));
    }
}