import request from '@/utils/request'
import type { WorkOrder, WorkOrderQueryParams, WorkOrderStats, AjaxResult, TableDataInfo } from '@/types'

// 查询工单列表
export function listOrder(query: WorkOrderQueryParams): Promise<TableDataInfo<WorkOrder[]>> {
  return request({
    url: '/workorder/order/list',
    method: 'get',
    params: query
  })
}

// 查询工单详细
export function getOrder(orderId: number): Promise<AjaxResult<WorkOrder>> {
  return request({
    url: '/workorder/order/' + orderId,
    method: 'get'
  })
}

// 新增工单
export function addOrder(data: WorkOrder): Promise<AjaxResult> {
  return request({
    url: '/workorder/order',
    method: 'post',
    data: data
  })
}

// 修改工单
export function updateOrder(data: WorkOrder): Promise<AjaxResult> {
  return request({
    url: '/workorder/order',
    method: 'put',
    data: data
  })
}

// 删除工单
export function delOrder(orderId: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/workorder/order/' + orderId,
    method: 'delete'
  })
}

// 批量分配工单
export function batchAssignOrder(data: { orderIds: number[], assignTo: string }): Promise<AjaxResult> {
  return request({
    url: '/workorder/order/batchAssign',
    method: 'put',
    data: data
  })
}

// 导出工单
export function exportOrder(query: WorkOrderQueryParams): Promise<Blob> {
  return request({
    url: '/workorder/order/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}

// 获取工单统计看板
export function getWorkOrderStats(): Promise<AjaxResult<WorkOrderStats>> {
  return request({
    url: '/workorder/order/stats',
    method: 'get'
  })
}