# 设备工单管理模块 — 设计文档

> 文档版本：v1.0 | 编写日期：2026-07-03 | 基于 RuoYi-Vue3-TypeScript v3.9.2

---

## 一、概述

### 1.1 业务背景

设备工单管理是企业设备运维体系的核心模块，覆盖设备从**报修 → 派单 → 维修 → 归档**的全生命周期管理。本模块基于若依快速开发框架，利用代码生成器搭建基础 CRUD，并在此基础上进行深度的业务定制。

### 1.2 业务角色

| 角色 | 权限范围 | 核心操作 |
|------|----------|----------|
| **管理员** | 全局管理权限 | 派单、审核归档、统计看板、批量导出 |
| **维修员** | 工单执行权限 | 接单、维修、上传记录、完成工单 |
| **普通员工** | 报修权限 | 提交工单、查看本人工单状态 |

### 1.3 业务流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 员工提交  │ → │ 管理员    │ → │ 维修员    │ → │ 维修员    │ → │ 管理员    │
│ 报修工单  │   │ 派单维修员│   │ 接单维修  │   │ 完成工单  │   │ 审核归档  │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
     │              │              │              │              │
     ▼              ▼              ▼              ▼              ▼
  未派单         已派单         维修中         已完成         已归档
```

---

## 二、数据库设计

### 2.1 设备表（device_info）

| 字段名 | 类型 | 长度 | 必填 | 说明 |
|--------|------|------|------|------|
| device_id | bigint | - | PK | 设备编号，自增主键 |
| device_code | varchar | 64 | Y | 设备编码，唯一索引 |
| device_name | varchar | 100 | Y | 设备名称 |
| device_model | varchar | 100 | N | 设备型号 |
| location | varchar | 255 | N | 安装位置 |
| status | char | 1 | Y | 状态（0正常 1维修中 2报废） |
| purchase_time | datetime | - | N | 采购时间 |
| price | decimal | 10,2 | N | 采购价格 |
| responsible_by | varchar | 64 | N | 负责人（关联用户表） |
| remark | varchar | 500 | N | 备注 |
| create_by | varchar | 64 | - | 创建者（框架字段） |
| create_time | datetime | - | - | 创建时间（框架字段） |
| update_by | varchar | 64 | - | 更新者（框架字段） |
| update_time | datetime | - | - | 更新时间（框架字段） |

```sql
CREATE TABLE device_info (
  device_id       bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  device_code     varchar(64)   NOT NULL COMMENT '设备编码',
  device_name     varchar(100)  NOT NULL COMMENT '设备名称',
  device_model    varchar(100)  DEFAULT NULL COMMENT '设备型号',
  location        varchar(255)  DEFAULT NULL COMMENT '安装位置',
  status          char(1)       DEFAULT '0' COMMENT '状态（0正常 1维修中 2报废）',
  purchase_time   datetime      DEFAULT NULL COMMENT '采购时间',
  price           decimal(10,2) DEFAULT NULL COMMENT '采购价格',
  responsible_by  varchar(64)   DEFAULT NULL COMMENT '负责人',
  remark          varchar(500)  DEFAULT NULL COMMENT '备注',
  create_by       varchar(64)   DEFAULT '' COMMENT '创建者',
  create_time     datetime      DEFAULT NULL COMMENT '创建时间',
  update_by       varchar(64)   DEFAULT '' COMMENT '更新者',
  update_time     datetime      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (device_id),
  UNIQUE KEY uk_device_code (device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';
```

### 2.2 工单主表（work_order）

| 字段名 | 类型 | 长度 | 必填 | 说明 |
|--------|------|------|------|------|
| order_id | bigint | - | PK | 工单ID，自增主键 |
| order_no | varchar | 32 | Y | 工单编号，唯一索引（雪花算法生成） |
| device_id | bigint | - | Y | 关联设备ID |
| reporter_by | varchar | 64 | Y | 报修人（关联用户表） |
| fault_desc | varchar | 1000 | Y | 故障描述 |
| fault_type | varchar | 64 | N | 故障类型（字典） |
| urgency_level | char | 1 | Y | 紧急程度（1普通 2紧急 3特急） |
| order_status | char | 1 | Y | 工单状态（0未派单 1已派单 2维修中 3已完成 4已归档） |
| assign_to | varchar | 64 | N | 指派的维修员（关联用户表） |
| assign_time | datetime | - | N | 派单时间 |
| finish_time | datetime | - | N | 完成时间 |
| archive_time | datetime | - | N | 归档时间 |
| archive_by | varchar | 64 | N | 归档人 |
| archive_remark | varchar | 500 | N | 归档备注 |
| create_by | varchar | 64 | - | 创建者（框架字段） |
| create_time | datetime | - | - | 创建时间（框架字段） |
| update_by | varchar | 64 | - | 更新者（框架字段） |
| update_time | datetime | - | - | 更新时间（框架字段） |

```sql
CREATE TABLE work_order (
  order_id       bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  order_no       varchar(32)   NOT NULL COMMENT '工单编号',
  device_id      bigint(20)    NOT NULL COMMENT '设备ID',
  reporter_by    varchar(64)   NOT NULL COMMENT '报修人',
  fault_desc     varchar(1000) NOT NULL COMMENT '故障描述',
  fault_type     varchar(64)   DEFAULT NULL COMMENT '故障类型',
  urgency_level  char(1)       DEFAULT '1' COMMENT '紧急程度（1普通 2紧急 3特急）',
  order_status   char(1)       DEFAULT '0' COMMENT '工单状态（0未派单 1已派单 2维修中 3已完成 4已归档）',
  assign_to      varchar(64)   DEFAULT NULL COMMENT '维修员',
  assign_time    datetime      DEFAULT NULL COMMENT '派单时间',
  finish_time    datetime      DEFAULT NULL COMMENT '完成时间',
  archive_time   datetime      DEFAULT NULL COMMENT '归档时间',
  archive_by     varchar(64)   DEFAULT NULL COMMENT '归档人',
  archive_remark varchar(500)  DEFAULT NULL COMMENT '归档备注',
  create_by      varchar(64)   DEFAULT '' COMMENT '创建者',
  create_time    datetime      DEFAULT NULL COMMENT '创建时间',
  update_by      varchar(64)   DEFAULT '' COMMENT '更新者',
  update_time    datetime      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_device_id (device_id),
  KEY idx_order_status (order_status),
  KEY idx_assign_to (assign_to),
  KEY idx_reporter_by (reporter_by),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单主表';
```

### 2.3 工单维修记录表（work_order_record）

| 字段名 | 类型 | 长度 | 必填 | 说明 |
|--------|------|------|------|------|
| record_id | bigint | - | PK | 记录ID，自增主键 |
| order_id | bigint | - | Y | 关联工单ID |
| repair_by | varchar | 64 | Y | 维修人（关联用户表） |
| repair_time | datetime | - | Y | 维修时间 |
| repair_solution | varchar | 2000 | Y | 维修方案 |
| part_consumption | varchar | 500 | N | 配件消耗 |
| image_urls | varchar | 2000 | N | 图片附件（JSON数组，支持多图） |
| repair_result | char | 1 | Y | 维修结果（0已修复 1部分修复 2无法修复） |
| remark | varchar | 500 | N | 备注 |
| create_by | varchar | 64 | - | 创建者（框架字段） |
| create_time | datetime | - | - | 创建时间（框架字段） |
| update_by | varchar | 64 | - | 更新者（框架字段） |
| update_time | datetime | - | - | 更新时间（框架字段） |

```sql
CREATE TABLE work_order_record (
  record_id        bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  order_id         bigint(20)    NOT NULL COMMENT '工单ID',
  repair_by        varchar(64)   NOT NULL COMMENT '维修人',
  repair_time      datetime      NOT NULL COMMENT '维修时间',
  repair_solution  varchar(2000) NOT NULL COMMENT '维修方案',
  part_consumption varchar(500)  DEFAULT NULL COMMENT '配件消耗',
  image_urls       varchar(2000) DEFAULT NULL COMMENT '图片附件',
  repair_result    char(1)       DEFAULT '0' COMMENT '维修结果（0已修复 1部分修复 2无法修复）',
  remark           varchar(500)  DEFAULT NULL COMMENT '备注',
  create_by        varchar(64)   DEFAULT '' COMMENT '创建者',
  create_time      datetime      DEFAULT NULL COMMENT '创建时间',
  update_by        varchar(64)   DEFAULT '' COMMENT '更新者',
  update_time      datetime      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (record_id),
  KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单维修记录表';
```

### 2.4 E-R 关系

```
┌───────────────┐       ┌──────────────────┐       ┌──────────────────────┐
│  device_info  │       │   work_order     │       │ work_order_record    │
├───────────────┤       ├──────────────────┤       ├──────────────────────┤
│ device_id  PK │──1:N──│ order_id      PK │──1:N──│ record_id         PK │
│ device_code   │       │ order_no         │       │ order_id          FK │
│ device_name   │       │ device_id     FK │       │ repair_by            │
│ location      │       │ reporter_by      │       │ repair_time          │
│ status        │       │ fault_desc       │       │ repair_solution      │
│ ...           │       │ urgency_level    │       │ part_consumption     │
└───────────────┘       │ order_status     │       │ image_urls           │
                        │ assign_to        │       │ repair_result        │
                        │ ...              │       │ ...                  │
                        └──────────────────┘       └──────────────────────┘
```

---

## 三、基于代码生成器的基础搭建

### 3.1 生成配置

| 配置项 | device_info | work_order | work_order_record |
|--------|-------------|------------|-------------------|
| 生成模块名 | device | workorder | workorder |
| 业务名 | info | order | record |
| 功能权限前缀 | device:info | workorder:order | workorder:record |
| 树表 | 否 | 否 | 否 |
| 父菜单 | 设备管理 | 工单管理 | 工单管理 |

### 3.2 生成后的文件结构

```
ruoyi/
├── src/
│   ├── api/
│   │   ├── workorder/
│   │   │   ├── order.ts          # 工单主表 CRUD API
│   │   │   └── record.ts         # 维修记录 CRUD API
│   │   └── device/
│   │       └── info.ts           # 设备信息 CRUD API
│   ├── views/
│   │   ├── workorder/
│   │   │   ├── order/
│   │   │   │   ├── index.vue     # 工单列表页
│   │   │   │   └── detail.vue    # 工单详情页
│   │   │   └── record/
│   │   │       └── index.vue     # 维修记录列表
│   │   └── device/
│   │       └── info/
│   │           └── index.vue     # 设备信息列表页
│   ├── types/
│   │   └── api/
│   │       ├── workorder/
│   │       │   ├── order.ts      # 工单类型定义
│   │       │   └── record.ts     # 维修记录类型定义
│   │       └── device/
│   │           └── info.ts       # 设备信息类型定义
│   └── router/                   # 动态路由由后端管理
```

> 注：device_info 表可复用若依现有的代码生成方式，但本设计建议设备管理作为独立基础模块，工单为核心业务模块。

---

## 四、自定义改造：前端改造重点

### 4.1 工单列表页（多条件联查）

**改造点**：在生成代码基础上，增加多维度联合查询条件。

```typescript
// src/types/api/workorder/order.ts
import type { PageDomain, BaseEntity } from "../common";

/** 工单查询参数 */
export interface WorkOrderQueryParams extends PageDomain {
  orderNo?: string;           // 工单编号模糊查询
  deviceName?: string;        // 设备名称（跨表查询）
  deviceId?: number;          // 精确设备ID
  reporterBy?: string;        // 报修人
  assignTo?: string;          // 维修员
  orderStatus?: string;       // 工单状态
  urgencyLevel?: string;      // 紧急程度
  faultType?: string;         // 故障类型
  params?: {
    beginTime?: string;       // 开始时间
    endTime?: string;         // 结束时间
  };
}

/** 工单实体 */
export interface WorkOrder extends BaseEntity {
  orderId?: number;
  orderNo?: string;
  deviceId?: number;
  deviceName?: string;        // 关联设备名称（非数据库字段）
  deviceCode?: string;        // 关联设备编码
  reporterBy?: string;
  reporterName?: string;      // 报修人姓名（非数据库字段）
  faultDesc?: string;
  faultType?: string;
  urgencyLevel?: string;
  orderStatus?: string;
  assignTo?: string;
  assignName?: string;        // 维修员姓名（非数据库字段）
  assignTime?: string;
  finishTime?: string;
  archiveTime?: string;
  archiveBy?: string;
  archiveRemark?: string;
  recordCount?: number;       // 维修记录数（非数据库字段）
}

/** 工单统计 */
export interface WorkOrderStats {
  totalCount: number;         // 当月工单总量
  pendingCount: number;       // 待处理工单数
  inProgressCount: number;    // 维修中工单数
  completedCount: number;     // 已完成工单数
  faultTopDevices: {          // 故障率Top设备
    deviceId: number;
    deviceName: string;
    faultCount: number;
  }[];
}
```

**前端列表页查询条件区改造**（`views/workorder/order/index.vue`）：

```typescript
// <script setup> 中的关键导入和状态定义
import { useDict } from '@/utils/dict'
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const currentUser = computed(() => userStore.name)  // 当前登录用户名，用于维修员身份判断

const { work_order_status, work_order_urgency, work_order_repair_result, work_order_fault_type } = useDict(
  "work_order_status",
  "work_order_urgency",
  "work_order_repair_result",
  "work_order_fault_type"
)

const dateRange = ref<string[]>([])  // 日期范围选择器 v-model
```

```html
<!-- 多条件查询区域 -->
<el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
  <el-form-item label="工单编号" prop="orderNo">
    <el-input v-model="queryParams.orderNo" placeholder="工单编号" clearable style="width: 160px" />
  </el-form-item>
  <el-form-item label="设备名称" prop="deviceName">
    <el-input v-model="queryParams.deviceName" placeholder="设备名称" clearable style="width: 160px" />
  </el-form-item>
  <el-form-item label="报修人" prop="reporterBy">
    <el-input v-model="queryParams.reporterBy" placeholder="报修人" clearable style="width: 140px" />
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
```

```typescript
/** 日期范围转换为 params.beginTime / params.endTime（使用 RuoYi 的 addDateRange 工具） */
function handleQuery() {
  queryParams.value.pageNum = 1
  addDateRange(queryParams.value, dateRange.value)  // 全局方法，自动设置 params.beginTime / params.endTime
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  dateRange.value = []
  handleQuery()
}
```

### 4.2 工单列表表格（多表关联展示）

**表格列改造**：关联展示设备名称、报修人姓名、维修员姓名。

```html
<el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
  <el-table-column type="selection" width="55" align="center" />
  <el-table-column label="工单编号" align="center" prop="orderNo" width="180" />
  <el-table-column label="设备名称" align="center" prop="deviceName" width="140" :show-overflow-tooltip="true" />
  <el-table-column label="故障描述" align="center" prop="faultDesc" width="200" :show-overflow-tooltip="true" />
  <el-table-column label="报修人" align="center" prop="reporterName" width="100" />
  <el-table-column label="紧急程度" align="center" width="100">
    <template #default="scope">
      <dict-tag :options="work_order_urgency" :value="scope.row.urgencyLevel" />
    </template>
  </el-table-column>
  <el-table-column label="工单状态" align="center" width="100">
    <template #default="scope">
      <dict-tag :options="work_order_status" :value="scope.row.orderStatus" />
    </template>
  </el-table-column>
  <el-table-column label="维修员" align="center" prop="assignName" width="100" />
  <el-table-column label="报修时间" align="center" prop="createTime" width="160" />
  <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
    <template #default="scope">
      <!-- 根据角色和状态展示不同操作按钮 -->
      <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
      <!-- 管理员：派单按钮，仅未派单状态 -->
      <el-button v-if="scope.row.orderStatus === '0'" link type="primary" icon="User" @click="handleAssign(scope.row)" v-hasPermi="['workorder:order:assign']">派单</el-button>
      <!-- 维修员：接单按钮，仅已派单且当前用户是维修员 -->
      <el-button v-if="scope.row.orderStatus === '1' && scope.row.assignTo === currentUser" link type="primary" icon="CaretRight" @click="handleStartRepair(scope.row)">接单</el-button>
      <!-- 维修员：完成工单，仅维修中状态 -->
      <el-button v-if="scope.row.orderStatus === '2' && scope.row.assignTo === currentUser" link type="primary" icon="Select" @click="handleComplete(scope.row)">完成</el-button>
      <!-- 管理员：归档按钮，仅已完成状态 -->
      <el-button v-if="scope.row.orderStatus === '3'" link type="primary" icon="FolderChecked" @click="handleArchive(scope.row)" v-hasPermi="['workorder:order:archive']">归档</el-button>
    </template>
  </el-table-column>
</el-table>
```

### 4.3 工单状态流转（不可逆）

**状态机定义**：

```
  未派单(0) ──→ 已派单(1) ──→ 维修中(2) ──→ 已完成(3) ──→ 已归档(4)
     ↑             ↑             ↑             ↑             ↑
  员工提交     管理员派单    维修员接单    维修员完成    管理员归档
```

**前端状态流转校验**（在操作按钮点击时校验）：

```typescript
// 状态流转映射表
const statusFlow: Record<string, string[]> = {
  '0': ['1'],           // 未派单 → 已派单
  '1': ['2'],           // 已派单 → 维修中
  '2': ['3'],           // 维修中 → 已完成
  '3': ['4'],           // 已完成 → 已归档
}

/** 获取工单状态标签 */
const statusLabels: Record<string, string> = {
  '0': '未派单', '1': '已派单', '2': '维修中', '3': '已完成', '4': '已归档'
}
function getStatusLabel(status: string): string {
  return statusLabels[status] || '未知'
}

/** 校验状态流转是否合法 */
function validateStatusTransition(current: string, target: string): boolean {
  const allowed = statusFlow[current]
  if (!allowed || !allowed.includes(target)) {
    proxy.$modal.msgError(`工单状态不可从 ${getStatusLabel(current)} 变更为 ${getStatusLabel(target)}`)
    return false
  }
  return true
}
```

### 4.4 批量分配工单

```typescript
/** 批量分配工单 */
function handleBatchAssign() {
  if (ids.value.length === 0) {
    proxy.$modal.msgError("请选择要分配的工单")
    return
  }
  // 打开选择维修员对话框
  assignDialogVisible.value = true
}

/** 确认批量分配 */
function submitBatchAssign(assignTo: string) {
  const params = {
    orderIds: ids.value,
    assignTo: assignTo
  }
  batchAssignOrder(params).then(() => {
    proxy.$modal.msgSuccess("分配成功")
    getList()
  })
}
```

### 4.5 批量导出 Excel

复用若依现有的 `proxy.download` 导出机制：

```typescript
/** 导出按钮操作 */
function handleExport() {
  proxy.download("workorder/order/export", {
    ...queryParams.value,
  }, `workorder_${new Date().getTime()}.xlsx`)
}
```

### 4.6 提交工单时自动生成唯一编号

**前端**：提交时由后端生成，前端只需展示后端返回的编号。

**后端 Service 层**（Hutool 雪花算法）：

```java
// 使用 Hutool 的 IdUtil 生成唯一工单编号
// 格式：WO + yyyyMMdd + 15位雪花ID
String orderNo = "WO" + DateUtil.format(new Date(), "yyyyMMdd") + IdUtil.getSnowflakeNextIdStr();
workOrder.setOrderNo(orderNo);
```

### 4.7 紧急工单自动推送站内消息

**设计原则**：紧急工单通知由后端 Service 层在创建工单时自动处理，**前端不直接调用通知 API**，避免安全风险。

**前端**：提交工单时不需额外处理，后端自动完成通知推送，前端仅展示通知结果。

**后端 Service 实现**（复用的通知模块）：

```java
// WorkOrderServiceImpl.java — 创建工单时自动判断
@Override
public int insertWorkOrder(WorkOrder workOrder) {
    // 1. 生成雪花算法工单编号
    String orderNo = "WO" + DateUtil.format(new Date(), "yyyyMMdd")
        + IdUtil.getSnowflakeNextIdStr();
    workOrder.setOrderNo(orderNo);
    workOrder.setOrderStatus("0"); // 未派单
    // 2. 插入工单
    int result = workOrderMapper.insertWorkOrder(workOrder);
    // 3. 如果是紧急/特急工单，自动推送通知给管理员
    if ("2".equals(workOrder.getUrgencyLevel()) || "3".equals(workOrder.getUrgencyLevel())) {
        pushUrgentNotice(workOrder);
    }
    return result;
}

private void pushUrgentNotice(WorkOrder workOrder) {
    SysNotice notice = new SysNotice();
    notice.setNoticeTitle("紧急工单：" + workOrder.getOrderNo());
    notice.setNoticeType("1");  // 通知类型
    notice.setNoticeContent("有新的紧急工单 " + workOrder.getOrderNo()
        + " 需要处理，故障描述：" + workOrder.getFaultDesc()
        + "，请及时派单。");
    notice.setStatus("0");      // 正常
    notice.setCreateBy("system");
    noticeService.insertNotice(notice);
}
```

### 4.8 工单完成校验

**前端**：维修员完成工单时，强制校验必须上传维修图片和填写维修方案。

```typescript
/** 完成工单 */
function handleComplete(row: WorkOrder) {
  // 打开完成工单对话框
  completeForm.value = {
    orderId: row.orderId,
    repairSolution: '',
    partConsumption: '',
    imageUrls: [],          // 前端使用数组，提交时序列化为 JSON 字符串
    repairResult: '0'
  }
  completeDialogVisible.value = true
}

/** 提交完成 */
function submitComplete() {
  const form = { ...completeForm.value }
  // 前端校验
  if (!form.repairSolution) {
    proxy.$modal.msgError("请填写维修方案")
    return
  }
  if (!form.imageUrls || form.imageUrls.length === 0) {
    proxy.$modal.msgError("请上传至少一张维修图片")
    return
  }
  // 将图片数组序列化为 JSON 字符串（实体中的 imageUrls 为 string 类型）
  form.imageUrls = JSON.stringify(form.imageUrls)
  // 提交维修记录 + 更新工单状态
  addRecord(form).then(() => {
    proxy.$modal.msgSuccess("工单已完成")
    completeDialogVisible.value = false
    getList()
  })
}
```

### 4.9 统计看板

**前端**：在工单管理首页增加统计卡片和图表区域。

```html
<!-- 统计卡片 -->
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
  <el-table :data="stats.faultTopDevices" v-loading="statsLoading" max-height="300">
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
```

```typescript
/** 获取统计看板数据 */
function loadStats() {
  statsLoading.value = true
  getWorkOrderStats().then(response => {
    stats.value = response.data || {
      totalCount: 0,
      pendingCount: 0,
      inProgressCount: 0,
      completedCount: 0,
      faultTopDevices: []
    }
  }).finally(() => {
    statsLoading.value = false
  })
}
```

---

## 五、完整前端文件清单

### 5.1 API 层

**`src/api/workorder/order.ts`** — 工单主表 API

```typescript
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
```

**`src/api/workorder/record.ts`** — 维修记录 API

```typescript
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

// 新增维修记录
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
```

**`src/api/device/info.ts`** — 设备信息 API

```typescript
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
```

### 5.2 类型定义

**`src/types/api/workorder/order.ts`** — 见上文 4.1 节

**`src/types/api/workorder/record.ts`**

```typescript
import type { BaseEntity } from "../common";

/** 维修记录 */
export interface WorkOrderRecord extends BaseEntity {
  recordId?: number;
  orderId?: number;
  repairBy?: string;
  repairName?: string;       // 维修人姓名（非数据库字段）
  repairTime?: string;
  repairSolution?: string;
  partConsumption?: string;
  imageUrls?: string;        // JSON数组字符串
  imageList?: string[];      // 解析后的图片列表（前端使用）
  repairResult?: string;
}
```

**`src/types/api/device/info.ts`**

```typescript
import type { PageDomain, BaseEntity } from "../common";

/** 设备查询参数 */
export interface DeviceInfoQueryParams extends PageDomain {
  deviceCode?: string;       // 设备编码
  deviceName?: string;       // 设备名称
  status?: string;           // 设备状态
  responsibleBy?: string;    // 负责人
}

/** 设备信息 */
export interface DeviceInfo extends BaseEntity {
  deviceId?: number;
  deviceCode?: string;
  deviceName?: string;
  deviceModel?: string;
  location?: string;
  status?: string;           // 0正常 1维修中 2报废
  purchaseTime?: string;
  price?: number;
  responsibleBy?: string;
  responsibleName?: string;  // 负责人姓名（非数据库字段）
}
```

### 5.3 类型统一导出

**`src/types/api/index.ts`** 添加：

```typescript
// workorder 模块
export * from "./workorder/order";
export * from "./workorder/record";
// device 模块
export * from "./device/info";
```

### 5.4 页面视图

| 文件路径 | 说明 |
|----------|------|
| `src/views/workorder/order/index.vue` | 工单列表页（含查询/表格/操作/统计看板） |
| `src/views/workorder/order/detail.vue` | 工单详情页（含维修记录时间线） |
| `src/views/workorder/record/index.vue` | 维修记录列表（嵌入工单详情内） |
| `src/views/device/info/index.vue` | 设备信息列表（基础 CRUD） |

### 5.5 字典配置

在若依后端字典管理中新增以下字典：

| 字典类型 | 字典标签 | 字典值 | 说明 |
|----------|----------|--------|------|
| `work_order_status` | 未派单 | 0 | 工单状态 |
| `work_order_status` | 已派单 | 1 | 工单状态 |
| `work_order_status` | 维修中 | 2 | 工单状态 |
| `work_order_status` | 已完成 | 3 | 工单状态 |
| `work_order_status` | 已归档 | 4 | 工单状态 |
| `work_order_urgency` | 普通 | 1 | 紧急程度 |
| `work_order_urgency` | 紧急 | 2 | 紧急程度 |
| `work_order_urgency` | 特急 | 3 | 紧急程度 |
| `work_order_repair_result` | 已修复 | 0 | 维修结果 |
| `work_order_repair_result` | 部分修复 | 1 | 维修结果 |
| `work_order_repair_result` | 无法修复 | 2 | 维修结果 |
| `work_order_fault_type` | 机械故障 | 0 | 故障类型 |
| `work_order_fault_type` | 电气故障 | 1 | 故障类型 |
| `work_order_fault_type` | 软件故障 | 2 | 故障类型 |
| `work_order_fault_type` | 网络故障 | 3 | 故障类型 |
| `work_order_fault_type` | 其他 | 4 | 故障类型 |

### 5.6 权限标识

| 权限标识 | 说明 |
|----------|------|
| `workorder:order:list` | 查询工单 |
| `workorder:order:add` | 新增工单 |
| `workorder:order:edit` | 修改工单 |
| `workorder:order:remove` | 删除工单 |
| `workorder:order:assign` | 派单 |
| `workorder:order:archive` | 归档 |
| `workorder:order:export` | 导出工单 |
| `workorder:order:stats` | 查看统计 |
| `workorder:record:list` | 查询维修记录 |
| `workorder:record:add` | 新增维修记录 |
| `workorder:record:remove` | 删除维修记录 |

---

## 六、后端改造重点（概要）

### 6.1 多表关联查询

**MyBatis Mapper 联查 SQL 示例**（`WorkOrderMapper.xml`）：

```xml
<select id="selectWorkOrderList" parameterType="WorkOrder" resultMap="WorkOrderResult">
  SELECT
    wo.order_id, wo.order_no, wo.fault_desc, wo.urgency_level,
    wo.order_status, wo.create_time,
    di.device_id, di.device_name, di.device_code,
    ru1.user_name AS reporter_name,
    ru2.user_name AS assign_name
  FROM work_order wo
  LEFT JOIN device_info di ON wo.device_id = di.device_id
  LEFT JOIN sys_user ru1 ON wo.reporter_by = ru1.user_name
  LEFT JOIN sys_user ru2 ON wo.assign_to = ru2.user_name
  <where>
    <if test="orderNo != null and orderNo != ''">
      AND wo.order_no LIKE CONCAT('%', #{orderNo}, '%')
    </if>
    <if test="deviceName != null and deviceName != ''">
      AND di.device_name LIKE CONCAT('%', #{deviceName}, '%')
    </if>
    <if test="deviceId != null">
      AND wo.device_id = #{deviceId}
    </if>
    <if test="reporterBy != null and reporterBy != ''">
      AND wo.reporter_by LIKE CONCAT('%', #{reporterBy}, '%')
    </if>
    <if test="assignTo != null and assignTo != ''">
      AND wo.assign_to = #{assignTo}
    </if>
    <if test="orderStatus != null and orderStatus != ''">
      AND wo.order_status = #{orderStatus}
    </if>
    <if test="params.beginTime != null and params.beginTime != ''">
      AND wo.create_time &gt;= #{params.beginTime}
    </if>
    <if test="params.endTime != null and params.endTime != ''">
      AND wo.create_time &lt;= #{params.endTime}
    </if>
  </where>
  ORDER BY wo.create_time DESC
</select>
```

### 6.2 工单编号生成

```java
// 使用 Hutool 雪花算法工具类
import cn.hutool.core.util.IdUtil;

// 在 Service 层 addWorkOrder 方法中
String orderNo = "WO" + DateUtil.format(new Date(), "yyyyMMdd")
    + IdUtil.getSnowflakeNextIdStr();
workOrder.setOrderNo(orderNo);
```

### 6.3 状态流转校验

```java
// 后端状态流转校验
public enum OrderStatus {
    UNASSIGNED("0", "未派单"),
    ASSIGNED("1", "已派单"),
    IN_REPAIR("2", "维修中"),
    COMPLETED("3", "已完成"),
    ARCHIVED("4", "已归档");

    private static final Map<String, Set<String>> FLOW_MAP = new HashMap<>();
    static {
        FLOW_MAP.put("0", Set.of("1"));
        FLOW_MAP.put("1", Set.of("2"));
        FLOW_MAP.put("2", Set.of("3"));
        FLOW_MAP.put("3", Set.of("4"));
    }

    public static void validateTransition(String current, String target) {
        Set<String> allowed = FLOW_MAP.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new ServiceException("工单状态不可逆，无法从 " + current + " 变更为 " + target);
        }
    }
}
```

### 6.4 工单完成校验

```java
// 完成工单时校验
public void completeWorkOrder(WorkOrderRecord record) {
    WorkOrder order = workOrderMapper.selectWorkOrderById(record.getOrderId());
    if (!"2".equals(order.getOrderStatus())) {
        throw new ServiceException("仅维修中的工单可以完成");
    }
    // 校验必须上传图片和填写维修方案
    if (StringUtils.isEmpty(record.getRepairSolution())) {
        throw new ServiceException("请填写维修方案");
    }
    if (StringUtils.isEmpty(record.getImageUrls())) {
        throw new ServiceException("请上传至少一张维修图片");
    }
    // 保存维修记录
    workOrderRecordMapper.insertWorkOrderRecord(record);
    // 更新工单状态
    order.setOrderStatus("3");
    order.setFinishTime(new Date());
    workOrderMapper.updateWorkOrder(order);
}
```

### 6.5 统计看板聚合查询

```xml
<select id="selectWorkOrderStats" resultType="com.ruoyi.workorder.domain.WorkOrderStats">
  SELECT
    COUNT(*) AS totalCount,
    SUM(CASE WHEN order_status IN ('0','1') THEN 1 ELSE 0 END) AS pendingCount,
    SUM(CASE WHEN order_status = '2' THEN 1 ELSE 0 END) AS inProgressCount,
    SUM(CASE WHEN order_status = '3' THEN 1 ELSE 0 END) AS completedCount
  FROM work_order
  WHERE DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')
</select>

<select id="selectFaultTopDevices" resultType="com.ruoyi.workorder.domain.FaultTopDevice">
  SELECT
    di.device_id, di.device_name, COUNT(*) AS faultCount
  FROM work_order wo
  LEFT JOIN device_info di ON wo.device_id = di.device_id
  WHERE DATE_FORMAT(wo.create_time, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')
  GROUP BY wo.device_id
  ORDER BY faultCount DESC
  LIMIT 10
</select>
```

---

## 七、路由与菜单配置

### 7.1 动态路由（后端返回）

后端在 `sys_menu` 表中配置菜单后，通过 `/getRouters` 接口返回给前端动态注册。

| 菜单名称 | 路由地址 | 权限标识 | 组件路径 |
|----------|----------|----------|----------|
| 设备工单管理 | /workorder | - | Layout |
| 工单列表 | /workorder/order | workorder:order:list | workorder/order/index |
| 工单详情 | /workorder/order/detail | workorder:order:list | workorder/order/detail |
| 维修记录 | /workorder/record | workorder:record:list | workorder/record/index |
| 设备管理 | /device | - | Layout |
| 设备信息 | /device/info | device:info:list | device/info/index |

### 7.2 前端路由无需额外配置

所有业务路由均由后端动态管理，前端只需在 `permission.ts` 中通过 `loadView` 自动匹配 views 目录下的组件。

---

## 八、数据结构与字典

### 8.1 字典值定义

| 字典类型 | 字典标签 | 字典值 | 排序 | CSS类 |
|----------|----------|--------|------|-------|
| work_order_status | 未派单 | 0 | 1 | default |
| work_order_status | 已派单 | 1 | 2 | primary |
| work_order_status | 维修中 | 2 | 3 | warning |
| work_order_status | 已完成 | 3 | 4 | success |
| work_order_status | 已归档 | 4 | 5 | info |
| work_order_urgency | 普通 | 1 | 1 | default |
| work_order_urgency | 紧急 | 2 | 2 | warning |
| work_order_urgency | 特急 | 3 | 3 | danger |
| work_order_repair_result | 已修复 | 0 | 1 | success |
| work_order_repair_result | 部分修复 | 1 | 2 | warning |
| work_order_repair_result | 无法修复 | 2 | 3 | danger |
| work_order_fault_type | 机械故障 | 0 | 1 | default |
| work_order_fault_type | 电气故障 | 1 | 2 | default |
| work_order_fault_type | 软件故障 | 2 | 3 | default |
| work_order_fault_type | 网络故障 | 3 | 4 | default |
| work_order_fault_type | 其他 | 4 | 5 | default |

### 8.2 前端字典使用

```typescript
// 在 Vue 组件中加载字典
const { work_order_status, work_order_urgency, work_order_repair_result, work_order_fault_type } = useDict(
  "work_order_status",
  "work_order_urgency",
  "work_order_repair_result",
  "work_order_fault_type"
)
```

---

## 九、实施步骤

### 第1步：数据库建表

在 MySQL 中执行 3 张核心表的建表 SQL。

### 第2步：代码生成器生成基础 CRUD

1. 登录若依后台 → 系统工具 → 代码生成 → 导入 3 张表
2. 分别配置生成选项（模块名、业务名、权限前缀、父菜单）
3. 生成代码并下载，将生成的文件复制到对应位置

### 第3步：前端自定义改造

按本设计文档第四章的内容对生成代码进行以下改造：

1. **类型定义**：添加 `WorkOrderQueryParams`、`WorkOrderStats` 等扩展类型
2. **API 层**：添加 `batchAssignOrder`、`exportOrder`、`getWorkOrderStats` 等接口
3. **工单列表页**：
   - 改造查询条件区，增加多条件查询
   - 改造表格列，展示关联的设备名称、报修人姓名、维修员姓名
   - 增加状态流转操作按钮（根据角色和状态动态显示）
   - 添加统计看板卡片区域
4. **工单详情页**：嵌入维修记录时间线
5. **字典配置**：在后台字典管理中新增工单相关字典

### 第4步：后端自定义改造

按本设计文档第六章的内容对生成代码进行以下改造：

1. **Mapper XML**：改造联查 SQL，关联 device_info 和 sys_user 表
2. **Service 层**：添加工单编号生成、状态流转校验、完成校验
3. **Controller 层**：添加批量分配、导出、统计看板接口
4. **通知模块**：紧急工单自动调用通知接口

### 第5步：菜单和权限配置

1. 在后端菜单管理中新增工单管理菜单目录和菜单项
2. 配置权限标识，关联角色权限

### 第6步：测试验证

1. 测试工单完整生命周期流转（提交 → 派单 → 接单 → 维修 → 完成 → 归档）
2. 测试状态不可逆校验
3. 测试多条件联查
4. 测试批量分配和导出
5. 测试统计看板数据准确性

---

## 十、设计总结

本设计方案基于若依框架的代码生成器快速搭建基础 CRUD，在此基础上进行深度定制：

| 维度 | 方案 |
|------|------|
| **基础能力** | 代码生成器生成 3 张表的 CRUD |
| **多表联查** | 改造 Mapper XML 关联 device_info + sys_user 表 |
| **状态流转** | 前后端双重校验，单向不可逆 |
| **业务编号** | 后端 Hutool 雪花算法自动生成 |
| **消息通知** | 复用若依通知公告模块，紧急工单自动推送 |
| **完成校验** | 前端强制校验图片+方案，后端二次校验 |
| **统计看板** | MyBatis 聚合查询 + 前端卡片展示 |
| **批量操作** | 批量分配 + 批量导出 Excel |
| **权限控制** | 基于若依权限体系，v-hasPermi 指令控制按钮级权限 |