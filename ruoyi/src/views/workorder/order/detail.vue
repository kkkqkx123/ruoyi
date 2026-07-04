<template>
  <div class="app-container">
    <el-card shadow="hover" class="mb8">
      <template #header>
        <span>工单详情 - {{ orderDetail.orderNo }}</span>
      </template>
      <el-descriptions :column="2" border v-if="orderDetail.orderId">
        <el-descriptions-item label="工单编号">{{ orderDetail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="工单状态">
          <dict-tag :options="work_order_status" :value="orderDetail.orderStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ orderDetail.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备编码">{{ orderDetail.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="报修人">{{ orderDetail.reporterName || orderDetail.reporterBy }}</el-descriptions-item>
        <el-descriptions-item label="报修时间">{{ orderDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <dict-tag :options="work_order_urgency" :value="orderDetail.urgencyLevel" />
        </el-descriptions-item>
        <el-descriptions-item label="故障类型">{{ orderDetail.faultType }}</el-descriptions-item>
        <el-descriptions-item label="维修员">{{ orderDetail.assignName || orderDetail.assignTo }}</el-descriptions-item>
        <el-descriptions-item label="派单时间">{{ orderDetail.assignTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ orderDetail.finishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="归档时间">{{ orderDetail.archiveTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ orderDetail.faultDesc }}</el-descriptions-item>
        <el-descriptions-item label="归档备注" :span="2">{{ orderDetail.archiveRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 维修记录时间线 -->
    <el-card shadow="hover">
      <template #header>
        <span>维修记录</span>
      </template>
      <el-timeline v-if="recordList.length > 0">
        <el-timeline-item
          v-for="record in recordList"
          :key="record.recordId"
          :timestamp="record.repairTime"
          placement="top"
        >
          <div class="record-item">
            <p><strong>维修人：</strong>{{ record.repairName || record.repairBy }}</p>
            <p><strong>维修方案：</strong>{{ record.repairSolution }}</p>
            <p v-if="record.partConsumption"><strong>配件消耗：</strong>{{ record.partConsumption }}</p>
            <p><strong>维修结果：</strong>
              <dict-tag :options="work_order_repair_result" :value="record.repairResult" />
            </p>
            <div v-if="record.imageList && record.imageList.length > 0" class="image-list">
              <el-image
                v-for="(img, idx) in record.imageList"
                :key="idx"
                :src="img"
                :preview-src-list="record.imageList"
                style="width: 100px; height: 100px; margin-right: 8px;"
              />
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无维修记录" />
    </el-card>

    <div style="margin-top: 16px; text-align: center;">
      <el-button @click="goBack">返回列表</el-button>
    </div>
  </div>
</template>

<script setup lang="ts" name="OrderDetail">
import { getOrder } from "@/api/workorder/order"
import { listRecord } from "@/api/workorder/record"
import { useDict } from "@/utils/dict"
import { useRoute, useRouter } from "vue-router"
import type { WorkOrder, WorkOrderRecord } from '@/types'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const { work_order_status, work_order_urgency, work_order_repair_result } = useDict(
  "work_order_status",
  "work_order_urgency",
  "work_order_repair_result"
)

const orderDetail = ref<WorkOrder>({})
const recordList = ref<WorkOrderRecord[]>([])

/** 加载工单详情 */
function loadDetail() {
  const orderId = Number(route.params?.orderId || route.params?.id)
  if (!orderId) return
  getOrder(orderId).then(response => {
    orderDetail.value = response.data!
  })
  listRecord({ orderId }).then(response => {
    recordList.value = response.rows.map((item: WorkOrderRecord) => {
      if (item.imageUrls) {
        try {
          item.imageList = JSON.parse(item.imageUrls)
        } catch {
          item.imageList = []
        }
      }
      return item
    })
  })
}

function goBack() {
  router.push('/workorder/order')
}

loadDetail()
</script>

<style scoped>
.record-item p { margin: 4px 0; }
.image-list { display: flex; flex-wrap: wrap; margin-top: 8px; }
</style>