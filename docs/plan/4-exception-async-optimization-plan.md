# 异常体系统一与异步任务改造设计方案

## 1. 需求概览

### 1.1 统一业务异常体系改造
当前若依异常体系存在分散问题：

**现状分析：**
- 异常类分散：`ServiceException` + `BaseException` + `UserException` + `FileException` + 各种细分异常，继承体系混乱
- 无统一业务错误枚举：业务错误（如工单不存在、状态不允许操作等）都是硬编码 `throw new ServiceException("xxx")`，没有标准化错误码
- 日志信息不完整：现有 `GlobalExceptionHandler` 只打印了错误信息和堆栈，但没有记录请求参数、当前用户、接口路径等排错必需信息
- 返回码不规范：前端无法根据标准化错误码做差异化处理

**改造目标：**
- 创建统一业务错误码枚举 `BizErrorCode`，涵盖常见业务场景（工单相关、库存相关、权限相关等）
- 统一业务异常类 `BizException`，使用枚举构造
- 增强 `GlobalExceptionHandler`，统一区分：
  - 业务异常：返回标准化 `{code: 错误码, msg: 错误信息}`，不打印完整堆栈（只打印info级别）
  - 系统异常：返回固定 500，打印完整堆栈 + 请求信息（用户、路径、参数）方便排错
- 异常日志增加上下文信息：请求URI、用户ID、用户名、请求参数

### 1.2 异步任务改造

**现状分析：**
- 若依已有 `ThreadPoolConfig` 配置了 `threadPoolTaskExecutor`（核心 50，最大 200，队列 1000）和 `scheduledExecutorService`
- 已有 `AsyncManager` 使用 `ScheduledExecutorService` 处理异步日志
- **问题：** 业务代码中一些耗时操作（导出、消息推送、统计）仍然是同步执行，会阻塞接口响应
  - 例如 `WorkOrderServiceImpl.pushUrgentNotice()` 是同步调用 `noticeService.insertNotice()`，阻塞主流程
  - 报表统计、大数据量导出会让前端等待很久
- `@Async` 注解默认使用Spring自动配置的线程池，没有自定义参数配置

**改造目标：**
- 启用 Spring `@EnableAsync` 开启注解异步支持
- 优化自定义线程池配置，参数外部化可配置
- 将工单消息推送、报表统计、操作日志记录等非核心流程改造成异步执行
- 配置合理的拒绝策略，防止任务堆积压垮服务

---

## 2. 现有代码分析

### 2.1 异常体系现状

| 类型 | 现状 | 问题 |
|------|------|------|
| ServiceException | 通用业务异常，支持code+message | 没有预定义枚举，到处硬编码 |
| BaseException | 基础异常，支持i18n国际化 | 继承体系复杂，业务代码很少用 |
| UserException/FileException | 细分模块异常 | 继承BaseException，每个模块一个子类，扩展麻烦 |
| GlobalExceptionHandler | 已经统一捕获 | 日志缺少请求上下文信息，业务异常和系统异常日志级别相同 |

**业务代码当前写法示例（来自 WorkOrderServiceImpl）：**

```java
if (order == null) {
    throw new ServiceException("工单不存在");
}
if (!"2".equals(order.getOrderStatus())) {
    throw new ServiceException("仅维修中的工单可以完成");
}
```

问题：
1. 没有错误码，前端无法区分是哪种业务错误
2. 错误信息散布在各处，统一修改困难
3. 不便于国际化（i18n）改造

### 2.2 异步体系现状

| 组件 | 现状 |
|------|------|
| ThreadPoolConfig | 已存在，硬编码参数：corePoolSize=50, maxPoolSize=200, queueCapacity=1000，拒绝策略 CallerRunsPolicy |
| AsyncManager | 使用 ScheduledExecutorService 处理日志异步，接口是 `execute(TimerTask)`，使用不方便 |
| @EnableAsync | 需要确认是否已开启（若依原生默认不开启@EnableAsync） |
| 业务异步 | 工单通知推送 `pushUrgentNotice()` 是同步执行，阻塞主流程 |

**当前同步问题示例：**
```java
// 插入工单后同步推送通知，阻塞主流程
if ("2".equals(workOrder.getUrgencyLevel()) || "3".equals(workOrder.getUrgencyLevel())) {
    pushUrgentNotice(workOrder);  // 同步执行，插入数据库，阻塞
}
```

---

## 3. 改造方案设计

### 3.1 统一业务异常体系

#### 3.1.1 新增业务错误枚举 `BizErrorCode`

**文件位置：**  
`/workspace/ruoyi-backend/ruoyi-common/src/main/java/com/ruoyi/common/enums/BizErrorCode.java`

**设计：**
- 枚举结构：`code` (int 错误码) + `message` (String 默认错误信息)
- 错误码分段规则：
  - 1xxx: 工单相关错误
  - 2xxx: 设备相关错误
  - 3xxx: 库存相关错误
  - 4xxx: 文件上传相关错误
  - 5xxx: 通用业务错误

**枚举定义示例：**

```java
public enum BizErrorCode {

    // ========== 1xxx 工单相关 ==========
    WORK_ORDER_NOT_FOUND(1001, "工单不存在"),
    WORK_ORDER_STATUS_INVALID(1002, "工单当前状态不允许此操作"),
    WORK_ORDER_REPAIR_SOLUTION_EMPTY(1003, "请填写维修方案"),
    WORK_ORDER_IMAGE_EMPTY(1004, "请上传至少一张维修图片"),
    WORK_ORDER_NOT_COMPLETED(1005, "仅已完成的工单可以归档"),

    // ========== 2xxx 设备相关 ==========
    DEVICE_NOT_FOUND(2001, "设备不存在"),
    DEVICE_CODE_DUPLICATE(2002, "设备编号已存在"),

    // ========== 3xxx 库存相关 ==========
    STOCK_NOT_ENOUGH(3001, "库存不足"),
    STOCK_ITEM_NOT_FOUND(3002, "库存物料不存在"),

    // ========== 4xxx 文件上传 ==========
    FILE_UPLOAD_FAILED(4001, "文件上传失败"),
    FILE_SECURITY_CHECK_FAILED(4002, "文件安全校验未通过"),

    // ========== 5xxx 通用 ==========
    PARAMETER_INVALID(5001, "参数不合法"),
    OPERATION_DENIED(5002, "操作被拒绝"),
    ;

    private final int code;
    private final String message;

    BizErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
```

#### 3.1.2 统一业务异常 `BizException`

**文件位置：**  
`/workspace/ruoyi-backend/ruoyi-common/src/main/java/com/ruoyi/common/exception/BizException.java`

**设计：**
- 继承 `RuntimeException`
- 持有 `BizErrorCode` 引用
- 支持覆盖默认错误信息

```java
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final BizErrorCode errorCode;

    public BizException(BizErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BizException(BizErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public int getCode() {
        return code;
    }

    public BizErrorCode getErrorCode() {
        return errorCode;
    }
}
```

**兼容性说明：**  
保留 `ServiceException`，新代码使用 `BizException`，旧代码逐步迁移，不强制删除。

#### 3.1.3 增强 `GlobalExceptionHandler`

**文件位置：**  
`/workspace/ruoyi-backend/ruoyi-framework/src/main/java/com/ruoyi/framework/web/exception/GlobalExceptionHandler.java`

**改造点：**

1. **新增 `BizException` 处理器**：
   - 业务异常只打印 `info` 级别日志（因为业务异常是预期内的，不需要打ERROR级堆栈）
   - 日志包含：错误码、错误信息、请求信息
   - 返回标准化 `{code: 业务错误码, msg: 错误信息}`

2. **增强所有异常处理器的日志**：
   - 统一记录请求URI、用户ID（如果已登录）、请求参数
   - 格式：`[业务异常] code={}, uri={}, userId={}, msg={}`

**增强后的处理逻辑：**

| 异常类型 | 日志级别 | 返回码 |
|----------|----------|--------|
| BizException | INFO | 业务错误码（枚举中的code） |
| ServiceException | ERROR（保持兼容） | 原code |
| 其他RuntimeException | ERROR | 500 |
| Exception | ERROR | 500 |

**关键代码：**

```java
/**
 * 业务异常（统一枚举）处理
 */
@ExceptionHandler(BizException.class)
public AjaxResult handleBizException(BizException e, HttpServletRequest request) {
    String requestURI = request.getRequestURI();
    Long userId = null;
    String username = null;
    try {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            userId = loginUser.getUserId();
            username = loginUser.getUsername();
        }
    } catch (Exception ignored) {
        // 未登录场景，忽略
    }
    // 业务异常打印info级别，包含上下文信息
    log.info("[业务异常] code={}, uri={}, userId={}, username={}, msg={}",
            e.getCode(), requestURI, userId, username, e.getMessage());
    return AjaxResult.error(e.getCode(), e.getMessage());
}
```

3. **增强系统异常日志**：在处理 `RuntimeException` 和 `Exception` 时，同样记录请求上下文

### 3.1.4 业务代码改造示例

**改造前：**
```java
if (order == null) {
    throw new ServiceException("工单不存在");
}
if (!"2".equals(order.getOrderStatus())) {
    throw new ServiceException("仅维修中的工单可以完成");
}
```

**改造后：**
```java
if (order == null) {
    throw new BizException(BizErrorCode.WORK_ORDER_NOT_FOUND);
}
if (!"2".equals(order.getOrderStatus())) {
    throw new BizException(BizErrorCode.WORK_ORDER_STATUS_INVALID);
}
```

优点：
- 错误码统一，前端可识别
- 错误信息集中管理，便于统一修改和国际化
- 代码更简洁

---

### 3.2 异步任务改造

#### 3.2.1 开启 `@EnableAsync`

**文件位置：**  
`/workspace/ruoyi-backend/ruoyi-framework/src/main/java/com/ruoyi/framework/config/ThreadPoolConfig.java`

在配置类上添加 `@EnableAsync` 注解，开启Spring注解驱动的异步支持。

#### 3.2.2 线程池配置外部化

改造 `ThreadPoolConfig`，将核心参数从硬编码改为从 `application.yml` 读取：

```yaml
# application.yml 新增配置
spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 100
        keep-alive: 60s
```

或者在ruoyi配置下新增：

```yaml
ruoyi:
  async:
    core-pool-size: 10
    max-pool-size: 50
    queue-capacity: 100
    keep-alive-seconds: 300
```

**修改 `ThreadPoolConfig`：**

```java
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Value("${ruoyi.async.core-pool-size:10}")
    private int corePoolSize;

    @Value("${ruoyi.async.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${ruoyi.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${ruoyi.async.keep-alive-seconds:300}")
    private int keepAliveSeconds;

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 拒绝策略：调用者线程执行，防止任务丢失，避免压垮服务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 设置线程名称前缀，方便排查问题
        executor.setThreadNamePrefix("async-ruoyi-");
        return executor;
    }
}
```

**参数说明：**
- 核心线程数：10（业务系统异步任务不会特别多，默认10足够）
- 最大线程数：50（峰值可扩展到50）
- 队列容量：100（队列满了才会创建新线程，防止无限队列OOM）
- 拒绝策略：`CallerRunsPolicy`（调用者线程执行，既不丢弃也不抛出异常，起到背压作用）

#### 3.2.3 业务异步改造

**改造范围：**

| 改造点 | 原方式 | 新方式 | 文件位置 |
|--------|--------|--------|----------|
| 紧急工单通知推送 | 同步 | `@Async` 异步 | WorkOrderServiceImpl.pushUrgentNotice() |
| 操作日志记录 | AsyncManager(TimerTask) | 可保留，也可改用 `@Async` | LogAspect |
| 工单统计报表导出 | 同步 | 异步+任务记录 | 新增异步导出方法 |
| 工单统计数据刷新 | 同步 | `@Async` 异步 | WorkOrderServiceImpl |

**改造示例 - 工单通知推送：**

```java
/**
 * 推送紧急工单通知（异步执行）
 */
@Async("threadPoolTaskExecutor")
public void pushUrgentNotice(WorkOrder workOrder) {
    SysNotice notice = new SysNotice();
    notice.setNoticeTitle("紧急工单：" + workOrder.getOrderNo());
    notice.setNoticeType("1");
    notice.setNoticeContent("有新的紧急工单 " + workOrder.getOrderNo()
            + " 需要处理，故障描述：" + workOrder.getFaultDesc()
            + "，请及时派单。");
    notice.setStatus("0");
    notice.setCreateBy("system");
    noticeService.insertNotice(notice);
}
```

**调用方：**

```java
// 插入工单成功后，异步推送通知，不阻塞主流程
if ("2".equals(workOrder.getUrgencyLevel()) || "3".equals(workOrder.getUrgencyLevel())) {
    workOrderService.pushUrgentNotice(workOrder);  // 异步，立即返回
}
```

**改造示例 - 异步报表统计：**

```java
/**
 * 异步刷新工单统计数据
 */
@Async("threadPoolTaskExecutor")
public void asyncRefreshStats() {
    // 执行统计查询
    WorkOrderStats stats = workOrderMapper.selectWorkOrderStats();
    // 写入缓存
    redisCache.setCacheObject(CacheConstants.WORKORDER_STATS_KEY + "dashboard", stats, 600, TimeUnit.SECONDS);
}
```

#### 3.2.4 异常处理

异步任务中抛出的异常不会被 `GlobalExceptionHandler` 捕获，需要：
- 在异步方法内部捕获异常并记录日志
- 重要任务可以记录失败日志到数据库

```java
@Async("threadPoolTaskExecutor")
public void pushUrgentNotice(WorkOrder workOrder) {
    try {
        // ... 推送逻辑
    } catch (Exception e) {
        log.error("推送紧急工单通知失败，orderId={}, orderNo={}",
                workOrder.getOrderId(), workOrder.getOrderNo(), e);
        // 可以选择记录到数据库或者告警
    }
}
```

---

## 4. 实施路线图

### 阶段一：统一业务异常体系（5个子任务）

| 序号 | 任务 | 文件 | 类型 |
|------|------|------|------|
| 1.1 | 创建 `BizErrorCode` 枚举 | ruoyi-common | 新建 |
| 1.2 | 创建 `BizException` 业务异常 | ruoyi-common | 新建 |
| 1.3 | 增强 `GlobalExceptionHandler` 处理 `BizException` 并增加上下文日志 | ruoyi-framework | 修改 |
| 1.4 | 在 `application.yml` 增加默认配置 | ruoyi-admin | 修改 |
| 1.5 | 改造工单模块业务代码使用新异常 | ruoyi-workorder | 修改 |

### 阶段二：异步任务改造（4个子任务）

| 序号 | 任务 | 文件 | 类型 |
|------|------|------|------|
| 2.1 | 在 `ThreadPoolConfig` 添加 `@EnableAsync` 并配置外部化参数 | ruoyi-framework | 修改 |
| 2.2 | 在 `application.yml` 增加异步线程池配置 | ruoyi-admin | 修改 |
| 2.3 | 改造 `WorkOrderServiceImpl.pushUrgentNotice` 为异步 | ruoyi-workorder | 修改 |
| 2.4 | （可选）改造其他耗时操作（统计、导出）为异步 | 各模块 | 修改 |

### 验收标准

1. **异常体系**
   - 抛出业务异常时，返回正确的错误码，日志格式包含请求上下文
   - 系统异常时，日志包含完整请求信息，返回500错误码
   - 原有 `ServiceException` 仍然能正常工作，向后兼容

2. **异步任务**
   - `@Async` 注解生效，异步任务使用自定义线程池
   - 推送通知不阻塞主接口，响应时间明显缩短
   - 线程池满时有合理的拒绝策略，不会导致服务崩溃

---

## 5. 风险与兼容性分析

| 风险点 | 影响 | 应对措施 |
|--------|------|----------|
| 原有代码使用 `ServiceException` | 无影响 | 保留原有处理逻辑，新代码使用 `BizException`，逐步迁移 |
| `@EnableAsync` 代理问题 | 同类内部调用异步方法不生效 | 文档说明，建议将异步方法抽到单独的AsyncService |
| 线程池参数不合理 | 可能导致资源争用 | 参数外部化，可通过配置文件调整 |
| 异步任务异常吞掉 | 出错无法发现 | 要求异步方法内部必须try-catch并打错误日志 |

---

## 6. 总结

本次改造完成后：

1. **异常体系**：业务错误有统一枚举，错误码标准化，日志包含完整上下文信息，便于线上排错
2. **异步改造**：耗时操作异步化，接口响应更快，用户体验更好，线程池有合理的容量和拒绝策略，保证稳定性
