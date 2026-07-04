import request from '@/utils/request'
import type { WorkOrderRecord, AjaxResult, TableDataInfo } from '@/types'

// 查询维修记录列表
export function listRecord(query: { orderId?: number }): Promise<TableDataInfo<WorkOrderRecord[]>> {
  return request({
    url: '/workorder/record/list',
    method: 'get',
    params: query
  })
}

// 新增维修记录（完成工单时提交）
export function addRecord(data: WorkOrderRecord): Promise<AjaxResult> {
  return request({
    url: '/workorder/record',
    method: 'post',
    data: data
  })
}

// 删除维修记录
export function delRecord(recordId: number): Promise<AjaxResult> {
  return request({
    url: '/workorder/record/' + recordId,
    method: 'delete'
  })
}