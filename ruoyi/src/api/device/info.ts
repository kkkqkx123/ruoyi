import request from '@/utils/request'
import type { DeviceInfo, DeviceInfoQueryParams, AjaxResult, TableDataInfo } from '@/types'

// 查询设备列表
export function listDevice(query: DeviceInfoQueryParams): Promise<TableDataInfo<DeviceInfo[]>> {
  return request({
    url: '/device/info/list',
    method: 'get',
    params: query
  })
}

// 查询设备详细
export function getDevice(deviceId: number): Promise<AjaxResult<DeviceInfo>> {
  return request({
    url: '/device/info/' + deviceId,
    method: 'get'
  })
}

// 新增设备
export function addDevice(data: DeviceInfo): Promise<AjaxResult> {
  return request({
    url: '/device/info',
    method: 'post',
    data: data
  })
}

// 修改设备
export function updateDevice(data: DeviceInfo): Promise<AjaxResult> {
  return request({
    url: '/device/info',
    method: 'put',
    data: data
  })
}

// 删除设备
export function delDevice(deviceId: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/device/info/' + deviceId,
    method: 'delete'
  })
}