# 前端配套功能补充分析报告

> 文档版本：v1.0 | 编写日期：2026-07-04 | 基于 RuoYi-Vue3-TypeScript v3.9.2

---

## 目录

1. [前端现状总览](#一前端现状总览)
2. [阶段一：设备工单 — 前端已实现 vs 待补充](#二阶段一设备工单--前端已实现-vs-待补充)
3. [阶段二：性能优化 — 前端配套分析](#三阶段二性能优化--前端配套分析)
4. [阶段三：安全增强 — 前端配套分析](#四阶段三安全增强--前端配套分析)
5. [阶段四：异常与异步 — 前端配套分析](#五阶段四异常与异步--前端配套分析)
6. [前端测试补充分析](#六前端测试补充分析)
7. [优先级与实施建议](#七优先级与实施建议)
8. [实施路线图](#八实施路线图)

---

## 一、前端现状总览

### 1.1 已实现的前端功能

| 模块 | 文件 | 状态 |
|------|------|------|
| 工单列表页 | `views/workorder/order/index.vue` | ✅ 完整实现（统计看板、多条件查询、状态流转、派单/完成/归档对话框、导入导出） |
| 工单详情页 | `views/workorder/order/detail.vue` | ✅ 完整实现（描述信息 + 维修记录时间线） |
| 维修记录列表 | `views/workorder/record/index.vue` | ✅ 基础 CRUD 实现 |
| 设备信息管理 | `views/device/info/index.vue` | ✅ 基础 CRUD 实现 |
| 工单 API | `api/workorder/order.ts` | ✅ 8 个接口（list/get/add/update/del/batchAssign/export/stats） |
| 维修记录 API | `api/workorder/record.ts` | ✅ 3 个接口（list/add/del） |
| 设备信息 API | `api/device/info.ts` | ✅ 5 个接口（list/get/add/update/del） |
| 类型定义 | `types/api/workorder/order.ts` | ✅ WorkOrder、WorkOrderQueryParams、WorkOrderStats |
| 类型定义 | `types/api/workorder/record.ts` | ✅ WorkOrderRecord |
| 类型定义 | `types/api/device/info.ts` | ✅ DeviceInfo、DeviceInfoQueryParams |
| 前端单元测试 | 5 个文件（~62 个用例） | ✅ API 测试 + 类型测试 + 工具函数测试 |
| 请求拦截器 | `utils/request.ts` | ✅ 基础 Token 注入、防重复提交、401 重登录 |

### 1.2 前端测试覆盖

| 测试文件 | 用例数 | 覆盖范围 |
|----------|--------|----------|
| `api/workorder/order.test.ts` | 15 | 工单 API 请求 |
| `api/workorder/record.test.ts` | 7 | 维修记录 API 请求 |
| `api/device/info.test.ts` | 6 | 设备信息 API 请求 |
| `types/workorder.test.ts` | 12 | 类型定义 |
| `utils/ruoyi.test.ts` | 12 | 工具函数 |
| **合计** | **52** | - |

---

## 二、阶段一：设备工单 — 前端已实现 vs 待补充

### 2.1 已实现对照

对照 [1-device-workorder-design.md](./1-device-workorder-design.md) 的设计要求：

| 设计要求 | 前端实现 | 状态 |
|----------|----------|------|
| 多条件联查（工单编号/设备名称/报修人/状态/紧急程度/时间段） | `index.vue` 查询区已实现 | ✅ |
| 表格多表关联展示（设备名称/报修人姓名/维修员姓名） | 表格列已展示 deviceName/reporterName/assignName | ✅ |
| 状态流转操作按钮（根据角色和状态动态显示） | 5 个按钮：详情/派单/接单/完成/归档 | ✅ |
| 批量分配工单 | `batchAssign` 对话框已实现 | ✅ |
| 批量导出 Excel | `handleExport` 使用 `proxy.download` 实现 | ✅ |
| 工单编号自动生成 | 后端雪花算法，前端无需处理 | ✅ |
| 紧急工单推送 | 后端自动处理，前端无需额外处理 | ✅ |
| 工单完成校验（强制图片+方案） | 前端已校验 `repairSolution` 和 `imageUrls` | ✅ |
| 统计看板（卡片 + Top 设备） | 4 个统计卡片 + 故障率 Top 设备表格 | ✅ |
| 工单详情页（维修记录时间线） | `detail.vue` 含描述信息 + 时间线 | ✅ |

### 2.2 待补充

| 待补充项 | 说明 | 优先级 | 工作量评估 |
|----------|------|--------|-----------|
| Vue 组件单元测试 | 使用 `@vue/test-utils` + `happy-dom` 测试组件渲染和交互 | P2 | 3 个组件 × 5-8 用例 = ~20 个 |
| 工单列表页加载状态优化 | 大数据量时分页加载骨架屏 | P3 | 低 |
| 设备信息页增加工单关联查看 | 在设备详情中查看该设备的历史工单 | P2 | 中 |
| 维修记录图片预览优化 | 大图预览时支持缩放的增强功能 | P3 | 低 |

---

## 三、阶段二：性能优化 — 前端配套分析

对照 [2-performance-optimization-plan.md](./2-performance-optimization-plan.md) 的要求：

### 3.1 后端优化涉及的前端影响

| 后端优化 | 前端影响 | 当前状态 | 待补充 |
|----------|----------|----------|--------|
| `@RedisCache` 缓存设备信息 | 无直接影响，前端无需修改 | ✅ 无需处理 | - |
| 流式导出 Excel（SXSSF） | 当前 `proxy.download` 使用 POST 同步下载，后端改为流式后需确认兼容 | ⚠️ 需验证 | 验证 `responseType: 'blob'` 兼容性 |
| **异步导出任务** | 新增：任务提交 → 轮询状态 → 下载的完整流程 | ❌ 未实现 | **P0 核心** |

### 3.2 异步导出前端新功能

**现状**：当前 `handleExport` 使用 `proxy.download` 同步导出，大数据量时前端会长时间等待。

**改造方案**：后端改为异步导出任务后，前端需要：

#### 3.2.1 新增 API

```typescript
// src/api/system/exportTask.ts
import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

// 提交异步导出任务
export function submitExportTask(query: any): Promise<AjaxResult<{ taskId: number }>> {
  return request({
    url: '/system/exportTask/submit',
    method: 'post',
    data: query
  })
}

// 查询导出任务状态
export function getExportTaskStatus(taskId: number): Promise<AjaxResult<{
  taskId: number
  status: string    // WAITING | PROCESSING | COMPLETED | FAILED
  fileName: string
  fileSize: number
  errorMsg: string
}>> {
  return request({
    url: '/system/exportTask/status/' + taskId,
    method: 'get'
  })
}

// 下载已完成的导出文件
export function downloadExportFile(taskId: number): Promise<Blob> {
  return request({
    url: '/system/exportTask/download/' + taskId,
    method: 'get',
    responseType: 'blob'
  })
}

// 查询导出任务列表
export function listExportTask(query: any): Promise<TableDataInfo<any[]>> {
  return request({
    url: '/system/exportTask/list',
    method: 'get',
    params: query
  })
}
```

#### 3.2.2 新增类型定义

```typescript
// src/types/api/system/exportTask.ts
export interface ExportTask {
  taskId: number
  taskName: string
  status: string       // WAITING / PROCESSING / COMPLETED / FAILED
  fileName: string
  fileSize: number
  errorMsg: string
  createTime: string
  completeTime: string
}
```

#### 3.2.3 修改工单导出逻辑

```typescript
// 在 views/workorder/order/index.vue 中修改
function handleExport() {
  // 改为异步导出
  submitExportTask({
    module: 'workorder',
    params: queryParams.value
  }).then(response => {
    const taskId = response.data.taskId
    proxy.$modal.msgSuccess('导出任务已提交，请在导出任务列表中查看进度')
    // 打开导出任务列表对话框 或 跳转到导出任务页面
    startPolling(taskId)
  })
}

/** 轮询导出任务状态 */
function startPolling(taskId: number) {
  const timer = setInterval(() => {
    getExportTaskStatus(taskId).then(response => {
      const task = response.data
      if (task.status === 'COMPLETED') {
        clearInterval(timer)
        downloadExportFile(taskId).then(blob => {
          saveAs(blob, task.fileName)
        })
      } else if (task.status === 'FAILED') {
        clearInterval(timer)
        proxy.$modal.msgError('导出失败：' + task.errorMsg)
      }
    })
  }, 2000) // 每 2 秒轮询一次
}
```

#### 3.2.4 新增导出任务管理页面

```typescript
// src/views/system/exportTask/index.vue
// 功能：查看所有导出任务列表，支持下载已完成的任务、删除失败的任务
```

---

## 四、阶段三：安全增强 — 前端配套分析

对照 [3-security-enhancement-plan.md](./3-security-enhancement-plan.md) 的要求：

### 4.1 后端优化涉及的前端影响

| 后端优化 | 前端影响 | 当前状态 | 待补充 |
|----------|----------|----------|--------|
| `@Desensitize` 脱敏 | 后端 AOP 统一处理，前端无需修改 | ✅ 无需处理 | - |
| `@RepeatSubmit` 增强 | 若依原生请求拦截器已有前端防重复提交 | ✅ 已有 | 测试验证即可 |
| **双 Token 机制** | **前端需适配 refreshToken 自动续期** | ❌ 未适配 | **P0 核心** |
| **MinIO 文件存储** | 前端文件上传需支持 MinIO 直传 | ⚠️ 部分实现 | P1 可选 |

### 4.2 双 Token 前端适配方案

**现状**：`request.ts` 响应拦截器在收到 401 时直接弹出重新登录对话框，体验差。

**改造目标**：accessToken 过期时，自动使用 refreshToken 无感续期，续期失败才跳转登录。

#### 4.2.1 修改 auth.ts — 增加 refreshToken 存取

```typescript
// src/utils/auth.ts
const TokenKey = 'Admin-Token'
const RefreshTokenKey = 'Admin-Refresh-Token'

export function getToken(): string {
  return Cookies.get(TokenKey)
}

export function setToken(token: string): void {
  Cookies.set(TokenKey, token)
}

export function removeToken(): void {
  Cookies.remove(TokenKey)
}

// 新增 refreshToken 相关方法
export function getRefreshToken(): string {
  return Cookies.get(RefreshTokenKey)
}

export function setRefreshToken(token: string): void {
  Cookies.set(RefreshTokenKey, token)
}

export function removeRefreshToken(): void {
  Cookies.remove(RefreshTokenKey)
}
```

#### 4.2.2 修改 request.ts — 响应拦截器增加 refresh 逻辑

```typescript
// src/utils/request.ts — 响应拦截器改造

// 是否正在刷新 Token 的标记
let isRefreshing = false
// 等待刷新 Token 的请求队列
let refreshSubscribers: ((token: string) => void)[] = []

function onRefreshed(token: string) {
  refreshSubscribers.forEach(callback => callback(token))
  refreshSubscribers = []
}

function addRefreshSubscriber(callback: (token: string) => void) {
  refreshSubscribers.push(callback)
}

// 响应拦截器
service.interceptors.response.use(
  res => {
    // ... 原有逻辑
  },
  async error => {
    const { config, response } = error
    // 401 且不是 refresh 请求本身
    if (response?.status === 401 && !config.url?.includes('/refreshToken')) {
      if (!isRefreshing) {
        isRefreshing = true
        const refreshToken = getRefreshToken()
        if (refreshToken) {
          try {
            const res = await request({
              url: '/refreshToken',
              method: 'post',
              data: { refreshToken },
              headers: { isToken: false }
            })
            const { accessToken, refreshToken: newRefreshToken } = res.data
            setToken(accessToken)
            if (newRefreshToken) setRefreshToken(newRefreshToken)
            isRefreshing = false
            onRefreshed(accessToken)
            // 重试原始请求
            config.headers['Authorization'] = 'Bearer ' + accessToken
            return service(config)
          } catch {
            isRefreshing = false
            refreshSubscribers = []
            // refresh 也失败，跳转登录
            useUserStore().logOut()
            location.href = '/index'
            return Promise.reject(error)
          }
        } else {
          // 没有 refreshToken，直接跳转登录
          useUserStore().logOut()
          location.href = '/index'
          return Promise.reject(error)
        }
      } else {
        // 正在刷新中，将请求加入队列等待
        return new Promise(resolve => {
          addRefreshSubscriber((token: string) => {
            config.headers['Authorization'] = 'Bearer ' + token
            resolve(service(config))
          })
        })
      }
    }
    return Promise.reject(error)
  }
)
```

#### 4.2.3 修改 login.ts — 登录后存储双 Token

```typescript
// src/api/login.ts — 登录方法
export function login(username: string, password: string, code: string, uuid: string) {
  return request({
    url: '/login',
    method: 'post',
    data: { username, password, code, uuid }
  }).then(response => {
    // 后端返回 accessToken + refreshToken
    const { accessToken, refreshToken } = response.data || response
    if (accessToken) setToken(accessToken)
    if (refreshToken) setRefreshToken(refreshToken)
    return response
  })
}
```

### 4.3 MinIO 文件上传前端适配

**现状**：前端使用 `el-upload` 上传到 `/common/upload` 接口，后端支持 local 和 minio 两种策略，前端无需改动（上传接口返回 URL 一致）。

**可选增强**：当需要前端直传 MinIO 时（大文件场景），需要：

```typescript
// 获取 MinIO 临时上传凭证
export function getMinioUploadUrl(): Promise<AjaxResult<{
  uploadUrl: string      // 预签名上传 URL
  fileUrl: string        // 上传后的访问 URL
  expiresIn: number      // 有效期（秒）
}>> {
  return request({
    url: '/common/minio/presignedUrl',
    method: 'get'
  })
}
```

**结论**：当前 `strategy: local` 策略下，前端无需改动。若切换为 `minio`，通过 `/common/upload` 上传时后端会自动路由到 MinIO，前端兼容。

---

## 五、阶段四：异常与异步 — 前端配套分析

对照 [4-exception-async-optimization-plan.md](./4-exception-async-optimization-plan.md) 的要求：

### 5.1 后端优化涉及的前端影响

| 后端优化 | 前端影响 | 当前状态 | 待补充 |
|----------|----------|----------|--------|
| `BizErrorCode` 统一错误码 | 前端需处理 1xxx/2xxx/3xxx/4xxx/5xxx 系列错误码 | ❌ 未适配 | **P1 重要** |
| `BizException` 统一异常 | 返回格式 `{code, msg}` 不变，兼容现有 `errorCode` 处理 | ✅ 兼容 | - |
| `GlobalExceptionHandler` 增强 | 后端日志增强，前端无影响 | ✅ 无需处理 | - |
| 异步任务（通知推送） | 后端异步执行，前端无影响 | ✅ 无需处理 | - |

### 5.2 前端错误码适配

**现状**：`src/utils/errorCode.ts` 只定义了 `401`、`403`、`404` 和 `default` 四个条目。

**改造方案**：增加 `BizErrorCode` 对应的错误码映射，支持前端根据错误码显示友好的中文提示。

```typescript
// src/utils/errorCode.ts
const errorCode: Record<string, string> = {
  '401': '认证失败，无法访问系统资源',
  '403': '当前操作没有权限',
  '404': '访问资源不存在',
  'default': '系统未知错误，请反馈给管理员',

  // ===== 工单模块错误码（1xxx）=====
  '1001': '工单不存在',
  '1002': '工单编号已存在',
  '1003': '工单当前状态不允许此操作',
  '1004': '工单已归档，无法修改',
  '1005': '工单创建失败',

  // ===== 设备模块错误码（2xxx）=====
  '2001': '设备不存在',
  '2002': '设备编码已存在',
  '2003': '设备当前状态不允许操作',

  // ===== 库存模块错误码（3xxx）=====
  '3001': '库存不足',

  // ===== 文件模块错误码（4xxx）=====
  '4001': '文件类型不允许上传',
  '4002': '文件内容不安全',
  '4003': '文件大小超出限制',
  '4004': '文件上传失败',

  // ===== 通用错误码（5xxx）=====
  '5001': '请求参数错误',
  '5002': '业务处理失败',
  '5003': '操作过于频繁，请稍后再试',
}

export default errorCode
```

**前端响应拦截器兼容性**：当前 `request.ts` 的响应拦截器逻辑为：

```typescript
const code = res.data.code || 200
const msg = errorCode[code] || res.data.msg || errorCode['default']
```

已能正确处理 `BizErrorCode` 返回的 `{code: 1001, msg: "工单不存在"}`，只需更新 `errorCode.ts` 即可。

---

## 六、前端测试补充分析

### 6.1 现有测试缺口

| 测试类别 | 已有 | 待补充 | 说明 |
|----------|------|--------|------|
| API 单元测试 | 28 个 | 5-8 个 | 新增 asyncExport 等 API |
| 类型定义测试 | 12 个 | 5-8 个 | ExportTask 等新类型 |
| 工具函数测试 | 12 个 | 0 个 | 已覆盖充分 |
| **Vue 组件测试** | **0 个** | **15-20 个** | **最大缺口** |
| **E2E 测试** | **0 个** | **15-25 个** | **最大缺口** |
| 请求拦截器测试 | 0 个 | 8-10 个 | Token 续期、防重复提交 |

### 6.2 待补充测试详情

#### 6.2.1 Vue 组件测试（使用 @vue/test-utils + happy-dom）

| 组件 | 测试项 | 预估用例数 | 优先级 |
|------|--------|-----------|--------|
| `views/workorder/order/index.vue` | 渲染查询条件、表格数据展示、分页、派单对话框、完成对话框、归档对话框、导出、删除确认 | 8-10 | P1 |
| `views/workorder/order/detail.vue` | 渲染工单详情、维修记录时间线、空记录显示 | 3-4 | P2 |
| `views/device/info/index.vue` | 设备列表渲染、新增/编辑对话框、删除确认 | 4-6 | P2 |

#### 6.2.2 请求拦截器测试

| 测试项 | 预估用例数 | 优先级 |
|--------|-----------|--------|
| Token 注入到请求头 | 2 | P1 |
| 防重复提交拦截 | 3 | P1 |
| 401 响应触发重新登录 | 2 | P1 |
| 响应成功数据处理 | 2 | P1 |
| 下载 Blob 数据处理 | 2 | P2 |

#### 6.2.3 E2E 测试（使用 Playwright）

| 测试场景 | 预估用例数 | 优先级 |
|----------|-----------|--------|
| 管理员登录 → 创建工单 → 派单 → 维修员接单 → 完成 → 归档 | 1 (场景式) | P0 |
| 工单状态流转校验（非法操作被阻止） | 3-4 | P0 |
| 多条件查询 + 重置 | 2-3 | P1 |
| 批量分配维修员 | 1-2 | P1 |
| Excel 导出 | 1-2 | P2 |
| 统计看板数据刷新 | 1-2 | P2 |
| 设备 CRUD 完整流程 | 1-2 | P1 |
| 防重复提交（双击拦截） | 1-2 | P1 |
| Token 过期自动续期 | 1-2 | P0 |

### 6.3 组件测试示例

```typescript
// __tests__/views/workorder/order/index.spec.ts
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import Order from '@/views/workorder/order/index.vue'

// Mock API 模块
vi.mock('@/api/workorder/order', () => ({
  listOrder: vi.fn().mockResolvedValue({
    code: 200,
    rows: [
      { orderId: 1, orderNo: 'WO20260704001', deviceName: '设备A', orderStatus: '0' }
    ],
    total: 1
  }),
  getWorkOrderStats: vi.fn().mockResolvedValue({
    code: 200,
    data: { totalCount: 10, pendingCount: 3, inProgressCount: 2, completedCount: 5, faultTopDevices: [] }
  })
}))

describe('工单列表页', () => {
  it('应渲染统计看板卡片', async () => {
    const wrapper = mount(Order, {
      global: {
        plugins: [createTestingPinia({ createSpy: vi.fn })]
      }
    })
    // 等待异步数据加载
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    // 验证统计卡片已渲染
    expect(wrapper.text()).toContain('当月工单总量')
    expect(wrapper.text()).toContain('待处理工单')
  })

  it('应渲染工单表格', async () => {
    const wrapper = mount(Order, {
      global: {
        plugins: [createTestingPinia({ createSpy: vi.fn })]
      }
    })
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('WO20260704001')
    expect(wrapper.text()).toContain('设备A')
  })
})
```

---

## 七、优先级与实施建议

### 7.1 实施路线图

```
第一优先级（P0）—— 核心功能稳定性（约 15-20 个测试 + 3 个改造）
├── 双 Token 自动续期（核心体验）
│   ├── 修改 src/utils/auth.ts — 增加 refreshToken 存取
│   ├── 修改 src/utils/request.ts — 响应拦截器增加 refresh 逻辑
│   └── 新增 refreshToken API 测试（3-4 个用例）
├── 异步导出前端功能（性能优化关键）
│   ├── 新增 src/api/system/exportTask.ts
│   ├── 新增 src/types/api/system/exportTask.ts
│   ├── 新增 src/views/system/exportTask/index.vue
│   └── 修改 src/views/workorder/order/index.vue handleExport
├── 错误码适配（用户体验）
│   └── 修改 src/utils/errorCode.ts — 增加 BizErrorCode 映射
└── 工单完整生命周期 E2E 测试
    └── 新增 Playwright E2E 测试（5-8 个场景）

第二优先级（P1）—— 功能完整性（约 15-20 个测试）
├── Vue 组件单元测试
│   ├── workorder/order/index.vue 测试（8-10 个）
│   └── device/info/index.vue 测试（4-6 个）
├── 请求拦截器测试（8-10 个）
├── 设备工单关联查看功能
│   └── 修改 device/info/index.vue，增加工单历史标签页
└── E2E 测试补充
    ├── 多条件查询 + 重置（2-3 个）
    ├── 批量分配（1-2 个）
    └── 设备 CRUD 流程（1-2 个）

第三优先级（P2）—— 边界与兼容性（约 10-15 个测试）
├── 工单详情页组件测试（3-4 个）
├── 维修记录列表组件测试（2-3 个）
├── 导出任务管理页面测试（3-4 个）
└── 边界场景 E2E 测试（Excel 导出、统计看板、缓存失效）
```

### 7.2 测试环境搭建

```bash
# 前端组件测试依赖
cd ruoyi
npm install -D @vue/test-utils happy-dom

# 前端 E2E 测试依赖（Playwright）
npm install -D @playwright/test
npx playwright install chromium

# 运行前端单元测试
npx vitest run

# 运行前端 E2E 测试（需先启动后端和前端）
npm run dev &  # 启动前端
npx playwright test
```

### 7.3 验收标准

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 前端单元测试数 | ≥ 80 个 | 当前 52 个，需补充 ~30 个 |
| 组件测试覆盖 | ≥ 3 个核心组件 | order/index、detail、device/info |
| E2E 测试数 | ≥ 15 个 | 覆盖关键用户场景 |
| 双 Token 续期 | 自动无感续期 | 401 时自动 refresh，不弹登录框 |
| 异步导出 | 提交任务 → 轮询 → 下载 | 不阻塞前端操作 |
| 错误码显示 | BizErrorCode 显示中文消息 | 所有 1xxx-5xxx 错误码有对应中文提示 |

### 7.4 风险与注意事项

| 风险 | 说明 | 缓解措施 |
|------|------|----------|
| 双 Token 并发请求 | 多个请求同时 401 会触发多次 refresh | 使用 `isRefreshing` 标志 + 请求队列 |
| refreshToken 泄露 | refreshToken 有效期长，泄露风险高 | 使用 HttpOnly Cookie 存储（后端实施） |
| 组件测试环境 | Vue 3 + Element Plus 组件 mock 复杂度 | 使用 `@vue/test-utils` 的 `stubs` 选项 |
| E2E 测试环境依赖 | 需要完整后端 + 数据库 + Redis 运行 | 配置 Docker Compose 测试环境 |
| 异步导出轮询开销 | 大量用户同时导出时轮询请求过多 | 轮询间隔 ≥ 2 秒，任务完成后停止轮询 |