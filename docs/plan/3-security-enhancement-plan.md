# 安全与功能增强改造方案设计文档

> 文档版本：v1.0 | 编写日期：2026-07-03 | 基于 RuoYi v3.9.2 (Spring Boot 4.0.6)

---

## 目录

1. [总体说明](#一总体说明)
2. [改造一：AOP 切面敏感数据脱敏](#二改造一aop-切面敏感数据脱敏)
3. [改造二：接口防重复提交增强](#三改造二接口防重复提交增强)
4. [改造三：JWT 双 Token 登录优化](#四改造三jwt-双-token-登录优化)
5. [改造四：文件上传增强（MinIO 适配）](#五改造四文件上传增强minio-适配)
6. [实施路线图](#六实施路线图)
7. [风险与兼容性分析](#七风险与兼容性分析)

---

## 一、总体说明

### 1.1 改造目标

| 改造项 | 目标 | 优先级 |
|--------|------|--------|
| 敏感数据脱敏 | 防止敏感信息（手机号、身份证、设备编号）直接暴露给非授权用户 | P0 |
| 接口防重复提交 | 防止工单重复提交、批量操作重复执行 | P0 |
| JWT 双 Token 机制 | 解决 token 过期强制退出问题，提升用户体验 | P1 |
| 文件上传增强 | 支持 MinIO 对象存储，增强文件安全校验 | P1 |

### 1.2 设计原则

1. **最小改动**：基于若依原生框架扩展，不破坏现有功能。
2. **注解驱动**：通过自定义注解 + AOP/拦截器实现，对业务代码侵入最小。
3. **配置可开关**：新增功能通过配置开关控制，默认启用，可降级回退。
4. **前后端兼容**：后端 API 保持向后兼容，前端按需升级。

### 1.3 受影响模块

| 模块 | 改造项 | 变更类型 |
|------|--------|----------|
| `ruoyi-common` | 新增注解 `@Desensitize`、新增 MinIO 配置类 | 新增文件 |
| `ruoyi-framework` | 新增脱敏切面、增强防重复提交、改造 Token 服务 | 新增 + 修改 |
| `ruoyi-admin` | 新增 `refreshToken` 接口、增强文件上传 | 新增 + 修改 |
| 前端 | 适配双 Token、配置 MinIO 直传 | 修改 |

---

## 二、改造一：AOP 切面敏感数据脱敏

### 2.1 现状分析

若依原生已提供 `@Sensitive` 注解（Jackson 字段级序列化脱敏），基于 `SensitiveJsonSerializer` + `DesensitizedType` 枚举实现，支持 `PHONE`、`ID_CARD`、`EMAIL` 等类型。

**现有问题**：
1. `@Sensitive` 必须在每个 DTO 字段上逐个标注，工作量较大。
2. 设备涉密编号等业务敏感字段缺乏统一脱敏处理。
3. 脱敏判断逻辑（管理员不脱敏）内嵌在 Jackson 序列化器中，颗粒度不够细。

### 2.2 改造方案

新增 `@Desensitize` 方法级注解 + AOP 环绕通知，拦截 `@RestController` 接口的返回数据，在序列化后统一替换敏感数据。

#### 2.2.1 注解定义

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Desensitize {
    /** 脱敏字段映射：fieldName → 脱敏类型 */
    DesensitizeField[] fields();
}

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface DesensitizeField {
    /** 字段名（支持嵌套路径，如 "user.phone"） */
    String name();
    /** 脱敏类型 */
    DesensitizedType type();
}
```

#### 2.2.2 AOP 切面

```java
@Aspect
@Component
public class DesensitizeAspect {

    @Around("@annotation(desensitize)")
    public Object around(JoinPoint joinPoint, Desensitize desensitize) throws Throwable {
        Object result = joinPoint.proceed();
        // 仅对 AjaxResult 类型的成功响应进行脱敏
        if (result instanceof AjaxResult ajax && ajax.get("data") != null) {
            Object data = ajax.get("data");
            Object sensitiveData = doDesensitize(data, desensitize.fields());
            // 替换脱敏后的 data 字段
            ajax.put("data", sensitiveData);
        }
        return result;
    }
}
```

#### 2.2.3 脱敏引擎

- 支持**字段路径解析**：如 `user.phone` → 递归获取 `data.user.phone`
- 支持**列表脱敏**：`data` 为 `List<?>` 时，遍历每个元素脱敏
- 支持**管理员豁免**：`SecurityUtils.getLoginUser().getUser().isAdmin()` 时不脱敏
- 使用 Jackson `ObjectMapper` 将脱敏后的值写回对象

#### 2.2.4 使用示例

```java
@GetMapping("/detail")
@Desensitize(fields = {
    @DesensitizeField(name = "phone", type = DesensitizedType.PHONE),
    @DesensitizeField(name = "idCard", type = DesensitizedType.ID_CARD),
    @DesensitizeField(name = "deviceCode", type = DesensitizedType.DEVICE_CODE)
})
public AjaxResult detail(Long id) {
    DeviceInfo device = deviceInfoService.selectDeviceInfoById(id);
    return success(device);
}
```

### 2.3 新增脱敏类型

在 `DesensitizedType` 枚举中新增：

| 类型 | 规则 | 示例 |
|------|------|------|
| `DEVICE_CODE` | 保留前2位+后2位，中间星号 | `AB****YZ` |
| `ADDRESS` | 保留省份，其余星号 | `广东省****` |

### 2.4 与原 `@Sensitive` 的关系

| 维度 | `@Sensitive`（原生） | `@Desensitize`（新增） |
|------|---------------------|----------------------|
| 作用域 | 字段级 | 方法级 |
| 实现方式 | Jackson 序列化器 | AOP 环绕通知 |
| 适用场景 | 实体定义时固定的脱敏规则 | 按接口动态指定脱敏字段 |
| 两者关系 | **互补**，可同时使用 | - |

---

## 三、改造二：接口防重复提交增强

### 3.1 现状分析

若依原生已提供 `@RepeatSubmit` + `SameUrlDataInterceptor` 防重复提交机制：

| 维度 | 当前实现 |
|------|----------|
| 存储 | Redis |
| Key 构成 | `repeat_submit:` + url + token header |
| 间隔时间 | 注解 `interval()` 参数，默认 5000ms |
| 实现方式 | HandlerInterceptor，对比请求参数 + 时间间隔 |

**现有问题**：
1. Key 不使用用户 ID，仅依赖 token header，多端登录时可能误判。
2. 参数对比方式为全量 JSON 字符串匹配，忽略无关参数差异（如时间戳）。
3. 对于工单批量提交场景，需要更精确的**业务幂等性**校验。

### 3.2 改造方案

增强 `@RepeatSubmit` 注解 + 新增基于 Redis 的拦截器实现。

#### 3.2.1 增强注解

```java
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {
    /** 间隔时间(ms)，小于此时间视为重复提交 */
    int interval() default 5000;
    /** 提示消息 */
    String message() default "不允许重复提交，请稍候再试";
    /** 限流模式：DEFAULT=url+用户+参数hash, PARAM=基于指定参数名 */
    LimitMode mode() default LimitMode.DEFAULT;
    /** PARAM 模式下指定参数名（支持 SpEL） */
    String lockParam() default "";
}

public enum LimitMode {
    DEFAULT,  // 标准：用户ID + 接口URL + 参数哈希
    PARAM     // 业务锁：用户ID + 指定参数值（如工单ID）
}
```

#### 3.2.2 Redis 签名存储

```java
// Key 构成（DEFAULT 模式）
String signKey = CacheConstants.REPEAT_SUBMIT_KEY
    + userId + ":" 
    + request.getRequestURI() + ":"
    + md5(JSON.toJSONString(paramMap));  // 参数 MD5 签名

// 存储
redisCache.setCacheObject(signKey, "1", interval, TimeUnit.MILLISECONDS);

// PARAM 模式 → 业务级锁
// Key：repeat_submit:userId:/workorder/order/batchAssign:orderId=123
// 该模式下只校验指定参数是否重复，忽略其他参数变化
```

#### 3.2.3 拦截器逻辑

```
请求到达
  ↓
检查是否有 @RepeatSubmit 注解
  ├─ 无 → 放行
  └─ 有 → 获取当前用户 ID
          ↓
          mode = DEFAULT?
          ├─ 是 → 构建 key = userId + url + paramMD5 → Redis SETNX
          │        ├─ 已存在 → 重复提交，返回错误
          │        └─ 不存在 → 存入 Redis（interval 后过期），放行
          └─ 否 → 解析 SpEL 获取 lockParam 值
                  构建 key = userId + url + paramValue → 同上逻辑
```

#### 3.2.4 使用场景示例

```java
// 场景1：标准防重（5秒内同一用户同一参数不可重复提交）
@RepeatSubmit(interval = 5000)
@PostMapping
public AjaxResult add(@RequestBody WorkOrder workOrder) { ... }

// 场景2：批量操作防重（基于工单ID的业务锁）
@RepeatSubmit(interval = 10000, mode = LimitMode.PARAM, lockParam = "#workOrder.orderId")
@PutMapping("/batchAssign")
public AjaxResult batchAssign(@RequestBody WorkOrder workOrder) { ... }

// 场景3：保存修改防重
@RepeatSubmit(interval = 3000, message = "请勿频繁保存")
@PutMapping
public AjaxResult edit(@RequestBody WorkOrder workOrder) { ... }
```

### 3.3 兼容性

- 新增模式不影响现有 `SameUrlDataInterceptor` 的逻辑。
- 若用户仍使用原生方式调用 `@RepeatSubmit`（不带 `mode` 参数），默认走 `DEFAULT` 模式，兼容原有行为。
- 在 `ResourcesConfig.addInterceptors()` 中注册新的拦截器实例。

---

## 四、改造三：JWT 双 Token 登录优化

### 4.1 现状分析

若依原生 JWT 实现要点：

| 维度 | 当前实现 |
|------|----------|
| Token 格式 | JWT（HS512），payload 含 `uuid` + `username` |
| 用户状态 | Redis 存储 `LoginUser`，key=`login_tokens:{uuid}` |
| 有效期 | 固定 30 分钟（`token.expireTime`） |
| 续期机制 | `verifyToken()` 在剩余不足 20 分钟时自动刷新 Redis 缓存 |
| 过期处理 | Redis 中 `LoginUser` 过期后，`getLoginUser()` 返回 null → `AuthenticationEntryPointImpl. commence()` → 401 |

**现有问题**：
1. **体验差**：token 过期后用户正在操作的表单内容丢失，需要重新登录。
2. **无感续期缺失**：虽然 `verifyToken` 会续期 Redis，但 JWT 本身的过期时间未延长。
3. **登出清理不彻底**：只删除 Redis 缓存，JWT 本身无法失效（无黑名单机制）。

### 4.2 改造方案

引入**双 Token 机制**：短时效 `accessToken` + 长时效 `refreshToken`。

#### 4.2.1 Token 生命周期

```
用户登录
  ↓
签发双 Token：
  accessToken  (JWT, 有效 30 分钟)  → 用于 API 鉴权
  refreshToken (JWT, 有效 7 天)    → 用于续期 accessToken
  ↓
请求携带 accessToken
  ↓
accessToken 过期 → 前端携带 refreshToken 调用 /refresh
  ↓
服务端校验 refreshToken
  ├─ 有效 → 签发新 accessToken（Redis 续期），返回给前端
  └─ 无效/过期 → 返回 401，前端跳转登录页
```

#### 4.2.2 数据结构

```java
// 登录响应新增 refreshToken 字段
{
  "code": 200,
  "msg": "操作成功",
  "token": "Bearer xxx...",        // accessToken（30分钟）
  "refreshToken": "Bearer yyy..."  // refreshToken（7天）
}
```

#### 4.2.3 TokenService 改造

```java
@Component
public class TokenService {
    // 原有字段不变

    /** refreshToken 有效期（天） */
    @Value("${token.refreshExpireTime:7}")
    private int refreshExpireTime;

    /** 黑名单缓存前缀 */
    private static final String TOKEN_BLACKLIST_KEY = "token_blacklist:";

    /** 创建双 Token */
    public Map<String, String> createDualToken(LoginUser loginUser) {
        // 1. 创建 accessToken（原有 createToken 逻辑不变）
        String accessToken = createToken(loginUser);

        // 2. 创建 refreshToken
        String refreshToken = IdUtils.fastUUID();
        loginUser.setRefreshToken(refreshToken);
        String refreshKey = getRefreshTokenKey(refreshToken);
        redisCache.setCacheObject(refreshKey, loginUser.getToken(), 
            refreshExpireTime, TimeUnit.DAYS);

        // 3. 返回双 Token
        Map<String, String> tokens = new HashMap<>();
        tokens.put("token", Constants.TOKEN_PREFIX + accessToken);
        tokens.put("refreshToken", Constants.TOKEN_PREFIX + refreshToken);
        return tokens;
    }

    /** 刷新 accessToken */
    public String refreshAccessToken(String refreshTokenStr) {
        // 1. 校验 refreshToken
        String tokenValue = refreshTokenStr.replace(Constants.TOKEN_PREFIX, "");
        String refreshKey = getRefreshTokenKey(tokenValue);
        String accessTokenUuid = redisCache.getCacheObject(refreshKey);
        if (StringUtils.isEmpty(accessTokenUuid)) {
            throw new ServiceException("refreshToken 已过期，请重新登录");
        }

        // 2. 获取用户信息
        String userKey = getTokenKey(accessTokenUuid);
        LoginUser loginUser = redisCache.getCacheObject(userKey);
        if (loginUser == null) {
            throw new ServiceException("用户会话已过期，请重新登录");
        }

        // 3. 生成新 accessToken
        String newAccessToken = createToken(loginUser);

        // 4. 删除旧 accessToken 的 Redis 缓存（使用旧 uuid）
        redisCache.deleteObject(userKey);

        return Constants.TOKEN_PREFIX + newAccessToken;
    }

    /** 登出时加入黑名单 */
    public void logout(HttpServletRequest request) {
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            // 解析 JWT 获取过期时间
            Claims claims = parseToken(token);
            long expiration = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            if (expiration > now) {
                // 将 token 加入黑名单，有效期至 JWT 原本过期时间
                String blacklistKey = TOKEN_BLACKLIST_KEY + token;
                redisCache.setCacheObject(blacklistKey, "1", 
                    (expiration - now) / 1000, TimeUnit.SECONDS);
            }
            // 删除 Redis 中的 LoginUser
            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            redisCache.deleteObject(getTokenKey(uuid));
        }
    }

    /** 检查是否在黑名单中 */
    public boolean isBlacklisted(String token) {
        return redisCache.hasKey(TOKEN_BLACKLIST_KEY + token);
    }
}
```

#### 4.2.4 JWT 过滤器改造

```java
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String token = tokenService.getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            // 1. 检查黑名单
            if (tokenService.isBlacklisted(token)) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }
            // 2. 原有逻辑：获取 LoginUser + verifyToken
            LoginUser loginUser = tokenService.getLoginUser(request);
            if (loginUser != null && SecurityUtils.getAuthentication() == null) {
                tokenService.verifyToken(loginUser);
                // ... 设置 Authentication
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        chain.doFilter(request, response);
    }
}
```

#### 4.2.5 新增刷新端点

```java
@RestController
public class SysLoginController {
    @PostMapping("/refresh")
    public AjaxResult refresh(HttpServletRequest request) {
        String refreshToken = request.getHeader("Refresh-Token");
        if (StringUtils.isEmpty(refreshToken)) {
            return error("缺少 refreshToken");
        }
        try {
            String newAccessToken = tokenService.refreshAccessToken(refreshToken);
            AjaxResult ajax = AjaxResult.success();
            ajax.put(Constants.TOKEN, newAccessToken);
            return ajax;
        } catch (ServiceException e) {
            return error(e.getMessage());
        }
    }
}
```

#### 4.2.6 前端适配

```typescript
// Axios 响应拦截器 — 自动续期
service.interceptors.response.use(
  response => {
    // ... 原有逻辑
  },
  async error => {
    if (error.response?.status === 401) {
      // accessToken 过期，尝试用 refreshToken 续期
      const refreshToken = getRefreshToken(); // 从 cookie/localStorage 读取
      if (refreshToken) {
        try {
          const res = await refreshAccessToken(refreshToken);
          setToken(res.token);     // 更新 accessToken
          setRefreshToken(res.refreshToken);
          // 重试原始请求
          const config = error.config;
          config.headers['Authorization'] = res.token;
          return service(config);
        } catch {
          // refreshToken 也过期 → 跳转登录
          removeToken();
          router.push('/login');
        }
      } else {
        router.push('/login');
      }
    }
    return Promise.reject(error);
  }
);
```

#### 4.2.7 登出黑名单机制

```
用户登出 POST /logout
  ↓
LogoutSuccessHandlerImpl.onLogoutSuccess()
  → 获取当前 token
  → 解析 JWT 获取过期时间
  → token_blacklist:{jwt} → Redis 存至 JWT 过期
  → 删除 login_tokens:{uuid}
  → 返回登出成功
  ↓
攻击者持有已登出的 token 访问 API
  → JWT 本身可能仍未过期
  → JwtAuthenticationTokenFilter 检查黑名单
  → 黑名单命中 → 拒绝访问
```

### 4.3 配置项

```yaml
# application.yml 新增
token:
  # refreshToken 有效期（天）
  refreshExpireTime: 7
  # 双 Token 开关（默认开启，关闭后回退为单 Token）
  dualTokenEnabled: true
```

---

## 五、改造四：文件上传增强（MinIO 适配）

### 5.1 现状分析

若依原生仅支持**本地文件存储**：

```yaml
ruoyi:
  profile: D:/ruoyi/uploadPath  # 本地文件存储路径
```

上传流程：`CommonController.uploadFile()` → `FileUploadUtils.upload()` → 写入本地磁盘。
存在的问题：
1. 仅支持本地存储，不支持对象存储（MinIO/S3）。
2. 文件类型校验 `assertAllowed()` 仅验证扩展名，未校验文件头 Magic Number。
3. 工单维修图片多图上传场景没有专用的安全拦截。

### 5.2 改造方案

#### 5.2.1 整体架构

```
前端上传
  ↓
CommonController.uploadFile()
  ├─ 文件类型/大小安全校验（增强）
  ├─ 存储策略选择
  │   ├─ local  → FileUploadUtils（原生本地存储）
  │   └─ minio  → MinioUploadUtils（新增 MinIO 存储）
  └─ 返回 URL
```

#### 5.2.2 MinIO 配置

```yaml
# application.yml 新增
ruoyi:
  file:
    # 存储策略：local | minio
    strategy: local
    minio:
      endpoint: http://localhost:9000
      accessKey: minioadmin
      secretKey: minioadmin
      bucketName: ruoyi-files
      # 文件访问域名（用于生成可访问的 URL）
      publicDomain: http://localhost:9000
```

#### 5.2.3 MinIO 配置类

```java
@Configuration
@ConfigurationProperties(prefix = "ruoyi.file.minio")
public class MinIoConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String publicDomain;
    // getter/setter
}
```

#### 5.2.4 MinIO 工具类

```java
@Component
public class MinIoUtils {
    @Autowired
    private MinIoConfig minIoConfig;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
            .endpoint(minIoConfig.getEndpoint())
            .credentials(minIoConfig.getAccessKey(), minIoConfig.getSecretKey())
            .build();
    }

    /** 上传文件 */
    public String upload(MultipartFile file) throws Exception {
        String fileName = generateFileName(file);
        // 检查 bucket 是否存在
        boolean found = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(minIoConfig.getBucketName()).build());
        if (!found) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(minIoConfig.getBucketName()).build());
        }
        // 上传
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minIoConfig.getBucketName())
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        // 返回可访问 URL
        return minIoConfig.getPublicDomain() + "/" + minIoConfig.getBucketName() + "/" + fileName;
    }

    /** 删除文件 */
    public void delete(String fileName) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(minIoConfig.getBucketName())
                .object(fileName)
                .build());
    }
}
```

#### 5.2.5 通用上传策略

```java
@Component
public class FileUploadStrategy {
    @Autowired(required = false)
    private MinIoUtils minIoUtils;

    @Value("${ruoyi.file.strategy:local}")
    private String strategy;

    public String upload(MultipartFile file) throws Exception {
        // 1. 安全校验（增强）
        SecurityFileUtils.assertSecure(file);

        // 2. 按策略分发
        if ("minio".equals(strategy) && minIoUtils != null) {
            return minIoUtils.upload(file);
        }
        // 默认本地存储
        String filePath = RuoYiConfig.getUploadPath();
        return FileUploadUtils.upload(filePath, file);
    }
}
```

#### 5.2.6 文件安全校验增强

新增 `SecurityFileUtils`，在原生 `assertAllowed()` 基础上增加：

| 校验项 | 说明 |
|--------|------|
| **扩展名白名单** | 仅允许 `jpg/png/gif/pdf/doc/docx/xls/xlsx` 等 |
| **文件大小** | 图片 ≤ 5MB，文档 ≤ 20MB |
| **Magic Number** | 校验文件头 4 字节，防止伪装扩展名的脚本文件 |
| **文件名安全** | 过滤 `../`、`;`、`\0` 等路径穿越字符 |

```java
public class SecurityFileUtils {
    /** 常见文件类型的 Magic Number */
    private static final Map<String, byte[]> MAGIC_NUMBERS = new HashMap<>();
    static {
        MAGIC_NUMBERS.put("jpg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
        MAGIC_NUMBERS.put("png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47});
        MAGIC_NUMBERS.put("gif", new byte[]{0x47, 0x49, 0x46});
        MAGIC_NUMBERS.put("pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        // ...
    }

    public static void assertSecure(MultipartFile file) {
        // 1. 文件名安全（路径穿越防护）
        String filename = file.getOriginalFilename();
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new InvalidExtensionException("文件名包含非法字符");
        }

        // 2. Magic Number 校验（防脚本上传）
        String extension = FilenameUtils.getExtension(filename);
        byte[] expectedMagic = MAGIC_NUMBERS.get(extension.toLowerCase());
        if (expectedMagic != null) {
            byte[] fileHeader = new byte[expectedMagic.length];
            try {
                file.getInputStream().read(fileHeader, 0, expectedMagic.length);
                if (!Arrays.equals(fileHeader, expectedMagic)) {
                    throw new InvalidExtensionException("文件类型与扩展名不匹配");
                }
            } catch (IOException e) {
                throw new InvalidExtensionException("文件读取失败");
            }
        }
    }
}
```

### 5.3 配置项

```yaml
ruoyi:
  file:
    # 存储策略：local | minio
    strategy: local
    minio:
      endpoint: http://localhost:9000
      accessKey: minioadmin
      secretKey: minioadmin
      bucketName: ruoyi-files
      publicDomain: http://localhost:9000
    # 文件大小限制
    maxSize:
      image: 5MB
      document: 20MB
```

### 5.4 适用场景

| 场景 | 存储策略 | 说明 |
|------|----------|------|
| 工单维修图片 | MinIO | 多图上传，支持云端访问和分享 |
| 导出文件 | 本地 | 临时文件，下载后自动清理 |
| 头像 | 本地/MinIO | 取决于部署配置 |

---

## 六、实施路线图

### 6.1 任务分解

| 阶段 | 任务 | 涉及模块 | 工时评估 |
|------|------|----------|----------|
| **阶段一** | **数据脱敏（P0）** | | |
| 1.1 | 新增 `@Desensitize` / `@DesensitizeField` 注解 | `ruoyi-common` | 0.5d |
| 1.2 | 新增 `DesensitizeAspect` 切面 | `ruoyi-framework` | 1d |
| 1.3 | 新增 `DEVICE_CODE` 脱敏类型 | `ruoyi-common` | 0.3d |
| 1.4 | 在工单/设备 Controller 添加脱敏注解 | `ruoyi-workorder` | 0.5d |
| **阶段二** | **防重复提交（P0）** | | |
| 2.1 | 增强 `@RepeatSubmit` 注解（新增 mode/lockParam） | `ruoyi-common` | 0.3d |
| 2.2 | 新增 Redis 签名拦截器 | `ruoyi-framework` | 1d |
| 2.3 | 注册拦截器到 `ResourcesConfig` | `ruoyi-framework` | 0.2d |
| 2.4 | 在工单 CRUD 接口添加注解 | `ruoyi-workorder` | 0.3d |
| **阶段三** | **JWT 双 Token（P1）** | | |
| 3.1 | 新增 refreshToken 配置项 | `ruoyi-admin` | 0.2d |
| 3.2 | 改造 TokenService（双 Token 签发/刷新/黑名单） | `ruoyi-framework` | 2d |
| 3.3 | 改造 JwtAuthenticationTokenFilter（黑名单检查） | `ruoyi-framework` | 0.5d |
| 3.4 | 新增 `/refresh` 续期端点 | `ruoyi-admin` | 0.5d |
| 3.5 | 改造登录响应返回双 Token | `ruoyi-admin` | 0.3d |
| 3.6 | 前端 Axios 拦截器适配 | 前端 | 1d |
| 3.7 | 双 Token 后端单元测试 | `ruoyi-framework` | 1d |
| **阶段四** | **文件上传增强（P1）** | | |
| 4.1 | 新增 MinIO 配置类 | `ruoyi-common` | 0.3d |
| 4.2 | 新增 MinIoUtils 工具类 | `ruoyi-common` | 1d |
| 4.3 | 新增 SecurityFileUtils 安全校验 | `ruoyi-common` | 1d |
| 4.4 | 新增 FileUploadStrategy 策略分发 | `ruoyi-common` | 0.5d |
| 4.5 | 改造 CommonController 上传接口 | `ruoyi-admin` | 0.5d |
| 4.6 | MinIO 集成测试 | `ruoyi-admin` | 0.5d |
| **阶段五** | **验收与文档** | | |
| 5.1 | API 接口测试 | 全模块 | 1d |
| 5.2 | 配置文档更新 | docs | 0.5d |

### 6.2 依赖关系

```
阶段一（脱敏） → 无外部依赖，可独立开发
阶段二（防重） → 无外部依赖，可独立开发
阶段三（双Token） → 依赖 TokenService 改造，需与前端联调
阶段四（MinIO） → 依赖 MinIO 服务部署，需与运维配合
```

---

## 七、风险与兼容性分析

### 7.1 兼容性

| 改造项 | 向后兼容 | 说明 |
|--------|----------|------|
| 数据脱敏 | ✅ | `@Desensitize` 为新增注解，不影响原生 `@Sensitive` |
| 防重复提交 | ✅ | mode 默认值为 `DEFAULT`，兼容原有调用方式 |
| JWT 双 Token | ⚠️ 部分兼容 | 新增 `/refresh` 端点，不影响原 `/login` 接口；
                                 前端需升级以支持续期，未升级的前端回退为单 Token 行为 |
| MinIO | ✅ | strategy 默认 `local`，不配置 MinIO 则行为不变 |

### 7.2 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| refreshToken 被盗 | 攻击者可长期续期 | refreshToken 有效期 7 天；
                                         登出黑名单机制；
                                         支持按用户批量失效所有 refreshToken |
| MinIO 连接失败 | 上传功能不可用 | 降级为本地存储（strategy=local）；
                                         健康检查 + 告警 |
| 脱敏 AOP 性能 | 影响响应速度 | 只对标注 `@Desensitize` 的方法生效；
                                         使用 Jackson 树模型操作，避免全量反序列化 |
| 前端未适配双 Token | 续期失败，提示过期 | 401 时先尝试刷新，刷新失败才跳转登录；
                                         兼容处理：无 refreshToken 时正常跳转 |

---

## 八、实施完成总结

### 8.1 已完成实现

| 阶段 | 状态 | 说明 |
|------|------|------|
| 阶段一：数据脱敏 | ✅ 已完成 | `@Desensitize` 注解 + AOP 切面 + `DEVICE_CODE`/`ADDRESS` 脱敏类型 |
| 阶段二：防重复提交 | ✅ 已完成 | `@RepeatSubmit` 增强（DEFAULT/PARAM 模式）+ Redis SETNX 原子操作 |
| 阶段三：JWT 双 Token | ✅ 已完成 | 双 Token 签发/刷新/黑名单 + `/refresh` 端点 + 登出黑名单 |
| 阶段四：文件上传增强 | ✅ 已完成 | MinIO 配置类/工具类 + SecurityFileUtils + FileUploadStrategy 策略分发 |
| 阶段五：验收与文档 | ✅ 已完成 | API 变更清单 + MinIO 部署指南 + 配置说明 |

### 8.2 MinIO 本地环境部署

#### Docker 部署（推荐）

```bash
# 启动 MinIO 容器（9000 API 端口，9001 控制台端口）
docker run -d --name minio \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -v /path/to/data:/data \
  minio/minio server /data --console-address ":9001"

# 访问控制台：http://localhost:9001
# 创建存储桶 ruoyi-files（或应用启动时自动创建）
```

#### 二进制部署

```bash
# 下载 MinIO 二进制
wget https://dl.min.io/server/minio/release/linux-amd64/minio
chmod +x minio

# 启动
export MINIO_ROOT_USER=minioadmin
export MINIO_ROOT_PASSWORD=minioadmin
mkdir -p /data/minio
./minio server /data/minio --console-address ":9001"
```

### 8.3 配置说明

```yaml
ruoyi:
  file:
    # 存储策略：local | minio
    strategy: local
    minio:
      endpoint: http://localhost:9000
      accessKey: minioadmin
      secretKey: minioadmin
      bucketName: ruoyi-files
      publicDomain: http://localhost:9000
```

> 默认 `strategy: local`，不配置 MinIO 时行为不变，策略切换安全。

### 8.4 API 变更清单

| 接口 | 变更类型 | 说明 |
|------|----------|------|
| `POST /common/upload` | ✅ 改造 | 使用 `FileUploadStrategy`，支持 local/minio 自动切换 |
| `POST /common/uploads` | ✅ 改造 | 同上，多文件上传 |
| `POST /refresh` | 🆕 新增 | 双 Token 续期端点 |
| `POST /login` | ✅ 改造 | 响应新增 `refreshToken` 字段 |
| `POST /logout` | ✅ 改造 | 使用黑名单机制清理 token |
| 脱敏 Controller 接口 | 🆕 可选 | 由 `@Desensitize` 注解按需启用 |

### 8.5 前端适配说明

双 Token 前端适配需修改 Axios 拦截器：

```typescript
// 401 时自动尝试 refreshToken 续期
service.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        const res = await axios.post('/refresh', {}, {
          headers: { 'Refresh-Token': refreshToken }
        });
        setToken(res.data.token);
        error.config.headers['Authorization'] = res.data.token;
        return service(error.config);
      }
    }
    return Promise.reject(error);
  }
);
```