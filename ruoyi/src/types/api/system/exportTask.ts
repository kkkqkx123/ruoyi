import type { AjaxResult, TableDataInfo } from '../common'

/** 导出任务 */
export interface ExportTask {
  taskId: number
  taskName: string
  module: string
  status: string       // WAITING / PROCESSING / COMPLETED / FAILED
  fileName: string
  fileSize: number
  errorMsg: string
  createBy: string
  createTime: string
  completeTime: string
}

/** 导出任务查询参数 */
export interface ExportTaskQuery {
  pageNum?: number
  pageSize?: number
  status?: string
  module?: string
  params?: {
    beginTime?: string
    endTime?: string
  }
}

/** 导出任务提交结果 */
export interface ExportTaskResult extends AjaxResult {
  data: {
    taskId: number
  }
}

/** 导出任务状态 */
export interface ExportTaskStatusResult extends AjaxResult {
  data: {
    taskId: number
    status: string
    fileName: string
    fileSize: number
    errorMsg: string
  }
}

// API 接口函数
import request from '@/utils/request'

/** 提交异步导出任务 */
export function submitExportTask(params: { module: string; query: any }): Promise<ExportTaskResult> {
  return request({
    url: '/system/exportTask/submit',
    method: 'post',
    data: params
  })
}

/** 查询导出任务状态 */
export function getExportTaskStatus(taskId: number): Promise<ExportTaskStatusResult> {
  return request({
    url: '/system/exportTask/status/' + taskId,
    method: 'get'
  })
}

/** 下载已完成的导出文件 */
export function downloadExportFile(taskId: number): Promise<Blob> {
  return request({
    url: '/system/exportTask/download/' + taskId,
    method: 'get',
    responseType: 'blob'
  })
}

/** 查询导出任务列表 */
export function listExportTask(query: ExportTaskQuery): Promise<TableDataInfo<ExportTask[]>> {
  return request({
    url: '/system/exportTask/list',
    method: 'get',
    params: query
  })
}