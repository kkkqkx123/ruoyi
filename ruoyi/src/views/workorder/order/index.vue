<template>
  <div class="app-container">
    <!-- 统计看板 -->
    <el-row :gutter="20" class="mb8">
      <el-col :span="6">
        <el-card shadow="hover" class="stats-card">
          <div class="stats-item">
            <div class="stats-label">当月工单总量</div>
            <div class="stats-value">{{ stats.totalCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stats-card stats-warning">
          <div class="stats-item">
            <div class="stats-label">待处理工单</div>
            <div class="stats-value">{{ stats.pendingCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stats-card stats-primary">
          <div class="stats-item">
            <div class="stats-label">维修中</div>
            <div class="stats-value">{{ stats.inProgressCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stats-card stats-success">
          <div class="stats-item">
            <div class="stats-label">已完成</div>
            <div class="stats-value">{{ stats.completedCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 故障率Top设备 -->
    <el-card shadow="hover" class="mb8">
      <template #header>
        <span>故障率Top设备</span>
      </template>
      <el-table :data="stats.faultTopDevices" v-loading="statsLoading" max-height="250">
        <el-table-column label="排名" type="index" width="60" align="center" />
        <el-table-column label="设备名称" prop="deviceName" />
        <el-table-column label="故障次数" prop="faultCount" align="center" />
        <el-table-column label="占比" align="center">
          <template #default="scope">
            {{ ((scope.row.faultCount / stats.totalCount) * 100).toFixed(1) }}%
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 查询区域 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="工单编号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="工单编号" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="设备名称" prop="deviceName">
        <el-input v-model="queryParams.deviceName" placeholder="设备名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="报修人" prop="reporterBy">
        <el-input v-model="queryParams.reporterBy" placeholder="报修人" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工单状态" prop="orderStatus">
        <el-select v-model="queryParams.orderStatus" placeholder="工单状态" clearable style="width: 140px">
          <el-option v-for="dict in work_order_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="紧急程度" prop="urgencyLevel">
        <el-select v-model="queryParams.urgencyLevel" placeholder="紧急程度" clearable style="width: 140px">
          <el-option v-for="dict in work_order_urgency" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="报修时间" style="width: 308px">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮行 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['workorder:order:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['workorder:order:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['workorder:order:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="User" :disabled="multiple" @click="handleBatchAssign" v-hasPermi="['workorder:order:assign']">分配</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['workorder:order:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 工单列表表格 -->
    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="工单编号" align="center" prop="orderNo" width="180" />
      <el-table-column label="设备名称" align="center" prop="deviceName" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="故障描述" align="center" prop="faultDesc" width="200" :show-overflow-tooltip="true" />
      <el-table-column label="报修人" align="center" prop="reporterName" width="100" />
      <el-table-column label="紧急程度" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="work_order_urgency" :value="scope.row.urgencyLevel" />
        </template>
      </el-table-column>
      <el-table-column label="工单状态" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="work_order_status" :value="scope.row.orderStatus" />
        </template>
      </el-table-column>
      <el-table-column label="维修员" align="center" prop="assignName" width="100" />
      <el-table-column label="报修时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="200">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
          <el-button v-if="scope.row.orderStatus === '0'" link type="primary" icon="User" @click="handleAssign(scope.row)" v-hasPermi="['workorder:order:assign']">派单</el-button>
          <el-button v-if="scope.row.orderStatus === '1' && scope.row.assignTo === currentUser" link type="primary" icon="CaretRight" @click="handleStartRepair(scope.row)">接单</el-button>
          <el-button v-if="scope.row.orderStatus === '2' && scope.row.assignTo === currentUser" link type="primary" icon="Select" @click="handleComplete(scope.row)">完成</el-button>
          <el-button v-if="scope.row.orderStatus === '3'" link type="primary" icon="FolderChecked" @click="handleArchive(scope.row)" v-hasPermi="['workorder:order:archive']">归档</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改工单对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="orderRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备" prop="deviceId">
              <el-select v-model="form.deviceId" placeholder="请选择设备" filterable style="width: 100%">
                <el-option v-for="item in deviceList" :key="item.deviceId" :label="item.deviceName" :value="item.deviceId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="故障类型" prop="faultType">
              <el-select v-model="form.faultType" placeholder="故障类型" clearable style="width: 100%">
                <el-option v-for="dict in work_order_fault_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急程度" prop="urgencyLevel">
              <el-select v-model="form.urgencyLevel" placeholder="紧急程度">
                <el-option v-for="dict in work_order_urgency" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="故障描述" prop="faultDesc">
              <el-input v-model="form.faultDesc" type="textarea" :rows="3" placeholder="请输入故障描述" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 派单对话框 -->
    <el-dialog title="分配维修员" v-model="assignDialogVisible" width="400px" append-to-body>
      <el-form ref="assignRef" :model="assignForm" label-width="80px">
        <el-form-item label="维修员" prop="assignTo">
          <el-select v-model="assignForm.assignTo" placeholder="请选择维修员" filterable style="width: 100%">
            <el-option v-for="user in repairerList" :key="user.userName" :label="user.nickName" :value="user.userName" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAssign">确 定</el-button>
          <el-button @click="assignDialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 完成工单对话框 -->
    <el-dialog title="完成工单" v-model="completeDialogVisible" width="600px" append-to-body>
      <el-form ref="completeRef" :model="completeForm" :rules="completeRules" label-width="100px">
        <el-form-item label="维修方案" prop="repairSolution">
          <el-input v-model="completeForm.repairSolution" type="textarea" :rows="3" placeholder="请详细填写维修方案" />
        </el-form-item>
        <el-form-item label="配件消耗" prop="partConsumption">
          <el-input v-model="completeForm.partConsumption" placeholder="更换的配件名称及数量" />
        </el-form-item>
        <el-form-item label="维修结果" prop="repairResult">
          <el-radio-group v-model="completeForm.repairResult">
            <el-radio v-for="dict in work_order_repair_result" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图片附件" prop="imageUrls">
          <el-upload
            multiple
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
            :file-list="completeForm.imageUrls"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitComplete">确 定</el-button>
          <el-button @click="completeDialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 归档对话框 -->
    <el-dialog title="归档工单" v-model="archiveDialogVisible" width="500px" append-to-body>
      <el-form ref="archiveRef" :model="archiveForm" label-width="80px">
        <el-form-item label="归档备注" prop="archiveRemark">
          <el-input v-model="archiveForm.archiveRemark" type="textarea" :rows="3" placeholder="请输入归档备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitArchive">确 定</el-button>
          <el-button @click="archiveDialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Order">
import { listOrder, getOrder, delOrder, addOrder, updateOrder, batchAssignOrder, exportOrder, getWorkOrderStats } from "@/api/workorder/order"
import { listDevice } from "@/api/device/info"
import { listUser } from "@/api/system/user"
import { addRecord } from "@/api/workorder/record"
import { getToken } from "@/utils/auth"
import { useDict } from "@/utils/dict"
import useUserStore from "@/store/modules/user"
import type { WorkOrder, WorkOrderQueryParams, WorkOrderStats, DeviceInfo } from '@/types'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const currentUser = computed(() => userStore.name)
const { work_order_status, work_order_urgency, work_order_repair_result, work_order_fault_type } = useDict(
  "work_order_status",
  "work_order_urgency",
  "work_order_repair_result",
  "work_order_fault_type"
)

// 图片上传配置
const uploadUrl = import.meta.env.VITE_APP_BASE_API + "/common/upload"
const uploadHeaders = { Authorization: "Bearer " + getToken() }

// 统计数据
const stats = reactive<WorkOrderStats>({
  totalCount: 0,
  pendingCount: 0,
  inProgressCount: 0,
  completedCount: 0,
  faultTopDevices: []
})
const statsLoading = ref(false)

// 表格数据
const orderList = ref<WorkOrder[]>([])
const deviceList = ref<DeviceInfo[]>([])
const repairerList = ref<any[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const dateRange = ref<string[]>([])

// 派单
const assignDialogVisible = ref(false)
const assignForm = reactive({ assignTo: '' })

// 完成工单
const completeDialogVisible = ref(false)
const completeForm = reactive({
  orderId: undefined as number | undefined,
  repairSolution: '',
  partConsumption: '',
  imageUrls: [] as any[],
  repairResult: '0'
})
const completeRules = {
  repairSolution: [{ required: true, message: "维修方案不能为空", trigger: "blur" }]
}

// 归档
const archiveDialogVisible = ref(false)
const archiveForm = reactive({ archiveRemark: '' })
const archiveOrderId = ref<number>()

const data = reactive({
  form: {} as WorkOrder,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: undefined,
    deviceName: undefined,
    deviceId: undefined,
    reporterBy: undefined,
    assignTo: undefined,
    orderStatus: undefined,
    urgencyLevel: undefined,
    faultType: undefined,
    params: {}
  } as WorkOrderQueryParams,
  rules: {
    deviceId: [{ required: true, message: "请选择设备", trigger: "change" }],
    faultDesc: [{ required: true, message: "故障描述不能为空", trigger: "blur" }],
    urgencyLevel: [{ required: true, message: "请选择紧急程度", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询工单列表 */
function getList() {
  loading.value = true
  listOrder(queryParams.value).then(response => {
    orderList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 加载统计看板 */
function loadStats() {
  statsLoading.value = true
  getWorkOrderStats().then(response => {
    if (response.data) {
      Object.assign(stats, response.data)
    }
  }).finally(() => {
    statsLoading.value = false
  })
}

/** 加载设备列表（供下拉选择） */
function loadDeviceList() {
  listDevice({ pageNum: 1, pageSize: 9999 }).then(response => {
    deviceList.value = response.rows
  })
}

/** 加载维修员列表（供派单选择） */
function loadRepairerList() {
  listUser({ pageNum: 1, pageSize: 9999 }).then(response => {
    repairerList.value = response.rows
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    orderId: undefined,
    deviceId: undefined,
    faultDesc: undefined,
    faultType: undefined,
    urgencyLevel: '1',
    orderStatus: '0'
  } as WorkOrder
  proxy.resetForm("orderRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  addDateRange(queryParams.value, dateRange.value)
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  dateRange.value = []
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: WorkOrder[]) {
  ids.value = selection.map(item => item.orderId!)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增工单"
}

/** 修改按钮操作 */
function handleUpdate(row?: WorkOrder) {
  reset()
  const orderId = row?.orderId || ids.value[0]
  getOrder(orderId).then(response => {
    form.value = response.data!
    open.value = true
    title.value = "修改工单"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["orderRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.orderId != undefined) {
        updateOrder(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addOrder(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
          loadStats()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row?: WorkOrder) {
  const orderIds = row?.orderId || ids.value
  proxy.$modal.confirm('是否确认删除工单编号为"' + orderIds + '"的数据项?').then(function () {
    return delOrder(orderIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 详情查看 */
function handleDetail(row: WorkOrder) {
  const router = useRouter()
  router.push('/workorder/order/detail/' + row.orderId)
}

// ========== 派单操作 ==========

/** 单个派单 */
function handleAssign(row: WorkOrder) {
  assignForm.assignTo = ''
  assignDialogVisible.value = true
  // 保存当前操作的工单ID
  ids.value = [row.orderId!]
}

/** 批量分配 */
function handleBatchAssign() {
  if (ids.value.length === 0) {
    proxy.$modal.msgError("请选择要分配的工单")
    return
  }
  assignForm.assignTo = ''
  assignDialogVisible.value = true
}

/** 提交分配 */
function submitAssign() {
  if (!assignForm.assignTo) {
    proxy.$modal.msgError("请选择维修员")
    return
  }
  batchAssignOrder({ orderIds: ids.value, assignTo: assignForm.assignTo }).then(() => {
    proxy.$modal.msgSuccess("分配成功")
    assignDialogVisible.value = false
    getList()
  })
}

// ========== 接单操作 ==========

/** 接单 */
function handleStartRepair(row: WorkOrder) {
  proxy.$modal.confirm('确认要接单处理工单 "' + row.orderNo + '" 吗?').then(function () {
    return updateOrder({ orderId: row.orderId, orderStatus: '2' } as WorkOrder)
  }).then(() => {
    proxy.$modal.msgSuccess("已接单，开始维修")
    getList()
  }).catch(() => {})
}

// ========== 完成工单 ==========

/** 完成工单 */
function handleComplete(row: WorkOrder) {
  completeForm.orderId = row.orderId
  completeForm.repairSolution = ''
  completeForm.partConsumption = ''
  completeForm.imageUrls = []
  completeForm.repairResult = '0'
  completeDialogVisible.value = true
}

/** 上传成功回调 */
function handleUploadSuccess(response: any) {
  completeForm.imageUrls.push({ name: response.newFileName, url: response.url })
}

/** 移除上传文件 */
function handleUploadRemove(uploadFile: any) {
  completeForm.imageUrls = completeForm.imageUrls.filter((item: any) => item.url !== uploadFile.url)
}

/** 提交完成 */
function submitComplete() {
  if (!completeForm.repairSolution) {
    proxy.$modal.msgError("请填写维修方案")
    return
  }
  if (completeForm.imageUrls.length === 0) {
    proxy.$modal.msgError("请上传至少一张维修图片")
    return
  }
  const recordData = {
    orderId: completeForm.orderId,
    repairSolution: completeForm.repairSolution,
    partConsumption: completeForm.partConsumption,
    imageUrls: JSON.stringify(completeForm.imageUrls.map((item: any) => item.url)),
    repairResult: completeForm.repairResult
  }
  addRecord(recordData).then(() => {
    proxy.$modal.msgSuccess("工单已完成")
    completeDialogVisible.value = false
    getList()
    loadStats()
  })
}

// ========== 归档操作 ==========

function handleArchive(row: WorkOrder) {
  archiveOrderId.value = row.orderId
  archiveForm.archiveRemark = ''
  archiveDialogVisible.value = true
}

function submitArchive() {
  updateOrder({
    orderId: archiveOrderId.value,
    orderStatus: '4',
    archiveRemark: archiveForm.archiveRemark
  } as WorkOrder).then(() => {
    proxy.$modal.msgSuccess("归档成功")
    archiveDialogVisible.value = false
    getList()
    loadStats()
  })
}

// ========== 导出 ==========

/** 导出按钮操作 */
function handleExport() {
  proxy.download("workorder/order/export", {
    ...queryParams.value,
  }, `workorder_${new Date().getTime()}.xlsx`)
}

/** 初始化 */
getList()
loadStats()
loadDeviceList()
loadRepairerList()
</script>

<style scoped>
.stats-card { cursor: pointer; }
.stats-item { text-align: center; }
.stats-label { font-size: 14px; color: #666; margin-bottom: 8px; }
.stats-value { font-size: 28px; font-weight: bold; color: #333; }
.stats-warning :deep(.el-card__body) { border-left: 3px solid #faad14; }
.stats-primary :deep(.el-card__body) { border-left: 3px solid #1890ff; }
.stats-success :deep(.el-card__body) { border-left: 3px solid #52c41a; }
</style>