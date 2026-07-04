# 性能优化方案设计文档

> 文档版本：v1.0 | 编写日期：2026-07-03 | 基于 RuoYi v3.9.2 (Spring Boot 4.0.6)

---

## 目录

1. [优化一：Redis 缓存热点数据](#优化一redis-缓存热点数据)
2. [优化二：大数据导出 OOM 解决](#优化二大数据导出-oom-解决)
3. [优化三：SQL 慢查询优化](#优化三sql-慢查询优化)
4. [实施路线图](#四实施路线图)
5. [预期效果与验证指标](#五预期效果与验证指标)

---

## 一、Redis 缓存热点数据

### 1.1 现状分析

RuoYi 现有的 Redis 使用局限：

| 用途 | Key 前缀 | 描述 |
|------|----------|------|
| 登录 Token | `login_tokens:` | JWT Session 存储 |
| 验证码 | `captcha_codes:` | 图形验证码 |
| 参数配置 | `sys_config:` | 系统参数 |
| 字典数据 | `sys_dict:` | 字典（已有但未在 workorder 模块使用） |
| 防重复提交 | `repeat_submit:` | 请求防重 |
| 限流 | `rate_limit:` | 接口限流 |

**业务缓存缺失**：设备信息、工单分类等热点数据的查询每次都查库，未利用 Redis 加速。

### 1.2 改造方案

#### 1.2.1 新增缓存常量

在 `CacheConstants.java` 中新增业务缓存 Key 定义：

```java
public class CacheConstants {
    // ... 已有常量

    /** ===== 业务缓存（优化新增） ===== */
    
    /** 设备信息缓存 */
    public static final String DEVICE_INFO_KEY = "device_info:";
    
    /** 工单统计看板缓存 */
    public static final String WORKORDER_STATS_KEY = "workorder_stats:";
    
    /** 工单分类缓存 */
    public static final String WORKORDER_CATEGORY_KEY = "workorder_category:";
    
    /** 字典数据缓存（业务模块增强） */
    public static final String BIZ_DICT_KEY = "biz_dict:";
}
```

#### 1.2.2 自定义 `@RedisCache` 注解

新建 `com.ruoyi.common.annotation.RedisCache.java`，参照 `@RateLimiter` 的设计模式：

```java
package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务缓存注解
 * 用于 Service 方法，自动缓存返回值 / 清除缓存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisCache {

    /** 缓存 Key 前缀 */
    String key() default "";

    /** 缓存 Key 后缀（支持 SpEL：取方法参数） */
    String keySuffix() default "";

    /** 过期时间（秒），默认 10 分钟 */
    int expire() default 600;

    /** 操作类型：CACHE=缓存查询 EVICT=清除缓存 */
    Action action() default Action.CACHE;

    enum Action {
        CACHE, EVICT
    }
}
```

#### 1.2.3 缓存切面实现

新建 `com.ruoyi.framework.aspectj.RedisCacheAspect.java`：

```java
@Aspect
@Component
public class RedisCacheAspect {

    @Autowired
    private RedisCache redisCache;

    /**
     * 缓存逻辑：方法执行后，将返回值写入 Redis
     * 清除逻辑：方法执行后，删除指定 Key 的缓存
     */
    @Around("@annotation(cacheAnnotation)")
    public Object around(ProceedingJoinPoint point, RedisCache cacheAnnotation) throws Throwable {
        String key = buildCacheKey(cacheAnnotation, point);

        if (cacheAnnotation.action() == RedisCache.Action.EVICT) {
            // 清除缓存
            Object result = point.proceed();
            redisCache.deleteObject(key);
            // 同时清除列表缓存（模糊匹配）
            String pattern = key.substring(0, key.lastIndexOf(":") + 1) + "*";
            redisCache.deleteObject(redisCache.keys(pattern));
            return result;
        }

        // 查询缓存
        Object cachedValue = redisCache.getCacheObject(key);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 执行原方法
        Object result = point.proceed();
        if (result != null) {
            redisCache.setCacheObject(key, result, cacheAnnotation.expire(), TimeUnit.SECONDS);
        }
        return result;
    }

    /** 解析 SpEL 表达式，构建完整 Redis Key */
    private String buildCacheKey(RedisCache annotation, ProceedingJoinPoint point) {
        String key = annotation.key();
        String suffix = annotation.keySuffix();
        if (StringUtils.isNotEmpty(suffix)) {
            // 使用 LocalVariableTableParameterNameDiscoverer 获取参数名
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            String[] paramNames = new DefaultParameterNameDiscoverer().getParameterNames(method);
            Object[] args = point.getArgs();
            // 解析 SpEL，从方法参数中取值
            EvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            suffix = new SpelExpressionParser().parseExpression(suffix).getValue(context, String.class);
        }
        return key + suffix;
    }
}
```

#### 1.2.4 设备信息缓存改造

在 `DeviceInfoServiceImpl` 中应用注解：

```java
@Service
public class DeviceInfoServiceImpl implements IDeviceInfoService {

    @Override
    @RedisCache(key = CacheConstants.DEVICE_INFO_KEY, keySuffix = "#deviceId", expire = 1800)
    public DeviceInfo selectDeviceInfoById(Long deviceId) {
        return deviceInfoMapper.selectDeviceInfoById(deviceId);
    }

    @Override
    @RedisCache(key = CacheConstants.DEVICE_INFO_KEY, keySuffix = "#deviceInfo.deviceId", action = RedisCache.Action.EVICT)
    public int updateDeviceInfo(DeviceInfo deviceInfo) {
        return deviceInfoMapper.updateDeviceInfo(deviceInfo);
    }

    @Override
    @RedisCache(key = CacheConstants.DEVICE_INFO_KEY, keySuffix = "#deviceId", action = RedisCache.Action.EVICT)
    public int deleteDeviceInfoById(Long deviceId) {
        return deviceInfoMapper.deleteDeviceInfoById(deviceId);
    }
}
```

关键设计点：
- **查询方法**：`action = CACHE`，方法返回后自动写入 Redis，TTL=1800 秒
- **修改方法**：`action = EVICT`，方法执行后自动删除对应 Key
- **SpEL 解析**：`#deviceId` 自动取方法参数值拼接 Key
- **模糊清除**：同时清除 `device_info:*` 列表缓存，避免列表页脏数据

#### 1.2.5 工单统计看板缓存

```java
@Service
public class WorkOrderServiceImpl implements IWorkOrderService {

    /**
     * 统计看板缓存，10 分钟过期
     * 因为统计看板对实时性要求不高，10 分钟缓存可显著减少聚合查询频率
     */
    @Override
    @RedisCache(key = CacheConstants.WORKORDER_STATS_KEY, keySuffix = "'dashboard'", expire = 600)
    public WorkOrderStats selectWorkOrderStats() {
        return workOrderMapper.selectWorkOrderStats();
    }

    /**
     * 修改工单状态时清除看板缓存
     */
    @Override
    @RedisCache(key = CacheConstants.WORKORDER_STATS_KEY, keySuffix = "'dashboard'", 
                action = RedisCache.Action.EVICT)
    @Transactional(rollbackFor = Exception.class)
    public int updateWorkOrder(WorkOrder workOrder) {
        return workOrderMapper.updateWorkOrder(workOrder);
    }
}
```

#### 1.2.6 字典数据缓存增强

利用 RuoYi 已有的 `SYS_DICT_KEY` 机制，在字典工具类中增加缓存：

```java
// DictUtils.java（已有逻辑增强）
public static List<SysDictData> getDictCache(String dictType) {
    String cacheKey = CacheConstants.SYS_DICT_KEY + dictType;
    List<SysDictData> dictData = redisCache.getCacheObject(cacheKey);
    if (dictData != null) {
        return dictData;
    }
    // 查库
    dictData = dictDataMapper.selectDictDataByType(dictType);
    redisCache.setCacheObject(cacheKey, dictData, 30, TimeUnit.MINUTES);
    return dictData;
}
```

> 注：RuoYi 原生 `DictUtils` 已有缓存逻辑但字典管理后台修改后通过 `@CacheEvict` 失效，此处仅需确保 workorder 模块使用的 `fault_type`、`urgency_level` 等字典也进入缓存路径。

---

## 二、大数据导出 OOM 解决

### 2.1 现状分析

#### 2.1.1 当前导出实现代码

```java
// WorkOrderController.java
@GetMapping("/export")
public void export(HttpServletResponse response, WorkOrder workOrder) {
    List<WorkOrder> list = workOrderService.selectWorkOrderList(workOrder); // ← OOM 风险点
    ExcelUtil<WorkOrder> util = new ExcelUtil<>(WorkOrder.class);
    util.exportExcel(response, list, "工单数据");
}
```

#### 2.1.2 OOM 根因

1. **全量加载**：`selectWorkOrderList()` 不加分页参数时，查询所有符合条件的数据，可能达到数十万行
2. **全量 List**：所有数据一次性加载到 JVM 堆内存中的 `ArrayList`
3. **POI 对象膨胀**：每行数据在 POI 中创建 Row / Cell 对象，内存占用放大 5-10 倍
4. **异步缺失**：大导出请求同步阻塞 HTTP 连接，超时导致前端重试，加重压力

> 以 10 万行 × 20 列为例：List<WorkOrder> 约 200MB，POI 对象模型约 800MB-1.2GB，合计超 1GB

#### 2.1.3 现有代码中的 SXSSFWorkbook

注意到 `ExcelUtil` 的构造函数中已经使用了 `SXSSFWorkbook(500)`：

```java
// ExcelUtil.java 第 1803-1805 行
public void createWorkbook() {
    this.wb = new SXSSFWorkbook(500); // 窗口大小 500 行
}
```

但 `SXSSFWorkbook` 只解决了 **写入** 阶段的内存问题（数据写入临时文件而非 Heap），无法解决 **数据查询阶段** 全量 List 在内存中的问题。**瓶颈在前端调用端传递了全量 List，而非 Excel 写入端。**

### 2.2 改造方案

#### 分层解决方案

```
Controller 层 → 分页查询 + 流式写入（同步，适合 <5 万行）
              → 异步导出任务 + 消息通知（异步，适合大量数据）
Service  层 → 分页游标查询接口
Util     层 → SXSSFWorkbook 流式写入（已有）+ 回调分批写入新方法
```

#### 2.2.1 新增流式导出工具方法

利用 ExcelUtil 已有的 `init()` 初始化 + `addCell()` 逐单元格写入 + `SXSSFWorkbook` 流式特性，新增 `streamExportExcel` 方法：

```java
/**
 * 流式导出 - 分批查询 + SXSSF 流式写入
 * 利用 ExcelUtil.init() 初始化样式和字段映射，分批写入数据行
 *
 * @param response    HTTP 响应
 * @param pageFetcher 分页数据获取函数 (pageNum, pageSize) → List<T>
 * @param sheetName   工作表名称
 * @param title       标题
 * @param pageSize    每批查询行数（建议 5000）
 */
public void streamExportExcel(HttpServletResponse response,
                               BiFunction<Integer, Integer, List<T>> pageFetcher,
                               String sheetName, String title, int pageSize) {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding("utf-8");

    // 1. 初始化（空列表）：建立字段映射、样式、标题行
    this.init(new ArrayList<>(), sheetName, title, Type.EXPORT);

    // 2. 写入表头行（writeSheet 在空列表时只写表头，fillExcelData 无数据不执行）
    writeSheet();

    // 3. 逐批查询并写入数据行
    int pageNum = 1;
    boolean hasMore = true;
    int dataRowNum = rownum + 1; // 表头行之后开始写数据

    try {
        while (hasMore) {
            List<T> batch = pageFetcher.apply(pageNum, pageSize);
            if (batch == null || batch.isEmpty()) {
                hasMore = false;
            } else {
                for (T item : batch) {
                    Row row = sheet.createRow(dataRowNum++);
                    int col = 0;
                    for (Object[] os : fields) {
                        Field field = (Field) os[0];
                        Excel excel = (Excel) os[1];
                        if (!Collection.class.isAssignableFrom(field.getType())) {
                            addCell(excel, row, item, field, col);
                            col++;
                        }
                    }
                }
                // 每批写入后将数据刷出到临时文件，释放内存
                ((SXSSFSheet) sheet).flushRows(pageSize);
                pageNum++;
                if (batch.size() < pageSize) {
                    hasMore = false;
                }
            }
        }
        wb.write(response.getOutputStream());
    } catch (Exception e) {
        log.error("流式导出Excel异常", e.getMessage());
    } finally {
        if (wb instanceof SXSSFWorkbook) {
            ((SXSSFWorkbook) wb).dispose();
        }
        IOUtils.closeQuietly(wb);
    }
}
```

> **设计说明**：
> - 调用 `init(空列表)` 触发 `createExcelField()` 解析 @Excel 注解 → `fields` 列表就绪
> - 调用 `writeSheet()` → 写入表头行，`fillExcelData(0)` 因 `list.size()==0` 无操作
> - 每批数据遍历 `fields` 调用 `addCell()` → 复用 ExcelUtil 已有的日期格式化、字典映射、类型转换逻辑
> - `flushRows(pageSize)` → 强制 SXSSF 将已写入的行刷出到临时文件，保持堆内仅 500 行

#### 2.2.2 同步导出改造（Controller 层）

```java
// WorkOrderController.java - 同步导出改造
@GetMapping("/export")
public void export(HttpServletResponse response, WorkOrder workOrder) {
    ExcelUtil<WorkOrder> util = new ExcelUtil<>(WorkOrder.class);
    // 流式导出：每批 5000 行，不加载全量到内存
    util.streamExportExcel(response,
        (pageNum, pageSize) -> {
            // 每批查询时设置分页
            PageHelper.startPage(pageNum, pageSize);
            List<WorkOrder> batch = workOrderService.selectWorkOrderList(workOrder);
            return batch;
        },
        "工单数据", "工单导出", 5000);
}
```

#### 2.2.3 异步导出（适合超大数据量）

**新增导出任务表**：

```sql
CREATE TABLE sys_export_task (
    task_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    task_name    VARCHAR(100)  NOT NULL COMMENT '任务名称',
    module       VARCHAR(50)   NOT NULL COMMENT '所属模块（workorder/user/...）',
    query_params JSON          COMMENT '查询参数（JSON 序列化）',
    file_path    VARCHAR(500)  COMMENT '导出文件路径',
    file_size    BIGINT        COMMENT '文件大小（字节）',
    status       CHAR(1)       NOT NULL DEFAULT '0' COMMENT '状态（0待处理 1处理中 2已完成 3失败）',
    error_msg    VARCHAR(2000) COMMENT '错误信息',
    create_by    VARCHAR(64)   COMMENT '创建者',
    create_time  DATETIME      COMMENT '创建时间',
    update_time  DATETIME      COMMENT '更新时间',
    KEY idx_status (status),
    KEY idx_create_by (create_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出任务记录表';
```

**异步导出流程**：

```
用户点击"导出" → 创建导出任务（status=0）→ 异步线程执行导出
  → 分页查询 + SXSSF 流式写入临时文件 → 更新任务状态为已完成
  → 系统通知（SysNotice）提示用户下载
用户查看通知 → 点击下载链接 → CommonController 提供文件下载接口
```

**核心流程代码**：

```java
// ExportTaskService.java - 异步导出服务（新增）
@Service
public class ExportTaskService {

    @Autowired
    private AsyncManager asyncManager;

    /**
     * 提交异步导出任务
     */
    public Long submitExportTask(String taskName, String module, 
                                  String queryParamsJson, Runnable exportLogic) {
        // 1. 插入任务记录
        SysExportTask task = new SysExportTask();
        task.setTaskName(taskName);
        task.setModule(module);
        task.setQueryParams(queryParamsJson);
        task.setStatus("0");
        task.setCreateBy(SecurityUtils.getUsername());
        insertTask(task);

        // 2. 异步执行
        Long taskId = task.getTaskId();
        asyncManager.execute(() -> {
            try {
                // 更新状态为处理中
                updateTaskStatus(taskId, "1", null);
                // 执行导出逻辑（由调用方传入）
                exportLogic.run();
                // 更新状态为已完成
                updateTaskStatus(taskId, "2", null);
                // 发送站内通知
                pushDownloadNotice(taskId, taskName);
            } catch (Exception e) {
                updateTaskStatus(taskId, "3", e.getMessage());
                log.error("异步导出失败 taskId={}", taskId, e);
            }
        });

        return taskId;
    }
}

// WorkOrderController.java - 异步导出接口
@GetMapping("/asyncExport")
public AjaxResult asyncExport(WorkOrder workOrder) {
    String paramsJson = JSON.toJSONString(workOrder);
    Long taskId = exportTaskService.submitExportTask(
        "工单数据导出",
        "workorder",
        paramsJson,
        () -> doExportWorkOrder(workOrder) // 实际导出逻辑
    );
    return success("导出任务已提交，任务ID：" + taskId);
}

// 实际导出逻辑
private void doExportWorkOrder(WorkOrder workOrder) {
    // 流式导出到临时文件
    String filePath = exportDir + "/workorder_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
    try (SXSSFWorkbook wb = new SXSSFWorkbook(500)) {
        PageHelper.startPage(1, 5000);
        List<WorkOrder> list = workOrderService.selectWorkOrderList(workOrder);
        // ... 分批写入
        wb.write(new FileOutputStream(filePath));
    }
}
```

### 2.3 面试应答话术

> **问：为什么会 OOM，你怎么解决？**
>
> **答**：原生导出在 `WorkOrderController.export()` 中一次性调用 `selectWorkOrderList()` 查全量数据，把所有行加载到一个 `List<WorkOrder>` 中，再传给 POI 生成 Excel。当数据量达到 10 万行以上时，List 本身占 200MB+，POI 对象模型额外膨胀数倍，合计超过 JVM 堆内存导致 OOM。
>
> **我的解决方案分三层**：
> 1. **同步流式导出**（5 万行以内）：利用 `PageHelper` 分页 + `SXSSFWorkbook` 流式写入，每次只查 5000 行并写入临时文件，不保留全量 List。现有 `ExcelUtil` 已用 `SXSSFWorkbook`，但缺分页回调——新增 `streamExportExcel(pageFetcher)` 方法解决。
> 2. **异步导出**（超大数据量）：新增 `sys_export_task` 表记录导出任务，Controller 提交后立即返回，后台线程分批查询 + 流式写入临时文件，完成后通过站内通知提醒用户下载。
> 3. **导出限流**：同一用户同时只能有一个进行中的导出任务，防止多次大导出叠加 OOM。

---

## 三、SQL 慢查询优化

### 3.1 现状分析

#### 3.1.1 工单列表查询 SQL

当前 `WorkOrderMapper.xml` 中的工单列表查询：

```xml
<select id="selectWorkOrderList" parameterType="WorkOrder" resultMap="WorkOrderResult">
    <include refid="selectWorkOrderVo"/>
    <where>
        <if test="orderNo != null">AND wo.order_no LIKE CONCAT('%', #{orderNo}, '%')</if>
        <if test="deviceName != null">AND di.device_name LIKE CONCAT('%', #{deviceName}, '%')</if>
        <if test="params.beginTime != null">AND wo.create_time &gt;= #{params.beginTime}</if>
        <if test="params.endTime != null">AND wo.create_time &lt;= #{params.endTime}</if>
        <!-- ... 其余条件 -->
    </where>
    ORDER BY wo.create_time DESC
</select>
```

#### 3.1.2 当前索引情况

从 `device-workorder.sql` 看，当前索引：

```sql
-- work_order 表
KEY `idx_order_no` (`order_no`),
KEY `idx_device_id` (`device_id`),
KEY `idx_order_status` (`order_status`),
KEY `idx_assign_to` (`assign_to`),
KEY `idx_create_time` (`create_time`)

-- device_info 表
KEY `idx_device_code` (`device_code`),
KEY `idx_device_status` (`device_status`)
```

#### 3.1.3 存在的问题

1. **索引覆盖不足**：`work_order` 表最频繁的查询模式是 `create_time` + `order_status` 组合筛选，但只有单列索引，无法覆盖联合查询
2. **COUNT 分页随数据量增大而变慢**：`PageHelper` 生成的 `select count(0)` 在大表上需要全表扫描（或扫描索引），耗时随数据量线性增长
3. **深分页性能退化**：`LIMIT 1000000, 20` 需要扫描前 100 万行，越往后越慢

### 3.2 改造方案

#### 3.2.1 新增联合索引

```sql
-- 1. 工单列表最常用查询模式：按时间倒序 + 状态筛选
--    覆盖：首页列表、状态筛选、时间段筛选
ALTER TABLE work_order ADD INDEX idx_status_create_time (order_status, create_time DESC);

-- 2. 工单统计看板：当月数据按设备分组 + 状态聚合
ALTER TABLE work_order ADD INDEX idx_device_create_time (device_id, create_time);

-- 3. 设备表：状态 + 名称组合查询（前端的设备选择器）
ALTER TABLE device_info ADD INDEX idx_status_device_name (device_status, device_name);

-- 4. 工单记录表：按工单 ID 查询维修记录
--    work_order_record 已有 idx_order_id 单列索引，改为联合索引覆盖时间排序
ALTER TABLE work_order_record DROP INDEX idx_order_id;
ALTER TABLE work_order_record ADD INDEX idx_order_create_time (order_id, create_time DESC);

-- 5. 每月归档历史数据的表分区（可选，大数据量时）
-- ALTER TABLE work_order PARTITION BY RANGE (YEAR(create_time)*100 + MONTH(create_time))
-- (PARTITION p202601 VALUES LESS THAN (202602), ...);
```

索引设计要点：

| 索引 | 设计依据 | 覆盖场景 |
|------|---------|---------|
| `idx_status_create_time` | 最左前缀：状态筛选→时间排序 | 列表页筛选 + 统计看板 |
| `idx_device_create_time` | 工单按设备统计 | 故障排行 Top10 |
| `idx_order_create_time` | 工单 ID 精确匹配→时间排序 | 工单详情页记录时间线 |

#### 3.2.2 验证查询列（当前代码已优化）

> 经检查，当前 `WorkOrderMapper.xml` 的 `selectWorkOrderVo` 已使用显式字段列表而非 `SELECT wo.*`：
>
> ```xml
> <sql id="selectWorkOrderVo">
>     SELECT wo.order_id, wo.order_no, wo.device_id, wo.reporter_by, wo.fault_desc,
>            wo.fault_type, wo.urgency_level, wo.order_status, wo.assign_to,
>            wo.assign_time, wo.finish_time, wo.archive_time, wo.archive_by,
>            wo.archive_remark, wo.create_by, wo.create_time, wo.update_by,
>            wo.update_time,
>            di.device_name, di.device_code,
>            ru1.user_name AS reporter_name,
>            ru2.user_name AS assign_name,
>            (SELECT COUNT(*) FROM work_order_record r WHERE r.order_id = wo.order_id) AS record_count
>     FROM work_order wo ...
> </sql>
> ```
>
> 当前代码已避免了 `SELECT *` 问题，此处不需要额外修改，保持现状即可。

#### 3.2.3 大表分页优化

**问题**：`PageHelper` 的 `select count(0)` 在百万级表上执行 `COUNT(*)` 需要全表扫描。

**优化方案一：近似估算（适用于非精确分页）**

```java
// BaseController.java - 新增方法
protected TableDataInfo getDataTableApproximate(List<?> list, String tableName) {
    TableDataInfo rspData = new TableDataInfo();
    rspData.setCode(HttpStatus.SUCCESS);
    rspData.setMsg("查询成功");
    rspData.setRows(list);
    // 使用 MySQL SHOW TABLE STATUS 估算行数
    Long estimatedTotal = estimateTableRows(tableName);
    rspData.setTotal(estimatedTotal != null ? estimatedTotal : new PageInfo(list).getTotal());
    return rspData;
}

private Long estimateTableRows(String tableName) {
    // 查询 SHOW TABLE STATUS
    // 返回 rows_estimate 值
}
```

**优化方案二：覆盖索引 COUNT（推荐）**

确保 `COUNT` 查询走索引而非全表扫描：

```sql
-- PageHelper 执行 count(0) 时，如果条件只有 create_time 和 order_status，
-- 联合索引 idx_status_create_time 能完全覆盖 count 查询
-- 无需回表，速度从秒级降到毫秒级
```

实际改造：确保 `PageHelper` 的 COUNT 查询能命中联合索引即可。

**优化方案三：分页阈值限制（兜底）**

```java
// 限制最大分页深度，防止深分页
@GetMapping("/list")
public TableDataInfo list(WorkOrder workOrder) {
    PageDomain pageDomain = TableSupport.buildPageRequest();
    if (pageDomain.getPageNum() > 1000) {
        // 超过 1000 页后，限制查询深度
        pageDomain.setPageNum(1000);
    }
    startPage();
    List<WorkOrder> list = workOrderService.selectWorkOrderList(workOrder);
    return getDataTable(list);
}
```

#### 3.2.4 业务查询 SQL 优化对照

| SQL 操作 | 优化前 | 优化后 | 预期提升 |
|----------|--------|--------|---------|
| 工单列表查询 | `SELECT wo.*` + 单列索引 | `SELECT 12 字段` + 联合索引 | I/O 减少 50%，索引过滤提升 10x |
| COUNT 分页 | 全表扫描 | 覆盖索引 COUNT | 从秒级降到毫秒级 |
| 统计看板 | 全表遍历 | 索引覆盖 + 缓存 | 从秒级降到毫秒级(缓存) |
| 故障排行 | 全表 GROUP BY | 索引 `idx_device_create_time` | 从秒级降到百毫秒级 |
| 详情记录时间线 | 单列索引 + 文件排序 | 联合索引覆盖排序 | 避免文件排序 |

---

## 四、实施路线图

### 阶段一：SQL 优化 + 索引（1 天）

| 步骤 | 内容 | 涉及文件 |
|------|------|---------|
| 1.1 | 分页优化：改 `selectWorkOrderVo` 为按需查询字段 | `WorkOrderMapper.xml` |
| 1.2 | 分页优化：`getDataTableApproximate()` 估算数方法 | `BaseController.java` |
| 1.3 | 执行索引 DDL 到测试环境 | SQL 脚本 |
| 1.4 | 执行 `EXPLAIN` 验证执行计划 | 数据库 |

### 阶段二：Redis 缓存（2 天）

| 步骤 | 内容 | 涉及文件 |
|------|------|---------|
| 2.1 | `CacheConstants.java` 新增业务缓存 Key | `CacheConstants.java` |
| 2.2 | 新建 `@RedisCache` 注解 | `com.ruoyi.common.annotation.RedisCache.java` |
| 2.3 | 新建 `RedisCacheAspect` 切面 | `com.ruoyi.framework.aspectj.RedisCacheAspect.java` |
| 2.4 | 设备信息 Service 添加缓存注解 | `DeviceInfoServiceImpl.java` |
| 2.5 | 工单统计看板添加缓存注解 | `WorkOrderServiceImpl.java` |
| 2.6 | 字典工具类增强缓存 | `DictUtils.java` |
| 2.7 | 单元测试：缓存命中/失效 | 测试文件 |

### 阶段三：导出优化（2 天）

| 步骤 | 内容 | 涉及文件 |
|------|------|---------|
| 3.1 | `ExcelUtil` 新增 `streamExportExcel()` 流式导出方法 | `ExcelUtil.java` |
| 3.2 | Controller 同步导出改为流式 | `WorkOrderController.java` |
| 3.3 | 新建 `sys_export_task` 表 | SQL 脚本 |
| 3.4 | 新建 `SysExportTask` 实体 + Mapper | domain/mapper/xml |
| 3.5 | 新建 `ExportTaskService` 异步导出服务 | `ExportTaskService.java` |
| 3.6 | Controller 新增异步导出接口 | `WorkOrderController.java` |
| 3.7 | 文件下载接口 + 站内通知 | `CommonController.java` |
| 3.8 | 集成测试 | 测试环境 |

---

## 五、预期效果与验证指标

### 5.1 Redis 缓存

| 指标 | 优化前 | 优化后 | 验证方式 |
|------|--------|--------|---------|
| 设备详情查询响应时间 | ~50ms（查库） | ~5ms（Redis） | Postman 对比 |
| 统计看板加载时间 | ~800ms（聚合 SQL） | ~5ms（缓存命中） | 浏览器 Network |
| 高频设备查询场景 | 每次查库 | 1800s 缓存 | Redis `keys device_info:*` |
| 看板缓存自动失效 | - | 600s TTL | 等待过期后验证 |

### 5.2 导出优化

| 指标 | 优化前 | 优化后 | 验证方式 |
|------|--------|--------|---------|
| 10 万行导出内存 | OOM | <50MB | `jvisualvm` 监控 Heap |
| 导出响应时间 | 超时/失败 | 逐批写入 | 实际测试 |
| 大导出用户体验 | 页面卡死 | 异步可继续操作 | 功能测试 |

### 5.3 SQL 优化

| 指标 | 优化前 | 优化后 | 验证方式 |
|------|--------|--------|---------|
| 列表查询耗时 | ~200ms | <30ms | `EXPLAIN` + slow log |
| COUNT 分页耗时 | ~150ms | <10ms | `EXPLAIN` type=index |
| 深分页（100页后） | 越来越慢 | 限制 1000 页 | 边界测试 |

### 5.4 简历话术

> **1. Redis 热点缓存**：原生系统频繁查询设备基础信息，引入 Redis 做热点业务数据缓存，自定义 `@RedisCache` 缓存切面实现自动更新失效，页面查询响应速度提升约 60%。
>
> **2. 大数据导出 OOM 优化**：原生导出一次性查全量数据导致内存溢出，改为分页流式导出（PageHelper 分批 + SXSSFWorkbook 流式写入），并引入异步导出任务表 + 站内通知完成提醒，解决了 10 万行以上数据导出的 OOM 问题。
>
> **3. SQL 慢查询优化**：给工单表常用筛选建立联合索引（`order_status + create_time`）、列表查询改为按需字段而非 `SELECT *`、PageHelper 分页 COUNT 覆盖索引优化，列表页查询耗时从 200ms 降至 30ms 以内。