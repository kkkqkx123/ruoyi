# 前端新增功能总结

> 文档版本：v1.0 | 编写日期：2026-07-04 | 对应后端计划 1-4

---

## 一、功能总览

本次在原始 RuoYi 前端基础上新增/修改了以下功能：

| 功能 | 涉及文件 | 对应后端计划 | 类型 |
|------|----------|-------------|------|
| 双 Token 自动续期 | 3 个修改 | 计划三（安全增强） | 核心改造 |
| 异步导出任务管理 | 3 个新增 | 计划二（性能优化） | 新增功能 |
| BizErrorCode 错误码映射 | 1 个修改 | 计划四（异常体系） | 增强改造 |

---

## 二、双 Token 自动续期

### 2.1 修改文件

| 文件 | 修改内容 |
|------|----------|
| `src/utils/auth.ts` | 新增 `getRefreshToken()` / `setRefreshToken()` / `removeRefreshToken()` 三个方法，使用 `Admin-Refresh-Token` 作为 Cookie key |
| `src/utils/request.ts` | 响应拦截器 401 处理改造：优先使用 refreshToken 调用 `/refreshToken` 接口无感续期，续期成功重试原始请求，续期失败才跳转登录页。新增 `isRefreshing` / `refreshSubscribers` 队列机制防止并发刷新 |
| `src/store/modules/user.ts` | 登录 action 兼容 `accessToken` + `refreshToken` 双 Token 和 `token` 单 Token 两种模式；登出 action 同时清除 refreshToken |
| `src/types/api/login.ts` | `LoginInfoResult` 增加 `accessToken?` 和 `refreshToken?` 可选字段 |

### 2.2 核心逻辑

```
401 响应
  ├─ 有 refreshToken → 调用 /refreshToken 续期
  │   ├─ 成功 → 更新本地 Token → 重试原始请求
  │   └─ 失败 → 跳转登录页
  └─ 无 refreshToken → 跳转登录页（兼容旧模式）
```

### 2.3 并发安全

使用 `isRefreshing` 标志 + `refreshSubscribers` 请求队列，多个请求同时 401 时只触发一次 refresh 请求，其余请求等待 refresh 完成后自动重试。

---

## 三、异步导出任务管理

### 3.1 新增文件

| 文件 | 说明 |
|------|------|
| `src/types/api/system/exportTask.ts` | 类型定义 + API 接口函数（submitExportTask / getExportTaskStatus / downloadExportFile / listExportTask） |
| `src/views/system/exportTask/index.vue` | 导出任务管理页面（查询、状态标签、下载、删除、错误详情） |
| `src/types/api/index.ts` | 新增 `export * from "./system/exportTask"` 导出 |

### 3.2 类型定义

```typescript
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
```

### 3.3 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/exportTask/submit` | POST | 提交异步导出任务 |
| `/system/exportTask/status/{taskId}` | GET | 查询任务状态 |
| `/system/exportTask/download/{taskId}` | GET | 下载已完成文件 |
| `/system/exportTask/list` | GET | 查询任务列表 |

### 3.4 页面功能

- 状态筛选查询（等待中/处理中/已完成/失败）
- 状态标签颜色区分（info/warning/success/danger）
- 已完成任务可下载，失败任务可查看错误详情
- 支持删除任务

---

## 四、BizErrorCode 错误码映射

### 4.1 修改文件

| 文件 | 修改内容 |
|------|----------|
| `src/utils/errorCode.ts` | 新增 14 个业务错误码映射 |

### 4.2 错误码分段

| 段 | 范围 | 说明 |
|----|------|------|
| 1xxx | 1001-1005 | 工单模块（不存在/已存在/状态不允许/已归档/创建失败） |
| 2xxx | 2001-2003 | 设备模块（不存在/编码已存在/状态不允许） |
| 3xxx | 3001 | 库存模块（库存不足） |
| 4xxx | 4001-4004 | 文件模块（类型不允许/内容不安全/大小超限/上传失败） |
| 5xxx | 5001-5003 | 通用模块（参数错误/业务处理失败/操作频繁） |

### 4.3 兼容性

当前 `request.ts` 响应拦截器 `const msg = errorCode[code] || res.data.msg || errorCode['default']` 已能正确处理，无需额外修改。

---

## 五、测试结果

### 5.1 前端测试（62 个全部通过）

```
Test Files  5 passed (5)
     Tests  62 passed (62)
```

| 测试文件 | 用例数 | 状态 |
|----------|--------|------|
| `api/workorder/order.test.ts` | 17 | ✅ |
| `api/workorder/record.test.ts` | 7 | ✅ |
| `api/device/info.test.ts` | 7 | ✅ |
| `types/workorder.test.ts` | 14 | ✅ |
| `utils/ruoyi.test.ts` | 17 | ✅ |

### 5.2 后端测试（109 个全部通过）

```
Tests run: 109, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 六、文件变更清单

```
ruoyi/
├── src/
│   ├── utils/
│   │   ├── auth.ts              (修改) +refreshToken 存取方法
│   │   ├── request.ts           (修改) +refreshToken 自动续期逻辑
│   │   └── errorCode.ts         (修改) +14 个 BizErrorCode 映射
│   ├── store/modules/
│   │   └── user.ts              (修改) 兼容双 Token 登录/登出
│   ├── types/
│   │   ├── api/
│   │   │   ├── index.ts         (修改) +exportTask 导出
│   │   │   ├── login.ts         (修改) +accessToken/refreshToken 字段
│   │   │   └── system/
│   │   │       └── exportTask.ts (新增) 导出任务类型 + API 函数
│   │   └── ...
│   └── views/
│       └── system/
│           └── exportTask/
│               └── index.vue    (新增) 导出任务管理页面
└── ...
```