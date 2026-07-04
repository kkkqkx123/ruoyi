<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="工单ID" prop="orderId" v-if="!orderId">
        <el-input v-model="queryParams.orderId" placeholder="工单ID" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['workorder:record:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="recordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="记录ID" align="center" prop="recordId" width="80" />
      <el-table-column label="工单ID" align="center" prop="orderId" width="80" />
      <el-table-column label="维修人" align="center" prop="repairName" width="100" />
      <el-table-column label="维修时间" align="center" prop="repairTime" width="160" />
      <el-table-column label="维修方案" align="center" prop="repairSolution" :show-overflow-tooltip="true" />
      <el-table-column label="配件消耗" align="center" prop="partConsumption" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="维修结果" align="center" width="100">
        <template #default="scope">
          <dict-tag :options="work_order_repair_result" :value="scope.row.repairResult" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['workorder:record:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="Record">
import { listRecord, delRecord } from "@/api/workorder/record"
import { useDict } from "@/utils/dict"
import type { WorkOrderRecord } from '@/types'

const { proxy } = getCurrentInstance()
const { work_order_repair_result } = useDict("work_order_repair_result")

const props = defineProps({
  orderId: { type: Number, default: undefined }
})

const recordList = ref<WorkOrderRecord[]>([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderId: props.orderId
})

/** 查询维修记录列表 */
function getList() {
  loading.value = true
  listRecord(queryParams).then(response => {
    recordList.value = response.rows
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
function handleSelectionChange(selection: WorkOrderRecord[]) {
  ids.value = selection.map(item => item.recordId!)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 删除按钮操作 */
function handleDelete(row?: WorkOrderRecord) {
  const recordIds = row?.recordId || ids.value
  proxy.$modal.confirm('是否确认删除维修记录编号为"' + recordIds + '"的数据项?').then(function () {
    return delRecord(recordIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>