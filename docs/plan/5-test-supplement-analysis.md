# 测试补充分析报告

> 文档版本：v1.0 | 编写日期：2026-07-03 | 基于 RuoYi v3.9.2 (Spring Boot 4.0.6)

---

## 目录

1. [测试现状总览](#一测试现状总览)
2. [阶段一：设备工单 — 补充分析](#二阶段一设备工单--补充分析)
3. [阶段二：性能优化 — 补充分析](#三阶段二性能优化--补充分析)
4. [阶段三：安全增强 — 补充分析](#四阶段三安全增强--补充分析)
5. [阶段四：异常与异步 — 补充分析](#五阶段四异常与异步--补充分析)
6. [集成测试分析](#六集成测试分析)
7. [E2E 测试分析](#七e2e-测试分析)
8. [优先级与实施建议](#八优先级与实施建议)

---

## 一、测试现状总览

### 1.1 现有测试覆盖

| 维度 | 文件数 | 测试数 | 覆盖范围 |
|------|--------|--------|----------|
| 后端单元测试 | 5 | ~60+ | WorkOrderServiceImpl/Controller, DeviceInfoController, WorkOrderRecordController, RedisCacheAnnotation |
| 前端单元测试 | 5 | ~62 | API 请求、类型定义、工具函数 |
| 集成测试 | 0 | 0 | 无 |
| E2E 测试 | 0 | 0 | 无 |

### 1.2 测试缺口总览

| 类别 | 已有 | 待补充 | 建议补充数 |
|------|------|--------|-----------|
| 后端单元测试 | 5 类 | 10 类 | ~80-100 个 |
| 前端单元测试 | 5 类 | 3 类 | ~30-40 个 |
| 集成测试 | 0 | 8 项 | ~40-50 个 |
| E2E 测试 | 0 | 6 项 | ~20-30 个 |

---

## 二、阶段一：设备工单 — 补充分析

### 2.1 现有测试

| 文件 | 已有测试 | 覆盖情况 |
|------|----------|----------|
| WorkOrderServiceImplTest | 27 个 | 创建、分配、完成、归档、状态流转 |
| WorkOrderControllerTest | 15 个 | 列表、CRUD、批量分配、统计、导出 |
| WorkOrderRecordControllerTest | 7 个 | 记录 CRUD |
| DeviceInfoControllerTest | 9 个 | 设备 CRUD |
| 前端 workorder/order.test.ts | 15 个 | API 请求 |
| 前端 workorder/record.test.ts | 7 个 | API 请求 |
| 前端 device/info.test.ts | 6 个 | API 请求 |
| 前端 workorder.test.ts | 12 个 | 类型定义 |

### 2.2 待补充单元测试

| 测试目标 | 测试项 | 优先级 | 说明 |
|----------|--------|--------|------|
| `WorkOrderServiceImpl` - delete | 删除行为、级联约束 | P1 | 当前无删除测试 |
| `WorkOrderServiceImpl` - select | 列表查询条件组合、空结果、分页参数传递 | P1 | 当前仅有委托验证 |
| `WorkOrderRecordServiceImpl` | 新增/修改/删除/查询完整测试 | P1 | 当前完全无测试 |
| `DeviceInfoServiceImpl` | 设备 CRUD + 缓存注解交互 | P1 | 当前完全无测试 |
| `WorkOrderController` - 参数校验 | 无效参数、空参数、权限拒绝 | P2 | 当前仅有正常路径 |
| `WorkOrderController` - export | 空数据导出、异常导出 | P2 | 当前仅验证正常导出 |
| `WorkOrderStats` / `FaultTopDevice` | VO 对象构造、序列化 | P2 | 当前无测试 |
| 前端 - 页面组件 | Vue 组件渲染测试 | P2 | 需要 @vue/test-utils |

### 2.3 补充建议

```java
// WorkOrderServiceImplTest 补充
@Test
void shouldDeleteOrder() { ... }

@Test
void shouldSelectListWithPagination() { ... }

@Test
void shouldSelectListWithAllConditions() { ... }

// DeviceInfoServiceImplTest 新建
@Test
void shouldCreateDevice() { ... }
@Test
void shouldUpdateDeviceEvictCache() { ... }
```

---

## 三、阶段二：性能优化 — 补充分析

### 3.1 现有测试

| 文件 | 已有测试 | 覆盖情况 |
|------|----------|----------|
| RedisCacheAnnotationTest | 少量 | 仅注解行为基本验证 |

### 3.2 待补充单元测试

| 测试目标 | 测试项 | 优先级 | 说明 |
|----------|--------|--------|------|
| `RedisCacheAspect` - 缓存命中 | 第一次查询走方法，第二次命中缓存 | **P0** | 核心缓存逻辑 |
| `RedisCacheAspect` - 缓存过期 | TTL 后重新查询 | **P0** | 验证过期机制 |
| `RedisCacheAspect` - EVICT 操作 | 修改方法清除缓存 | **P0** | 验证缓存失效 |
| `RedisCacheAspect` - SpEL 解析 | 不同参数生成不同 Key | **P0** | 验证 Key 唯一性 |
| `RedisCacheAspect` - 模糊清除 | 修改后清除列表缓存 | P1 | 验证清除模式 |
| `RedisCacheAspect` - null 值处理 | 方法返回 null 时不写入缓存 | P1 | 防止空缓存 |
| `ExcelUtil.streamExportExcel` | 分批写入、SXSSF 刷新、空数据导出 | P1 | 流式导出核心方法 |
| `ExportTaskService` | 任务创建、状态更新、异常流程 | P1 | 异步导出服务 |
| `SysExportTask` Domain | 实体构造、状态枚举 | P2 | 简单值对象 |
| `CacheConstants` | 常量值正确性 | P2 | 回归防护 |
| 前端 - 导出 API | 异步导出接口、任务状态轮询 | P2 | 需要新接口测试 |

### 3.3 使用 Mockito + Embedded Redis 的建议

```java
// RedisCacheAspectTest
@ExtendWith(MockitoExtension.class)
class RedisCacheAspectTest {

    @Mock
    private RedisCache redisCacheTemplate;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private RedisCacheAspect aspect;

    @Test
    void shouldReturnCachedValue_whenCacheHit() throws Throwable {
        String key = "device_info:1";
        DeviceInfo cached = new DeviceInfo();
        when(redisCacheTemplate.getCacheObject(key)).thenReturn(cached);

        Object result = aspect.around(joinPoint, createCacheAnnotation(key));

        assertThat(result).isEqualTo(cached);
        verify(joinPoint, never()).proceed(); // 未执行原方法
    }

    @Test
    void shouldExecuteMethodAndCache_whenCacheMiss() throws Throwable {
        String key = "device_info:1";
        when(redisCacheTemplate.getCacheObject(key)).thenReturn(null);
        when(joinPoint.proceed()).thenReturn(new DeviceInfo());

        Object result = aspect.around(joinPoint, createCacheAnnotation(key));

        verify(joinPoint).proceed(); // 执行了原方法
        verify(redisCacheTemplate).setCacheObject(eq(key), any(), eq(600), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldEvictCache_whenActionIsEvict() throws Throwable {
        String key = "device_info:1";
        when(joinPoint.proceed()).thenReturn(1);

        aspect.around(joinPoint, createEvictAnnotation(key));

        verify(redisCacheTemplate).deleteObject(key);
    }
}
```

---

## 四、阶段三：安全增强 — 补充分析

### 4.1 现有测试

- **无**。阶段三的所有组件均无测试覆盖。

### 4.2 待补充单元测试

| 测试目标 | 测试项 | 优先级 | 说明 |
|----------|--------|--------|------|
| `DesensitizeAspect` - 脱敏逻辑 | 手机号/身份证/设备编码脱敏后格式正确 | **P0** | 核心脱敏逻辑 |
| `DesensitizeAspect` - 管理员豁免 | 管理员登录时不脱敏 | **P0** | 权限管控关键 |
| `DesensitizeAspect` - 列表脱敏 | List<?> 中每个元素都脱敏 | **P0** | 列表场景 |
| `DesensitizeAspect` - 嵌套字段 | `user.phone` 路径解析 | P1 | 嵌套对象 |
| `DesensitizeAspect` - 非 AjaxResult | 返回非 AjaxResult 时跳过 | P1 | 兼容性 |
| `EnhancedRepeatSubmitInterceptor` - DEFAULT | 相同参数返回重复 | **P0** | 防重核心 |
| `EnhancedRepeatSubmitInterceptor` - PARAM | 基于指定参数名的业务锁 | **P0** | PARAM 模式 |
| `EnhancedRepeatSubmitInterceptor` - 不同用户 | 不同用户相同参数不冲突 | **P0** | 隔离性 |
| `EnhancedRepeatSubmitInterceptor` - 过期释放 | interval 后自动释放锁 | P1 | 防死锁 |
| `TokenService` - 双 Token 签发 | createDualToken 返回 access + refresh | **P0** | 双 Token 核心 |
| `TokenService` - refreshAccessToken | refreshToken 有效时签发新 accessToken | **P0** | 续期核心 |
| `TokenService` - refreshToken 过期 | refreshToken 过期返回异常 | **P0** | 异常处理 |
| `TokenService` - 黑名单 | 登出后 token 被加入黑名单 | **P0** | 安全登出 |
| `TokenService` - 黑名单过期 | 黑名单在 JWT 过期后自动清理 | P1 | 资源释放 |
| `JwtAuthenticationTokenFilter` - 黑名单拦截 | 黑名单中 token 被拒绝 | P1 | 过滤器逻辑 |
| `MinIoUtils` - 上传 | multipartFile 上传到 MinIO | P1 | 需要 Mock MinIO |
| `MinIoUtils` - 删除 | 删除已存在的对象 | P1 | 需要 Mock MinIO |
| `SecurityFileUtils` - Magic Number | jpg/png/gif/pdf 各类型校验 | **P0** | 安全核心 |
| `SecurityFileUtils` - 伪装文件 | .exe 伪装为 .jpg 被拦截 | **P0** | 安全核心 |
| `SecurityFileUtils` - 路径穿越 | `../etc/passwd` 被拦截 | **P0** | 安全核心 |
| `SecurityFileUtils` - 空文件 | 空文件校验 | P2 | 边界情况 |
| `FileUploadStrategy` - local 策略 | strategy=local 调用 FileUploadUtils | P1 | 策略分发 |
| `FileUploadStrategy` - minio 策略 | strategy=minio 调用 MinIoUtils | P1 | 策略分发 |
| `FileUploadStrategy` - 安全校验 | 上传时调用 SecurityFileUtils.assertSecure | P1 | 链路完整性 |
| `MinIoConfig` | 配置属性绑定 | P2 | 配置正确性 |
| `@RepeatSubmit` 注解解析 | mode/lockParam 默认值 | P2 | 注解正确性 |
| `LimitMode` 枚举 | 枚举值正确 | P2 | 回归防护 |
| 前端 - Token 续期拦截器 | 401 → refresh → retry | P1 | Axios 拦截器逻辑 |
| 前端 - 脱敏效果 | 字段格式正确性 | P2 | 前端脱敏显示 |

### 4.3 安全增强测试特别注意点

```
安全测试三原则：
1. 不仅要测"能正常工作的路径"，更要测"被阻止的非法路径"
2. Magic Number 测试需要构造真实文件头数据（byte 数组）
3. 脱敏测试要区分管理员/非管理员两种身份
```

---

## 五、阶段四：异常与异步 — 补充分析

### 5.1 现有测试

- **无**。阶段四的所有组件均无测试覆盖。

### 5.2 待补充单元测试

| 测试目标 | 测试项 | 优先级 | 说明 |
|----------|--------|--------|------|
| `BizErrorCode` - 枚举完整性 | 所有 14 个枚举的 code/message 正确 | **P0** | 枚举基础 |
| `BizErrorCode` - code 唯一性 | 所有 code 不重复 | **P0** | 防止错误码冲突 |
| `BizException` - 构造 | 通过枚举构造异常，code/message 正确 | **P0** | 异常基础 |
| `BizException` - 自定义 message | 覆盖默认错误信息 | P1 | 灵活性验证 |
| `BizException` - 序列化 | serialVersionUID 兼容性 | P2 | 跨版本兼容 |
| `GlobalExceptionHandler` - BizException | 返回正确错误码 + INFO 级别日志 | **P0** | 异常处理核心 |
| `GlobalExceptionHandler` - 日志上下文 | 日志包含 uri/userId/username | **P0** | 排错关键 |
| `GlobalExceptionHandler` - 未登录场景 | 未登录时日志不报 NPE | **P0** | 边界情况 |
| `GlobalExceptionHandler` - ServiceException | 兼容性处理，返回原 code | P1 | 兼容性 |
| `GlobalExceptionHandler` - RuntimeException | 500 返回 + 堆栈打印 | P1 | 系统异常 |
| `ThreadPoolConfig` - Bean 创建 | threadPoolTaskExecutor 正确配置 | P1 | 线程池基础 |
| `ThreadPoolConfig` - 参数外部化 | 从 application.yml 读取参数 | P1 | 配置加载 |
| `ThreadPoolConfig` - 拒绝策略 | CallerRunsPolicy 生效 | P2 | 需要压力测试 |
| `AsyncWorkOrderService` - 异步执行 | 调用后立即返回，不阻塞 | P1 | 异步行为 |
| `AsyncWorkOrderService` - 异常处理 | 内部异常被捕获并记录日志 | P1 | 防止吞异常 |
| `WorkOrderServiceImpl` - BizException 替换 | 原 ServiceException 改为 BizException 后的行为 | P1 | 业务改造验证 |
| `WorkOrderServiceImpl` - 异步调用 | 创建工单时调用 AsyncWorkOrderService | P1 | 异步集成 |

### 5.3 核心测试代码示例

```java
// GlobalExceptionHandlerTest
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void shouldReturnBizErrorCode_whenBizException() {
        when(request.getRequestURI()).thenReturn("/workorder/order/1");

        BizException ex = new BizException(BizErrorCode.WORK_ORDER_NOT_FOUND);
        AjaxResult result = handler.handleBizException(ex, request);

        assertEquals(1001, result.get("code"));
        assertEquals("工单不存在", result.get("msg"));
    }

    @Test
    void shouldNotThrowNPE_whenNotLoggedIn() {
        when(request.getRequestURI()).thenReturn("/workorder/order/1");
        // 模拟未登录场景 - SecurityUtils.getLoginUser() 抛出异常

        BizException ex = new BizException(BizErrorCode.WORK_ORDER_NOT_FOUND);
        assertDoesNotThrow(() -> handler.handleBizException(ex, request));
    }
}
```

---

## 六、集成测试分析

### 6.1 需要补充的集成测试

| 测试项 | 测试内容 | 所需环境 | 优先级 | 估计用例数 |
|--------|----------|----------|--------|-----------|
| 异常体系全链路 | Controller → Service → BizException → GlobalExceptionHandler → 响应 | Spring Context | **P0** | 6-8 |
| 异步任务执行 | @Async 方法在线程池中执行 + CallerRunsPolicy 拒绝场景 | Spring Context | **P0** | 4-6 |
| 双 Token 全流程 | 登录 → accessToken → refresh → 新 accessToken → 登出 → 黑名单 | Spring Context + Redis | **P0** | 6-8 |
| 防重复提交全链路 | 重复请求被拦截、不同用户不冲突、PARAM 模式 | Spring Context + Redis | P1 | 4-6 |
| Redis 缓存全链路 | @RedisCache 在 Spring 上下文中缓存命中/失效 | Spring Context + Redis | **P0** | 4-6 |
| MinIO 上传集成 | 上传文件到 MinIO、验证文件可访问 | MinIO Server | P1 | 3-5 |
| 数据脱敏全链路 | @Desensitize 在 Controller 中生效、AjaxResult 脱敏效果 | Spring Context | P1 | 4-6 |
| Mapper SQL 集成 | MyBatis 动态 SQL 在 H2/MySQL 中正确执行 | H2/MySQL | P1 | 8-10 |
| 工单完整生命周期 | 创建 → 派单 → 接单 → 完成 → 归档，验证每一步状态 | Spring Context | P1 | 1 (场景式) |

### 6.2 集成测试技术方案

```java
// 使用 @SpringBootTest + @AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WorkOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnBizErrorCode_whenOrderNotFound() throws Exception {
        mockMvc.perform(get("/workorder/order/99999"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.code").value(1001)) // WORK_ORDER_NOT_FOUND
               .andExpect(jsonPath("$.msg").value("工单不存在"));
    }

    @Test
    void shouldCompleteFullWorkflow() throws Exception {
        // 创建工单 → 分配 → 完成 → 归档
        // 验证每一步的 status 字段
    }
}

// MinIO 集成测试（需要 Testcontainers）
@Testcontainers
class MinIoUtilsIntegrationTest {

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest")
        .withCommand("server /data")
        .withExposedPorts(9000);
}
```

### 6.3 集成测试环境清单

| 依赖 | 说明 | 替代方案 |
|------|------|----------|
| Spring Context | @SpringBootTest | - |
| Redis | @EnableRedis + Embedded Redis / Testcontainers | Jedis Mock |
| MySQL/H2 | 数据库连接 | H2 内存数据库 (兼容 MySQL SQL) |
| MinIO | 对象存储服务 | MockMinioClient / WireMock |
| JWT Key | token 签名密钥 | 测试密钥配置 |

---

## 七、E2E 测试分析

### 7.1 需要补充的 E2E 测试

| 测试场景 | 测试步骤 | 优先级 | 估计用例数 |
|----------|----------|--------|-----------|
| 工单完整交互流程 | 登录 → 进入工单列表 → 创建工单 → 派单 → 维修员接单 → 完成(上传图片+填写方案) → 管理员归档 → 验证状态 | **P0** | 8-10 |
| Token 过期续期 | 等待 accessToken 过期 → 触发续期 → 接口正常调用 → 登出 → 黑名单生效 | **P0** | 4-6 |
| 防重复提交 | 快速双击提交按钮 → 第二次被拦截 → 等待间隔后再次提交成功 | **P0** | 2-3 |
| 文件上传 | 上传合法文件（jpg/png/pdf）→ 上传伪装文件（exe 伪装 jpg）→ 验证成功/拒绝 | **P0** | 4-5 |
| 数据脱敏效果 | 管理员登录查看 → 非管理员登录查看 → 敏感字段格式不同 | P1 | 3-4 |
| 异步导出 | 提交导出任务 → 轮询任务状态 → 状态变为已完成 → 下载文件 | P1 | 3-4 |
| 统计看板 | 创建多个工单 → 验证看板数据 → 归档工单 → 看板刷新 | P1 | 3-4 |
| 批量分配 | 选择多个工单 → 批量分配维修员 → 验证各工单状态 | P1 | 2-3 |
| Excel 导出 | 导出工单列表 → 验证 Excel 文件格式和内容 | P2 | 2-3 |
| 缓存自动失效 | 修改设备信息 → 再次查询 → 返回新数据 | P2 | 2-3 |

### 7.2 E2E 测试技术选型

| 方案 | 说明 | 推荐度 |
|------|------|--------|
| **Playwright** | 支持 Chrome/Firefox/WebKit，录制回放，适合 Vue 3 应用 | ⭐⭐⭐⭐⭐ |
| **Cypress** | 生态成熟，社区支持好，但仅支持 Chrome | ⭐⭐⭐⭐ |
| **Selenium** | 最广泛，但配置复杂，速度慢 | ⭐⭐⭐ |

**推荐：Playwright**

### 7.3 E2E 测试示例（Playwright）

```typescript
// tests/e2e/workorder-full-lifecycle.spec.ts
import { test, expect } from '@playwright/test';

test.describe('工单完整生命周期', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'admin');
    await page.fill('[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/index');
  });

  test('管理员创建工单 → 派单 → 维修员完成 → 归档', async ({ page }) => {
    // 1. 进入工单列表
    await page.click('text=设备工单管理');
    await page.click('text=工单列表');
    await page.waitForSelector('.el-table');

    // 2. 创建新工单
    await page.click('text=新增');
    await page.fill('[name="faultDesc"]', '测试故障描述');
    // ... 填写表单
    await page.click('.el-dialog .el-button--primary >> text=确 定');
    await page.waitForResponse(/\/workorder\/order\/list/);

    // 3. 验证工单出现在列表中，状态为未派单
    await expect(page.locator('text=未派单')).toBeVisible();
  });

  test('双击提交触防重复提交', async ({ page }) => {
    await page.click('text=新增');
    await page.fill('[name="faultDesc"]', '防重测试');
    // 快速双击
    await page.click('.el-dialog .el-button--primary >> text=确 定');
    await page.click('.el-dialog .el-button--primary >> text=确 定');
    // 验证提示消息
    await expect(page.locator('text=不允许重复提交')).toBeVisible();
  });
});
```

---

## 八、优先级与实施建议

### 8.1 实施路线图

```
第一优先级（P0）—— 核心功能稳定性（约 50-60 个测试）
├── 后端单元测试
│   ├── RedisCacheAspect 缓存逻辑      (5 个)
│   ├── DesensitizeAspect 脱敏逻辑      (5 个)
│   ├── EnhancedRepeatSubmitInterceptor (4 个)
│   ├── TokenService 双Token            (6 个)
│   ├── SecurityFileUtils 安全校验       (6 个)
│   ├── GlobalExceptionHandler 异常处理  (4 个)
│   ├── BizErrorCode 枚举               (2 个)
│   └── BizException 构造               (2 个)
├── 集成测试
│   ├── 异常体系全链路                   (6 个)
│   ├── 异步任务执行                     (4 个)
│   └── 双 Token 全流程                  (6 个)
└── E2E 测试
    ├── 工单完整交互流程                 (8 个)
    ├── Token 过期续期                  (4 个)
    ├── 防重复提交                      (2 个)
    └── 文件上传安全                    (4 个)

第二优先级（P1）—— 功能完整性（约 40-50 个测试）
├── 后端单元测试
│   ├── ExcelUtil.streamExportExcel     (3 个)
│   ├── ExportTaskService                (3 个)
│   ├── MinIoUtils                       (4 个)
│   ├── FileUploadStrategy              (3 个)
│   ├── AsyncWorkOrderService           (3 个)
│   ├── ThreadPoolConfig                (3 个)
│   ├── DeviceInfoServiceImpl           (4 个)
│   └── 前端 Axios 拦截器               (3 个)
├── 集成测试
│   ├── Redis 缓存全链路                 (4 个)
│   ├── Mapper SQL 集成                 (8 个)
│   ├── MinIO 上传集成                  (3 个)
│   └── 工单完整生命周期                 (1 个)
└── E2E 测试
    ├── 数据脱敏效果                    (3 个)
    ├── 异步导出                        (3 个)
    └── 统计看板                        (3 个)

第三优先级（P2）—— 边界与兼容性（约 20-30 个测试）
├── 后端单元测试（边界情况）
├── 前端组件测试（Vue Test Utils）
└── E2E 测试（Excel 导出验证、缓存失效）
```

### 8.2 测试环境搭建

```bash
# 1. 后端测试依赖（pom.xml 中已有 spring-boot-starter-test）
#    新增嵌入式 Redis 依赖
<dependency>
    <groupId>it.ozimov</groupId>
    <artifactId>embedded-redis</artifactId>
    <version>0.7.3</version>
    <scope>test</scope>
</dependency>

# 2. 前端 E2E 依赖
npm install -D @playwright/test
npx playwright install chromium

# 3. 前端组件测试依赖
npm install -D @vue/test-utils happy-dom
```

### 8.3 测试执行脚本

```bash
# 后端单元测试（特定模块）
mvn test -pl ruoyi-framework -Dtest="com.ruoyi.framework.aspectj.*Test"
mvn test -pl ruoyi-workorder -Dtest="com.ruoyi.workorder.*Test"
mvn test -pl ruoyi-common -Dtest="com.ruoyi.common.exception.*Test"

# 后端集成测试
mvn test -pl ruoyi-admin -Dtest="com.ruoyi.*IntegrationTest"
mvn test -pl ruoyi-admin -Dtest="com.ruoyi.*IntegrationTest" \
  -Dspring.profiles.active=test

# 前端单元测试
cd ruoyi && npx vitest run

# 前端 E2E 测试
cd ruoyi && npx playwright test

# 全部测试（后端+前端）
cd ruoyi-backend && mvn test && cd ../ruoyi && npx vitest run
```

### 8.4 验收标准

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 后端单元测试数 | ≥ 100 个 | 覆盖全部扩展组件 |
| 前端单元测试数 | ≥ 80 个 | 覆盖 API + 组件 + 工具 |
| 集成测试数 | ≥ 30 个 | 覆盖核心全链路 |
| E2E 测试数 | ≥ 15 个 | 覆盖关键用户场景 |
| 测试覆盖率（后端） | ≥ 70% | 行覆盖率 + 分支覆盖率 |
| 测试覆盖率（前端） | ≥ 60% | 行覆盖率 |
| 构建耗时 | ≤ 5 分钟 | 测试+编译总时间 |

### 8.5 风险与注意事项

| 风险 | 说明 | 缓解措施 |
|------|------|----------|
| 集成测试需要外部服务 | Redis/MinIO 不可用 | 使用 Testcontainers 自动管理容器 |
| 异步测试执行不稳定 | @Async 导致竞态条件 | 使用 CountDownLatch / Awaitility |
| 脱敏测试依赖 SecurityUtils | 需要 Mock 登录上下文 | 使用 @WithMockUser / SecurityContextHolder |
| MinIO 测试需要真实文件头 | Magic Number 校验需要构造 byte | 测试资源目录准备真实文件 |
| E2E 测试环境依赖 | 需要部署完整前后端 | 配置 Docker Compose 测试环境 |
| 测试数据污染 | 测试执行后留下脏数据 | @Transactional / @BeforeEach 清理 |