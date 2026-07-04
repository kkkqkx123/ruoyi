<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="任务状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="任务状态" clearable style="width: 140px">
          <el-option label="等待中" value="WAITING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item label="导出模块" prop="module">
        <el-input v-model="queryParams.module" placeholder="导出模块" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:exportTask:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务ID" align="center" prop="taskId" width="80" />
      <el-table-column label="任务名称" align="center" prop="taskName" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="导出模块" align="center" prop="module" width="120" />
      <el-table-column label="任务状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">
            {{ statusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件大小" align="center" prop="fileSize" width="100">
        <template #default="scope">
          <span v-if="scope.row.fileSize">{{ formatFileSize(scope.row.fileSize) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="完成时间" align="center" prop="completeTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="150">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 'COMPLETED'"
            link type="primary" icon="Download"
            @click="handleDownload(scope.row)"
          >下载</el-button>
          <el-button
            v-if="scope.row.status === 'FAILED'"
            link type="danger" icon="InfoFilled"
            @click="showError(scope.row)"
          >错误详情</el-button>
          <el-button
            link type="primary" icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:exportTask:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="ExportTask">
import { listExportTask, getExportTaskStatus, downloadExportFile } from '@/types/api/system/exportTask'
import type { ExportTask, ExportTaskQuery } from '@/types/api/system/exportTask'

const { proxy } = getCurrentInstance()

const taskList = ref<ExportTask[]>([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)

const queryParams = reactive<ExportTaskQuery>({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  module: undefined
})

const statusTypeMap: Record<string, string> = {
  WAITING: 'info',
  PROCESSING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger'
}

const statusLabelMap: Record<string, string> = {
  WAITING: '等待中',
  PROCESSING: '处理中',
  COMPLETED: '已完成',
  FAILED: '失败'
}

function statusType(status: string): string {
  return statusTypeMap[status] || 'info'
}

function statusLabel(status: string): string {
  return statusLabelMap[status] || status
}

/** 格式化文件大小 */
function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/** 查询导出任务列表 */
function getList() {
  loading.value = true
  listExportTask(queryParams).then((response: any) => {
    taskList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: ExportTask[]) {
  ids.value = selection.map(item => item.taskId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 下载已完成的任务文件 */
function handleDownload(row: ExportTask) {
  downloadExportFile(row.taskId).then((blob: Blob) => {
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = row.fileName || `export_${row.taskId}.xlsx`
    link.click()
    URL.revokeObjectURL(link.href)
    proxy.$modal.msgSuccess('下载成功')
  })
}

/** 显示错误详情 */
function showError(row: ExportTask) {
  proxy.$modal.msgError(row.errorMsg || '未知错误')
}

/** 删除按钮操作 */
function handleDelete(row?: ExportTask) {
  const taskIds = row?.taskId || ids.value
  proxy.$modal.confirm('是否确认删除导出任务编号为"' + taskIds + '"的数据项?').then(function () {
    return import('@/types/api/system/exportTask').then(m => m.submitExportTask as any)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>