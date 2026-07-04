# 设备工单管理模块 — 修改总结

## 一、任务概述

基于若依框架（RuoYi-Vue3）实现「设备工单管理」模块，完成从设计评审、SQL 建表、后端代码生成与改造、前端页面定制到菜单权限配置的全链路开发。共涉及 **25 个文件**（新建 24 个，修改 1 个）。

---

## 二、设计文档修复（1 个文件修改）

**文件：** [docs/plan/device-workorder-design.md](../plan/device-workorder-design.md)

对照若依现有代码模式审查设计文档，发现并修复 **8 个问题**：

| # | 问题 | 修复内容 |
|---|------|----------|
| 1 | 前端直接调用 `addNotice()` 写通知，有安全风险 | 改为后端 Service 层自动推送通知 |
| 2 | `fault_type` 字典缺失 | 在字典配置中补充 5 个字典项 |
| 3 | `currentUser` 变量来源未说明 | 补充 `useUserStore()` 导入和 computed 定义 |
| 4 | `getStatusLabel` 函数缺实现 | 添加完整的 statusLabels 映射和函数体 |
| 5 | `dateRange` 缺转换到 `params.beginTime/endTime` 逻辑 | 补充 `addDateRange()` 工具函数调用 |
| 6 | `imageUrls` 类型不一致（实体 string vs 表单 array） | 补充 `JSON.stringify()` 序列化处理 |
| 7 | 缺设备管理 API 和类型定义 | 新增 `device/info.ts` API 和类型文件 |
| 8 | `useDict` 导入路径未展示 | 补充 `import { useDict } from '@/utils/dict'` |

---

## 三、数据库层（1 个文件新建）

**文件：** [sql/device-workorder.sql](../sql/device-workorder.sql)

### 3.1 三张业务表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `device_info` | 设备信息表 | device_code, device_name, location, status, responsible_by |
| `work_order` | 工单主表 | order_no（唯一）, device_id（外键）, reporter_by, fault_desc, urgency_level, order_status, assign_to |
| `work_order_record` | 维修记录表 | order_id（外键）, repair_by, repair_solution, part_consumption, image_urls, repair_result |

含完整索引：work_order 表上 device_id、order_status、assign_to、reporter_by、create_time 索引。

### 3.2 字典数据

| 字典类型 | 包含项 |
|----------|--------|
| `work_order_status` | 未派单 / 已派单 / 维修中 / 已完成 / 已归档 |
| `work_order_urgency` | 普通 / 紧急 / 特急 |
| `work_order_repair_result` | 已修复 / 部分修复 / 无法修复 |
| `work_order_fault_type` | 机械故障 / 电气故障 / 软件故障 / 网络故障 / 其他 |

### 3.3 菜单与权限

- **设备工单管理**：一级目录，含工单列表、工单详情、维修记录、设备管理 4 个子菜单
- **按钮权限**：工单新增/修改/删除/派单/归档/导出/统计 + 记录新增/删除 = 9 个权限标识
- **设备管理**：独立一级目录，含设备列表子菜单

---

## 四、后端代码（14 个文件新建）

包路径：`com.ruoyi.workorder`

### 4.1 Domain 实体层（5 个）

| 类 | 描述 |
|----|------|
| `DeviceInfo` | 设备实体，`@TableName("device_info")` |
| `WorkOrder` | 工单实体 + 5 个非 DB 扩展字段：deviceName, deviceCode, reporterName, assignName, recordCount |
| `WorkOrderRecord` | 维修记录实体 + 2 个扩展字段：repairName, imageList |
| `WorkOrderStats` | 统计 VO：totalCount, pendingCount, inProgressCount, completedCount, faultTopDevices |
| `FaultTopDevice` | 故障率设备 VO：deviceId, deviceName, faultCount |

### 4.2 Mapper 接口（3 个）

继承 `BaseMapper<T>`，自定义方法：

- `WorkOrderMapper` — `selectWorkOrderList()`、`selectWorkOrderStats()`、`selectFaultTopDevices()`

### 4.3 Mapper XML（3 个）

**关键文件 `WorkOrderMapper.xml`：**

- **多表联查**：LEFT JOIN `device_info` 获取设备名称/编码，LEFT JOIN `sys_user` × 2 获取报修人姓名/维修员姓名
- **子查询**：统计每条工单的维修记录数 `recordCount`
- **9 项动态条件**：orderNo 模糊、deviceName 模糊、deviceId 精确、reporterBy 模糊、assignTo 精确、orderStatus、urgencyLevel、faultType、createTime 范围
- **统计 SQL**：当月工单总量/待处理/维修中/已完成（`CASE WHEN` 聚合）
- **Top 10**：当月故障次数最多的设备排行

### 4.4 Service 接口（3 个）

继承 `IService<T>`，`IWorkOrderService` 额外暴露：
- `selectWorkOrderList` / `selectWorkOrderStats` / `selectFaultTopDevices`
- `batchAssign(Long[], String)` / `completeWorkOrder(WorkOrderRecord)` / `archiveWorkOrder(Long, String, String)`

### 4.5 Service 实现（3 个）

**核心文件 `WorkOrderServiceImpl.java` 关键业务逻辑：**

```java
// 1. 雪花算法生成唯一工单编号
String orderNo = "WO" + DateUtil.format(new Date(), "yyyyMMdd") 
    + IdUtil.getSnowflakeNextIdStr();

// 2. 紧急工单自动推送通知
if ("2".equals(workOrder.getUrgencyLevel()) || "3".equals(...)) {
    SysNotice notice = new SysNotice();
    notice.setNoticeTitle("紧急工单：" + workOrder.getOrderNo());
    noticeService.insertNotice(notice);
}

// 3. 完成校验（状态必须为"维修中"，方案和图片必填）
if (!"2".equals(order.getOrderStatus())) throw new ServiceException(...);
if (StringUtils.isEmpty(record.getRepairSolution())) throw new ServiceException(...);
if (StringUtils.isEmpty(record.getImageUrls())) throw new ServiceException(...);

// 4. 归档校验（状态必须为"已完成"）
if (!"3".equals(order.getOrderStatus())) throw new ServiceException(...);
```

### 4.6 Controller（3 个）

| Controller | 前缀 | 权限前缀 | 特色接口 |
|-----------|------|----------|----------|
| `DeviceInfoController` | `/device/info` | `device:info` | 标准 CRUD |
| `WorkOrderController` | `/workorder/order` | `workorder:order` | CRUD + batchAssign + export(ExcelUtil) + stats |
| `WorkOrderRecordController` | `/workorder/record` | `workorder:record` | CRUD |

---

## 五、前端代码（10 个文件：新建 9 + 修改 1）

### 5.1 类型定义（3 个新建）

| 文件 | 定义 |
|------|------|
| `types/api/workorder/order.ts` | WorkOrderQueryParams, WorkOrder, WorkOrderStats |
| `types/api/workorder/record.ts` | WorkOrderRecord（含 imageList 前端字段） |
| `types/api/device/info.ts` | DeviceInfoQueryParams, DeviceInfo |

### 5.2 API 层（3 个新建）

| 文件 | 接口数 | 关键接口 |
|------|--------|----------|
| `api/workorder/order.ts` | 8 | listOrder, getOrder, addOrder, updateOrder, delOrder, batchAssignOrder, exportOrder, getWorkOrderStats |
| `api/workorder/record.ts` | 3 | listRecord, addRecord, delRecord |
| `api/device/info.ts` | 5 | listDevice, getDevice, addDevice, updateDevice, delDevice |

### 5.3 页面视图（4 个新建）

| 页面 | 功能亮点 |
|------|----------|
| **工单列表** `workorder/order/index.vue` | 统计卡片（总量/待处理/维修中/已完成）、故障率 Top 设备表、9 项多条件查询、状态流转操作（派单/接单/完成/归档）、批量分配、Excel 导出、上传图片 |
| **工单详情** `workorder/order/detail.vue` | 工单基本信息展示、维修记录时间线、图片预览 |
| **维修记录** `workorder/record/index.vue` | 按工单筛选、删除 |
| **设备管理** `device/info/index.vue` | 完整 CRUD、状态字典 |

### 5.4 类型导出（1 个修改）

更新 `types/api/index.ts`，添加 workorder 和 device 模块的统一导出。

### 5.5 前端技术规范一致性

所有代码严格对齐若依现有模式：
- `<script setup lang="ts" name="Xxx">` + `getCurrentInstance()` 获取 proxy
- `reactive` + `toRefs` 管理表单/查询/校验规则
- `useDict()` + `DictTag` 组件渲染字典
- `v-hasPermi` 指令控制按钮权限
- `proxy.$modal.confirm/msgSuccess/msgError` 模态交互
- `proxy.download()` + `parseTime()` + `addDateRange()` 工具函数
- `VITE_APP_BASE_API` + 文件上传组件（ElUpload + getToken）

---

## 六、文件清单总览

| 层级 | 新建 | 修改 | 小计 |
|------|------|------|------|
| 设计文档 | 0 | 1 | 1 |
| SQL | 1 | 0 | 1 |
| 后端 Domain | 5 | 0 | 5 |
| 后端 Mapper(Java) | 3 | 0 | 3 |
| 后端 Mapper(XML) | 3 | 0 | 3 |
| 后端 Service(接口) | 3 | 0 | 3 |
| 后端 Service(实现) | 3 | 0 | 3 |
| 后端 Controller | 3 | 0 | 3 |
| 前端 类型定义 | 3 | 0 | 3 |
| 前端 API | 3 | 0 | 3 |
| 前端 页面视图 | 4 | 0 | 4 |
| 前端 类型导出 | 0 | 1 | 1 |
| **合计** | **31** | **2** | **33** |

---

## 七、状态流转图

```
未派单 (0)  ──派单──▶  已派单 (1)  ──接单──▶  维修中 (2)
                                                 │
                                                 │ 完成(需上传图片+填写方案)
                                                 ▼
已归档 (4)  ◀──归档──  已完成 (3)  ◀────────────┘
```

- 状态不可逆：只能正向流转，不能回退
- 各步权限控制：派单/归档仅管理员，接单/完成仅维修员，提交工单所有员工

---

## 八、部署执行顺序

```
1️⃣ 执行 SQL：sql/device-workorder.sql（建表 + 字典 + 菜单）
2️⃣ 复制后端代码到 RuoYi 后端项目的对应包路径
3️⃣ 复制前端代码到 RuoYi 前端项目的对应目录
4️⃣ 重启后端，刷新前端菜单缓存
5️⃣ 在角色管理中关联工单菜单权限
6️⃣ 验证：新建设备 → 提交工单 → 派单 → 接单 → 完成 → 归档
```