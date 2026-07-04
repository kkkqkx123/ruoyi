# RuoYi 后端项目架构分析

> 分析日期：2026-07-03
> 项目版本：RuoYi v3.9.2 (Spring Boot 4.0.6)
> 项目路径：`/workspace/ruoyi-backend/`

---

## 目录

1. [项目概述](#1-项目概述)
2. [Maven 多模块架构](#2-maven-多模块架构)
3. [模块依赖关系图](#3-模块依赖关系图)
4. [ruoyi-common 公共模块](#4-ruoyi-common-公共模块)
5. [ruoyi-framework 框架模块](#5-ruoyi-framework-框架模块)
6. [ruoyi-system 系统模块](#6-ruoyi-system-系统模块)
7. [ruoyi-admin 启动模块](#7-ruoyi-admin-启动模块)
8. [ruoyi-workorder 业务模块](#8-ruoyi-workorder-业务模块)
9. [ruoyi-generator 代码生成器模块](#9-ruoyi-generator-代码生成器模块)
10. [ruoyi-quartz 定时任务模块](#10-ruoyi-quartz-定时任务模块)
11. [配置体系](#11-配置体系)
12. [安全架构（Spring Security + JWT）](#12-安全架构spring-security--jwt)
13. [MyBatis 持久层架构](#13-mybatis-持久层架构)
14. [多数据源架构](#14-多数据源架构)
15. [请求处理管道](#15-请求处理管道)
16. [业务模块设计模式（以 workorder 为例）](#16-业务模块设计模式以-workorder-为例)
17. [分层架构总结](#17-分层架构总结)

---

## 1. 项目概述

RuoYi 后端项目是一个基于 **Spring Boot 3 + Spring Security + JWT + MyBatis + Redis** 的多模块企业级快速开发平台。

### 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 4.0.6 | 应用框架 |
| Java | 17 | 语言版本 |
| MyBatis Spring Boot | 4.0.1 | 持久层框架 |
| Druid | 1.2.28 | 数据库连接池 |
| PageHelper | 4.1.0 | 分页插件 |
| Redis (Lettuce) | 随 Spring Boot | 缓存/Token 存储 |
| JWT (jjwt) | 0.9.1 | Token 认证 |
| Fastjson2 | 2.0.62 | JSON 序列化 |
| OSHI | 7.3.0 | 系统监控 |
| SpringDoc | 3.0.3 | API 文档 |
| POI | 4.1.2 | Excel 导入导出 |
| Kaptcha | 2.3.3 | 验证码生成 |
| Hutool | 5.8.28 | 工具库（workorder 模块） |
| MySQL | 8.0+ | 数据库 |

### 核心设计理念

- **前后端分离**：后端提供 RESTful API，JWT Token 认证
- **多模块解耦**：Maven 多模块架构，各模块职责清晰
- **代码生成器驱动**：通过代码生成器快速生成 CRUD 代码，减少重复劳动
- **AOP 切面增强**：通过注解实现日志记录、数据权限过滤、限流等功能
- **动态数据源**：支持主从数据库读写分离

---

## 2. Maven 多模块架构

### 模块清单

项目聚合了 **7 个 Maven 子模块**，定义在父 POM `ruoyi-backend/pom.xml` 中：

| 模块 | 包名 | 描述 | 类型 |
|------|------|------|------|
| `ruoyi-admin` | `com.ruoyi.web` | Web 启动入口 + Controller 层 | JAR |
| `ruoyi-common` | `com.ruoyi.common` | 公共核心代码 | JAR |
| `ruoyi-framework` | `com.ruoyi.framework` | 框架核心配置 | JAR |
| `ruoyi-system` | `com.ruoyi.system` | 系统管理模块 | JAR |
| `ruoyi-workorder` | `com.ruoyi.workorder` | 设备工单模块 | JAR |
| `ruoyi-quartz` | `com.ruoyi.quartz` | 定时任务模块 | JAR |
| `ruoyi-generator` | `com.ruoyi.generator` | 代码生成器模块 | JAR |

### 父 POM 核心配置

```xml
<!-- 位于 /workspace/ruoyi-backend/pom.xml -->
<groupId>com.ruoyi</groupId>
<artifactId>ruoyi</artifactId>
<version>3.9.2</version>
<packaging>pom</packaging>

<modules>
    <module>ruoyi-admin</module>
    <module>ruoyi-framework</module>
    <module>ruoyi-system</module>
    <module>ruoyi-quartz</module>
    <module>ruoyi-generator</module>
    <module>ruoyi-common</module>
    <module>ruoyi-workorder</module>
</modules>
```

---

## 3. 模块依赖关系图

```
ruoyi-admin (启动入口)
  ├── ruoyi-framework (框架核心)
  │   ├── ruoyi-common (公共工具)
  │   └── ruoyi-system (系统业务)
  │       └── ruoyi-common
  ├── ruoyi-quartz (定时任务)
  │   ├── ruoyi-framework
  │   │   └── ... (同上)
  │   └── ruoyi-common
  ├── ruoyi-generator (代码生成器)
  │   ├── ruoyi-framework
  │   └── ruoyi-common
  └── ruoyi-workorder (设备工单)
      ├── ruoyi-framework
      ├── ruoyi-system
      └── ruoyi-common
```

所有模块依赖关系可归纳为：

```
任何业务模块 → ruoyi-framework → ruoyi-system (可选) → ruoyi-common
```

`ruoyi-common` 是底层依赖，不依赖任何其他项目模块。

---

## 4. ruoyi-common 公共模块

这是所有模块的基础依赖，提供工具类和核心抽象。包路径：`com.ruoyi.common.*`

### 4.1 架构分层

```
com.ruoyi.common
├── annotation/        # 自定义注解（7 个）
│   ├── Log.java           # 操作日志
│   ├── DataScope.java     # 数据权限过滤
│   ├── DataSource.java    # 多数据源切换
│   ├── Excel.java         # Excel 导出字段
│   ├── Excels.java        # Excel 多注解
│   ├── RateLimiter.java   # 接口限流
│   ├── RepeatSubmit.java  # 防重复提交
│   ├── Anonymous.java     # 允许匿名访问
│   └── Sensitive.java     # 数据脱敏
├── config/            # 配置相关
│   ├── RuoYiConfig.java   # 项目配置（读取 ruoyi.* 配置项）
│   └── serializer/        # 序列化器
├── constant/          # 常量定义
│   ├── Constants.java     # 通用常量（含 Dept 数据权限常量）
│   ├── HttpStatus.java    # HTTP 状态码
│   ├── CacheConstants.java # 缓存 Key 前缀
│   ├── UserConstants.java  # 用户相关常量
│   ├── GenConstants.java   # 代码生成器常量
│   └── ScheduleConstants.java # 任务常量
├── core/              # 核心抽象
│   ├── controller/
│   │   └── BaseController.java  # Controller 基类
│   ├── domain/
│   │   ├── BaseEntity.java      # 实体基类
│   │   ├── TreeEntity.java      # 树形实体基类
│   │   ├── AjaxResult.java      # 统一响应封装
│   │   ├── R.java               # Feign 响应封装
│   │   ├── TreeSelect.java      # 树形下拉封装
│   │   ├── model/
│   │   │   ├── LoginUser.java   # 登录用户（UserDetails）
│   │   │   ├── LoginBody.java   # 登录请求体
│   │   │   └── RegisterBody.java # 注册请求体
│   │   └── entity/
│   │       ├── SysUser.java     # 用户实体
│   │       ├── SysRole.java     # 角色实体
│   │       ├── SysMenu.java     # 菜单实体
│   │       ├── SysDept.java     # 部门实体
│   │       ├── SysDictData.java # 字典数据
│   │       └── SysDictType.java # 字典类型
│   ├── page/
│   │   ├── PageDomain.java      # 分页请求参数
│   │   ├── TableDataInfo.java   # 分页响应封装
│   │   └── TableSupport.java    # 分页请求解析工具
│   ├── redis/
│   │   └── RedisCache.java      # Redis 操作封装
│   └── text/                    # 文本处理工具
├── exception/         # 异常体系
│   ├── base/BaseException.java  # 基础异常
│   ├── ServiceException.java    # 业务异常
│   ├── GlobalException.java     # 全局异常
│   ├── DemoModeException.java   # 演示模式异常
│   ├── file/                    # 文件相关异常
│   ├── job/TaskException.java   # 任务异常
│   └── user/                    # 用户相关异常
├── enums/             # 枚举定义
│   ├── BusinessType.java        # 业务类型
│   ├── BusinessStatus.java      # 业务状态
│   ├── OperatorType.java        # 操作者类型
│   ├── DataSourceType.java      # 数据源类型
│   └── ... 
├── filter/            # Servlet 过滤器
│   ├── XssFilter.java           # XSS 过滤
│   ├── RepeatableFilter.java    # 可重复读过滤器
│   ├── RefererFilter.java       # 防盗链
│   └── PropertyPreExcludeFilter.java
└── utils/             # 工具类（20+ 个）
    ├── poi/ExcelUtil.java       # Excel 导入导出（核心）
    ├── spring/SpringUtils.java  # Spring 容器工具
    ├── SecurityUtils.java       # 安全工具（获取当前用户）
    ├── ip/IpUtils.java          # IP 工具
    ├── uuid/IdUtils.java        # UUID 生成
    ├── sql/SqlUtil.java         # SQL 安全过滤
    ├── file/FileUploadUtils.java # 文件上传
    ├── http/ServletUtils.java   # Servlet 工具
    ├── bean/BeanUtils.java      # Bean 操作
    ├── sign/Base64.java         # Base64 编解码
    └── ... 
```

### 4.2 BaseEntity 基类

所有业务实体的基类，定义了公共字段：

```java
public class BaseEntity implements Serializable {
    private String searchValue;      // 搜索值
    private String createBy;         // 创建者
    private Date createTime;         // 创建时间
    private String updateBy;         // 更新者
    private Date updateTime;         // 更新时间
    private String remark;           // 备注
    private Map<String, Object> params; // 请求参数（动态扩展）
}
```

其中 `params` 字段用于：
- 传递 `beginTime`/`endTime` 时间范围参数（前端传 `params.beginTime`、`params.endTime`）
- 传递 `dataScope` 数据权限过滤 SQL（由 `DataScopeAspect` 自动注入）

### 4.3 BaseController 基类

```java
public class BaseController {
    // 分页工具方法
    protected void startPage()       // 启动 PageHelper 分页
    protected void startOrderBy()    // 启动排序
    
    // 响应封装
    protected TableDataInfo getDataTable(List<?> list) // 分页响应
    public AjaxResult success(Object data)             // 成功响应
    public AjaxResult error(String message)            // 失败响应
    protected AjaxResult toAjax(int rows)              // 响应影响行数
    
    // 当前用户获取
    public LoginUser getLoginUser()   // 获取登录用户
    public Long getUserId()           // 获取用户ID
    public Long getDeptId()           // 获取部门ID
    public String getUsername()       // 获取用户名
}
```

### 4.4 AjaxResult 统一响应

```json
{
  "code": 200,      // 状态码
  "msg": "操作成功", // 消息
  "data": {}        // 数据
}
```

### 4.5 ExcelUtil 工具类

通过 `@Excel` 注解驱动 Excel 导入导出，支持的 Excel 操作：

```java
// 导出示例
ExcelUtil<WorkOrder> util = new ExcelUtil<>(WorkOrder.class);
util.exportExcel(response, list, "工单数据");

// @Excel 注解在字段上定义
@Excel(name = "工单编号", width = 25)
private String orderNo;
@Excel(name = "故障描述", readConverterExp = "导出时字典映射")
private String faultDesc;
```

---

## 5. ruoyi-framework 框架模块

框架核心配置模块，包路径：`com.ruoyi.framework.*`

### 5.1 架构分层

```
com.ruoyi.framework
├── config/                    # Spring 配置类（10 个）
│   ├── ApplicationConfig.java   # 应用配置（@MapperScan + AOP）
│   ├── SecurityConfig.java      # Spring Security 配置
│   ├── MyBatisConfig.java       # MyBatis SqlSessionFactory
│   ├── DruidConfig.java         # Druid 多数据源配置
│   ├── RedisConfig.java         # Redis 配置
│   ├── FilterConfig.java        # Filter 链注册
│   ├── ResourcesConfig.java     # 静态资源 + Cors + 拦截器
│   ├── CaptchaConfig.java       # 验证码配置
│   ├── I18nConfig.java          # 国际化配置
│   ├── ThreadPoolConfig.java    # 线程池配置
│   ├── ServerConfig.java        # 服务器配置
│   └── properties/              # 属性配置类
├── aspectj/                   # AOP 切面（4 个）
│   ├── LogAspect.java           # 操作日志切面
│   ├── DataScopeAspect.java     # 数据权限过滤
│   ├── DataSourceAspect.java    # 多数据源切换
│   └── RateLimiterAspect.java   # 限流切面
├── datasource/                # 动态数据源
│   ├── DynamicDataSource.java         # 动态数据源
│   └── DynamicDataSourceContextHolder.java # 数据源上下文
├── interceptor/               # 请求拦截器
│   ├── RepeatSubmitInterceptor.java   # 防重复提交
│   └── impl/SameUrlDataInterceptor.java
├── manager/                   # 异步管理器
│   ├── AsyncManager.java            # 异步任务管理器
│   ├── ShutdownManager.java         # 优雅关闭
│   └── factory/AsyncFactory.java    # 异步任务工厂
├── security/                  # 安全相关
│   ├── context/
│   │   ├── AuthenticationContextHolder.java # 认证上下文
│   │   └── PermissionContextHolder.java     # 权限上下文
│   ├── filter/
│   │   └── JwtAuthenticationTokenFilter.java # JWT 过滤器
│   └── handle/
│       ├── AuthenticationEntryPointImpl.java  # 认证失败处理
│       └── LogoutSuccessHandlerImpl.java      # 退出成功处理
└── web/                       # Web 层服务
    ├── domain/Server.java          # 服务器监控
    ├── exception/GlobalExceptionHandler.java # 全局异常处理
    └── service/
        ├── TokenService.java         # JWT Token 服务
        ├── SysLoginService.java      # 登录服务
        ├── SysPermissionService.java # 权限服务
        ├── SysPasswordService.java   # 密码服务
        ├── SysRegisterService.java   # 注册服务
        └── UserDetailsServiceImpl.java # UserDetailsService
```

### 5.2 关键配置详解

#### ApplicationConfig

```java
@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.ruoyi.**.mapper")   // 扫描所有子模块 Mapper
public class ApplicationConfig {}
```

这是 MyBatis Mapper 扫描的核心配置。`com.ruoyi.**.mapper` 通配符会匹配：
- `com.ruoyi.system.mapper` （系统模块）
- `com.ruoyi.workorder.mapper` （工单模块）
- `com.ruoyi.quartz.mapper` （定时任务模块）
- `com.ruoyi.generator.mapper` （生成器模块）

#### MyBatisConfig

自定义 `SqlSessionFactory` Bean，解决 MyBatis `typeAliasesPackage` 的星号通配问题：

```java
@Configuration
public class MyBatisConfig {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        // 1. 从 application.yml 读取配置
        String typeAliasesPackage = env.getProperty("mybatis.typeAliasesPackage");  // com.ruoyi.**.domain
        String mapperLocations = env.getProperty("mybatis.mapperLocations");       // classpath*:mapper/**/*Mapper.xml
        String configLocation = env.getProperty("mybatis.configLocation");         // classpath:mybatis/mybatis-config.xml
        
        // 2. 通配符解析：将 com.ruoyi.**.domain 展开为实际存在的包
        typeAliasesPackage = setTypeAliasesPackage(typeAliasesPackage);
        
        // 3. 构建 SqlSessionFactory
        sessionFactory.setTypeAliasesPackage(typeAliasesPackage);
        sessionFactory.setMapperLocations(resolveMapperLocations(mapperLocations));
        sessionFactory.setConfigLocation(configLocation);
        return sessionFactory.getObject();
    }
}
```

`application.yml` 中的 MyBatis 配置：

```yaml
mybatis:
  typeAliasesPackage: com.ruoyi.**.domain
  mapperLocations: classpath*:mapper/**/*Mapper.xml
  configLocation: classpath:mybatis/mybatis-config.xml
```

Mapper XML 文件扫描路径：`classpath*:mapper/**/*Mapper.xml`，匹配 `ruoyi-xxx/src/main/resources/mapper/**/*Mapper.xml`

### 5.3 AOP 切面系统

#### LogAspect - 操作日志

```java
@Aspect
@Component
public class LogAspect {
    @Before(value = "@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog) { /* 记录开始时间 */ }
    
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        // 记录操作日志到数据库（异步）
        handleLog(joinPoint, controllerLog, null, jsonResult);
        AsyncManager.me().execute(AsyncFactory.recordOper(operLog));
    }
}
```

使用方式：

```java
@Log(title = "工单管理", businessType = BusinessType.INSERT)
@PostMapping
public AjaxResult add(@RequestBody WorkOrder workOrder) { ... }
```

#### DataScopeAspect - 数据权限

```java
@Aspect
@Component
public class DataScopeAspect {
    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) {
        // 根据用户角色 dataScope 级别，拼接 SQL 过滤条件
        // 数据范围：全部(1) / 自定义(2) / 本部门(3) / 部门及以下(4) / 仅本人(5)
        String sql = getSqlByDataScope(user, deptAlias, userAlias);
        baseEntity.getParams().put("dataScope", " AND (" + sql + ")");
    }
}
```

支持 5 级数据权限，通过 `@DataScope(deptAlias = "d", userAlias = "u")` 注解自动注入 SQL 过滤条件。

---

## 6. ruoyi-system 系统模块

提供系统核心业务功能，包括用户、角色、菜单、部门、字典、通知等管理。

### 6.1 数据模型

| 表名 | 实体 | 说明 |
|------|------|------|
| `sys_user` | SysUser | 用户表 |
| `sys_role` | SysRole | 角色表 |
| `sys_menu` | SysMenu | 菜单权限表 |
| `sys_dept` | SysDept | 部门表 |
| `sys_dict_type` | SysDictType | 字典类型表 |
| `sys_dict_data` | SysDictData | 字典数据表 |
| `sys_notice` | SysNotice | 通知公告表 |
| `sys_config` | SysConfig | 参数配置表 |
| `sys_oper_log` | SysOperLog | 操作日志表 |
| `sys_logininfor` | SysLogininfor | 登录日志表 |
| `sys_post` | SysPost | 岗位表 |
| `sys_user_online` | SysUserOnline | 在线用户 |
| `sys_role_dept` | SysRoleDept | 角色部门关联 |
| `sys_role_menu` | SysRoleMenu | 角色菜单关联 |
| `sys_user_role` | SysUserRole | 用户角色关联 |
| `sys_user_post` | SysUserPost | 用户岗位关联 |

### 6.2 核心 Service

| Service | 说明 | 主要方法 |
|---------|------|---------|
| `ISysUserService` | 用户管理 | insertUser, updateUser, deleteUserByIds, resetPwd, selectUserByUserName |
| `ISysRoleService` | 角色管理 | insertRole, updateRole, updateRoleStatus, selectRoleList |
| `ISysMenuService` | 菜单管理 | selectMenuTreeByUserId, selectMenuListByRoleId |
| `ISysDictDataService` | 字典数据 | selectDictDataByType, selectDictDataList |
| `ISysDictTypeService` | 字典类型 | selectDictTypeList, checkDictTypeUnique |
| `ISysNoticeService` | 通知公告 | insertNotice, selectNoticeList |
| `ISysConfigService` | 参数配置 | selectConfigByKey, updateConfigByKey |
| `ISysOperLogService` | 操作日志 | insertOperlog, selectOperLogList |
| `ISysLogininforService` | 登录日志 | insertLogininfor, selectLogininforList |

### 6.3 通知公告

`SysNotice` 实体用于站内消息推送。在 workorder 模块中，紧急工单自动调用 `ISysNoticeService.insertNotice()` 推送通知：

```java
SysNotice notice = new SysNotice();
notice.setNoticeTitle("紧急工单：" + orderNo);
notice.setNoticeType("1");  // 1=通知
notice.setNoticeContent("有新的紧急工单需要处理...");
notice.setStatus("0");      // 0=正常
notice.setCreateBy("system");
noticeService.insertNotice(notice);
```

---

## 7. ruoyi-admin 启动模块

### 7.1 启动入口

```java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
// 排除 DataSource 自动配置，使用 Druid 手动配置
public class RuoYiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuoYiApplication.class, args);
    }
}
```

### 7.2 Controller 层

位于 `com.ruoyi.web.controller.*`，按功能分类：

| 包路径 | 功能 | 关键 Controller |
|--------|------|----------------|
| `controller/common/` | 通用功能 | CaptchaController（验证码）, CommonController（通用请求） |
| `controller/monitor/` | 系统监控 | CacheController, ServerController, SysLogininforController, SysOperlogController, SysUserOnlineController |
| `controller/system/` | 系统管理 | SysUserController, SysRoleController, SysMenuController, SysDeptController, SysDictDataController, SysDictTypeController, SysNoticeController, SysConfigController, SysPostController, SysProfileController, SysLoginController, SysRegisterController |
| `controller/tool/` | 工具测试 | TestController |

### 7.3 认证流程 Controller

`SysLoginController` 提供登录接口：

```
POST /login
  Body: { username, password, code, uuid }
  → TokenService.createToken() 生成 JWT
  → 返回 { token: "Bearer xxx" }

POST /register
  Body: { username, password }
  → SysRegisterService.register()

POST /logout
  → TokenService.delLoginUser()
```

---

## 8. ruoyi-workorder 业务模块

这是自定义的业务模块，于整合阶段创建，使用纯 MyBatis 实现。

### 8.1 数据模型

| 表名 | 实体 | 说明 |
|------|------|------|
| `device_info` | DeviceInfo | 设备信息表 |
| `work_order` | WorkOrder | 工单主表 |
| `work_order_record` | WorkOrderRecord | 工单维修记录表 |

### 8.2 标准四层架构

```
ruoyi-workorder
├── controller/          // Controller 层 - REST API 入口
│   ├── WorkOrderController.java
│   ├── WorkOrderRecordController.java
│   └── DeviceInfoController.java
├── domain/              // Domain 层 - 实体对象
│   ├── WorkOrder.java           (extends BaseEntity)
│   ├── WorkOrderRecord.java     (extends BaseEntity)
│   ├── DeviceInfo.java          (extends BaseEntity)
│   ├── WorkOrderStats.java      (VO - 统计看板)
│   └── FaultTopDevice.java      (VO - 故障排行)
├── mapper/              // Mapper 层 - 数据访问
│   ├── WorkOrderMapper.java
│   ├── WorkOrderRecordMapper.java
│   └── DeviceInfoMapper.java
├── service/             // Service 层 - 业务逻辑
│   ├── IWorkOrderService.java
│   ├── IWorkOrderRecordService.java
│   ├── IDeviceInfoService.java
│   └── impl/
│       ├── WorkOrderServiceImpl.java
│       ├── WorkOrderRecordServiceImpl.java
│       └── DeviceInfoServiceImpl.java
└── resources/mapper/workorder/   // MyBatis XML
    ├── WorkOrderMapper.xml
    ├── WorkOrderRecordMapper.xml
    └── DeviceInfoMapper.xml
```

### 8.3 Controller 设计模式

所有 Controller 遵循 RuoYi 标准模式：

```java
@RestController
@RequestMapping("/workorder/order")
public class WorkOrderController extends BaseController {
    
    @Autowired
    private IWorkOrderService workOrderService;

    // 分页查询
    @PreAuthorize("@ss.hasPermi('workorder:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(WorkOrder workOrder) {
        startPage();
        List<WorkOrder> list = workOrderService.selectWorkOrderList(workOrder);
        return getDataTable(list);
    }

    // 详情
    @PreAuthorize("@ss.hasPermi('workorder:order:query')")
    @GetMapping("/{orderId}")
    public AjaxResult getInfo(@PathVariable Long orderId) { ... }

    // 新增
    @PreAuthorize("@ss.hasPermi('workorder:order:add')")
    @PostMapping
    public AjaxResult add(@RequestBody WorkOrder workOrder) { ... }

    // 修改
    @PreAuthorize("@ss.hasPermi('workorder:order:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody WorkOrder workOrder) { ... }

    // 删除
    @PreAuthorize("@ss.hasPermi('workorder:order:remove')")
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds) { ... }

    // 自定义业务接口
    @PreAuthorize("@ss.hasPermi('workorder:order:assign')")
    @PutMapping("/batchAssign")
    public AjaxResult batchAssign(@RequestBody Map<String, Object> params) { ... }

    // 统计看板
    @PreAuthorize("@ss.hasPermi('workorder:order:stats')")
    @GetMapping("/stats")
    public AjaxResult stats() { ... }

    // Excel 导出
    @PreAuthorize("@ss.hasPermi('workorder:order:export')")
    @GetMapping("/export")
    public void export(HttpServletResponse response, WorkOrder workOrder) { ... }
}
```

### 8.4 Service 层核心业务逻辑

```java
@Service
public class WorkOrderServiceImpl implements IWorkOrderService {

    // 1. 新增工单 - 自动生成编号 + 紧急通知推送
    @Transactional(rollbackFor = Exception.class)
    public int insertWorkOrder(WorkOrder workOrder) {
        String orderNo = "WO" + DateUtil.format(new Date(), "yyyyMMdd") 
                        + IdUtil.getSnowflakeNextIdStr(); // Hutool 雪花算法
        workOrder.setOrderNo(orderNo);
        workOrder.setOrderStatus("0"); // 初始状态：未派单
        if ("2".equals(workOrder.getUrgencyLevel()) || "3".equals(workOrder.getUrgencyLevel())) {
            pushUrgentNotice(workOrder); // 紧急工单推通知
        }
        return workOrderMapper.insertWorkOrder(workOrder);
    }

    // 2. 批量分配 - 状态流转：0→1
    @Transactional(rollbackFor = Exception.class)
    public void batchAssign(Long[] orderIds, String assignTo) {
        for (Long orderId : orderIds) {
            WorkOrder order = workOrderMapper.selectWorkOrderById(orderId);
            order.setAssignTo(assignTo);
            order.setAssignTime(new Date());
            order.setOrderStatus("1"); // 已派单
            workOrderMapper.updateWorkOrder(order);
        }
    }

    // 3. 完成工单 - 状态流转：2→3（含校验）
    @Transactional(rollbackFor = Exception.class)
    public void completeWorkOrder(WorkOrderRecord record) {
        WorkOrder order = workOrderMapper.selectWorkOrderById(record.getOrderId());
        if (!"2".equals(order.getOrderStatus())) {
            throw new ServiceException("仅维修中的工单可以完成");
        }
        if (StringUtils.isEmpty(record.getRepairSolution())) {
            throw new ServiceException("请填写维修方案");
        }
        if (StringUtils.isEmpty(record.getImageUrls())) {
            throw new ServiceException("请上传至少一张维修图片");
        }
        workOrderRecordMapper.insertWorkOrderRecord(record);
        order.setOrderStatus("3");
        order.setFinishTime(new Date());
        workOrderMapper.updateWorkOrder(order);
    }

    // 4. 归档工单 - 状态流转：3→4
    @Transactional(rollbackFor = Exception.class)
    public void archiveWorkOrder(Long orderId, String archiveBy, String archiveRemark) {
        WorkOrder order = workOrderMapper.selectWorkOrderById(orderId);
        if (!"3".equals(order.getOrderStatus())) {
            throw new ServiceException("仅已完成的工单可以归档");
        }
        order.setOrderStatus("4");
        order.setArchiveTime(new Date());
        workOrderMapper.updateWorkOrder(order);
    }
}
```

### 8.5 Mapper XML 多表联查

```xml
<sql id="selectWorkOrderVo">
    SELECT wo.*, di.device_name, di.device_code,
           ru1.user_name AS reporter_name,
           ru2.user_name AS assign_name,
           (SELECT COUNT(*) FROM work_order_record r WHERE r.order_id = wo.order_id) AS record_count
    FROM work_order wo
    LEFT JOIN device_info di ON wo.device_id = di.device_id
    LEFT JOIN sys_user ru1 ON wo.reporter_by = ru1.user_name
    LEFT JOIN sys_user ru2 ON wo.assign_to = ru2.user_name
</sql>
```

### 8.6 统计看板 SQL

```xml
<!-- 当月工单统计 - 聚合 CASE WHEN 实现 -->
<select id="selectWorkOrderStats">
    SELECT
        COUNT(*) AS totalCount,
        SUM(CASE WHEN order_status IN ('0','1') THEN 1 ELSE 0 END) AS pendingCount,
        SUM(CASE WHEN order_status = '2' THEN 1 ELSE 0 END) AS inProgressCount,
        SUM(CASE WHEN order_status = '3' THEN 1 ELSE 0 END) AS completedCount
    FROM work_order
    WHERE DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')
</select>

<!-- 故障率 Top10 设备 - GROUP BY + LIMIT -->
<select id="selectFaultTopDevices">
    SELECT di.device_id, di.device_name, COUNT(*) AS faultCount
    FROM work_order wo
    LEFT JOIN device_info di ON wo.device_id = di.device_id
    WHERE DATE_FORMAT(wo.create_time, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')
    GROUP BY wo.device_id, di.device_name
    ORDER BY faultCount DESC LIMIT 10
</select>
```

---

## 9. ruoyi-generator 代码生成器模块

基于 Velocity 模板引擎，根据数据库表结构自动生成 CRUD 代码。

### 9.1 核心流程

```
1. 用户选择数据库表 → GenController.importTableSave()
2. 读取表结构和列结构 → GenTableMapper / GenTableColumnMapper
3. 配置生成选项 → 模块名、包名、权限前缀、父菜单等
4. 生成代码 → VelocityUtils.renderByAlibabaTemplate()
   ├── Controller.java
   ├── Service.java & ServiceImpl.java
   ├── Mapper.java
   ├── Domain.java (支持主子表)
   ├── Mapper.xml
   ├── Vue 页面 (v2/v3/v3ts 三种模板)
   ├── TypeScript API
   └── SQL 菜单脚本
```

### 9.2 模板目录

```
resources/vm/
├── java/                        # Java 模板
│   ├── controller.java.vm       # Controller
│   ├── domain.java.vm           # 主表实体
│   ├── sub-domain.java.vm       # 子表实体
│   ├── mapper.java.vm           # Mapper
│   ├── service.java.vm          # Service 接口
│   └── serviceImpl.java.vm      # Service 实现
├── vue/                         # Vue 前端模板
│   ├── v3/                      # Vue 3 + JS
│   ├── v3ts/                    # Vue 3 + TypeScript
│   ├── index.vue.vm             # 标准页
│   ├── index-tree.vue.vm        # 树形页
│   └── view.vue.vm              # 详情页
├── js/api.js.vm                 # JavaScript API
├── ts/                          # TypeScript API
│   ├── api.ts.vm
│   ├── type.ts.vm
│   └── index.ts.vm
├── xml/mapper.xml.vm            # MyBatis XML
└── sql/sql.vm                   # 菜单 SQL
```

### 9.3 代码生成器配置

```yaml
# generator.yml
gen:
  author: ruoyi
  packageName: com.ruoyi
  autoRemovePre: true
  tablePrefix: sys_
```

---

## 10. ruoyi-quartz 定时任务模块

集成 Quartz 实现动态任务调度。

### 10.1 核心组件

| 组件 | 说明 |
|------|------|
| `SysJobController` | 任务管理 REST API |
| `SysJobLogController` | 任务日志 REST API |
| `ScheduleConfig` | Quartz Scheduler 配置 |
| `ScheduleUtils` | 创建/更新/删除 Quartz Job |
| `AbstractQuartzJob` | 任务执行抽象类 |
| `QuartzDisallowConcurrentExecution` | 禁止并发执行 |
| `QuartzJobExecution` | 允许并发执行 |
| `JobInvokeUtil` | 通过反射调用任务 Bean |
| `CronUtils` | Cron 表达式验证 |
| `RyTask` | 示例任务 |

### 10.2 任务表结构

```sql
sys_job       -- 任务定义表（job_id, bean_name, method, cron_expression, status）
sys_job_log   -- 任务执行日志（job_log_id, job_name, status, exception_info）
```

---

## 11. 配置体系

### 11.1 配置文件

| 文件 | 路径 | 说明 |
|------|------|------|
| `application.yml` | `ruoyi-admin/src/main/resources/` | 主配置 |
| `application-druid.yml` | `ruoyi-admin/src/main/resources/` | Druid 数据源 |
| `banner.txt` | `ruoyi-admin/src/main/resources/` | 启动 Banner |
| `logback.xml` | `ruoyi-admin/src/main/resources/` | 日志配置 |
| `mybatis/mybatis-config.xml` | `ruoyi-admin/src/main/resources/` | MyBatis 全局配置 |
| `i18n/messages.properties` | `ruoyi-admin/src/main/resources/` | 国际化消息 |
| `generator.yml` | `ruoyi-generator/src/main/resources/` | 代码生成器配置 |

### 11.2 application.yml 核心配置分段

```yaml
# ===== 项目配置 =====
ruoyi:
  name: RuoYi                        # 项目名称
  version: 3.9.2                     # 项目版本
  profile: D:/ruoyi/uploadPath       # 文件上传路径
  addressEnabled: false              # IP 定位开关
  captchaType: math                  # 验证码类型

# ===== 服务配置 =====
server:
  port: 8080
  servlet.context-path: /            # 应用路径
  tomcat:
    max-threads: 800                 # 最大线程数
    accept-count: 1000               # 排队数

# ===== Spring 配置 =====
spring:
  profiles.active: druid             # 激活数据源配置
  servlet.multipart:
    max-file-size: 10MB
    max-request-size: 20MB
  jackson:
    time-zone: GMT+8
    date-format: yyyy-MM-dd HH:mm:ss
  data.redis:
    host: localhost
    port: 6379
    lettuce.pool.max-active: 8

# ===== Token 配置 =====
token:
  header: Authorization
  secret: abcdefghijklmnopqrstuvwxyz
  expireTime: 30                     # 分钟

# ===== MyBatis 配置 =====
mybatis:
  typeAliasesPackage: com.ruoyi.**.domain
  mapperLocations: classpath*:mapper/**/*Mapper.xml
  configLocation: classpath:mybatis/mybatis-config.xml

# ===== PageHelper 分页配置 =====
pagehelper:
  helperDialect: mysql
  supportMethodsArguments: true
  params: count=countSql

# ===== 安全配置 =====
springdoc:
  api-docs.path: /v3/api-docs
  swagger-ui.path: /swagger-ui.html

referer:
  enabled: false
xss:
  enabled: true
  excludes: /system/notice
  urlPatterns: /system/*,/monitor/*,/tool/*
```

---

## 12. 安全架构（Spring Security + JWT）

### 12.1 认证流程

```
用户登录
  ↓
POST /login （无需认证）
  ↓
UsernamePasswordAuthenticationToken
  ↓
UserDetailsServiceImpl.loadUserByUsername()
  ↓
SysLoginService.validate() → 校验验证码 → 校验密码
  ↓
SysPermissionService.getMenuPermission() 获取权限
  ↓
TokenService.createToken() → 生成 UUID → 写入 Redis（30分钟）
  ↓
创建 JWT → 返回 Bearer token
  ↓
客户端后续请求携带 Authorization: Bearer xxx
```

### 12.2 请求鉴权流程

```
客户端请求
  ↓
JwtAuthenticationTokenFilter （OncePerRequestFilter）
  ↓
TokenService.getLoginUser(request)
  ↓
从 Header 提取 token → 解析 JWT Claims → 获取 uuid → Redis 查询 LoginUser
  ↓
verifyToken() 检查有效期，20分钟内自动刷新
  ↓
创建 UsernamePasswordAuthenticationToken → 设置到 SecurityContextHolder
  ↓
chain.doFilter() 继续执行
  ↓
SecurityConfig.authorizeHttpRequests()
  ├── /login, /register, /captchaImage → permitAll
  ├── /profile/** → permitAll (静态资源)
  ├── /swagger-ui/**, /v3/api-docs/** → permitAll
  └── 其他所有请求 → authenticated
  ↓
@PreAuthorize("@ss.hasPermi('xxx:xxx:xxx')") 方法级权限校验
  ↓
PermissionService.hasPermi() → 检查用户权限列表
```

### 12.3 SecurityConfig 配置

```java
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Configuration
public class SecurityConfig {
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .authorizeHttpRequests((requests) -> {
                // 放行登录、注册、验证码、静态资源、Swagger
                permitAllUrl.getUrls().forEach(url -> requests.requestMatchers(url).permitAll());
                requests.requestMatchers("/login", "/register", "/captchaImage").permitAll()
                    .requestMatchers(HttpMethod.GET, "/", "/*.html", "/profile/**").permitAll()
                    .requestMatchers("/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**", "/druid/**").permitAll()
                    .anyRequest().authenticated();
            })
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler))
            .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class)
            .build();
    }
}
```

### 12.4 权限模型

- **菜单权限**：`sys_menu` 表，perms 字段存储 `workorder:order:list` 等
- **角色权限**：`sys_role` + `sys_role_menu` 关联
- **@PreAuthorize**：通过 `@ss.hasPermi()` 或 `@ss.hasRole()` SpEL 表达式校验

权限标识格式：
```
模块:功能:操作
  workorder:order:list     → 查询工单
  workorder:order:add      → 新增工单
  workorder:order:edit     → 修改工单
  workorder:order:remove   → 删除工单
  workorder:order:assign   → 派单
  workorder:order:export   → 导出工单
  workorder:order:stats    → 统计看板
```

---

## 13. MyBatis 持久层架构

### 13.1 整体配置

- **不使用 MyBatis-Plus**：纯 MyBatis 的 XML 映射方式
- **自定义 SqlSessionFactory**：`MyBatisConfig` 解决 `**` 通配符扫描
- **Mapper 扫描**：`@MapperScan("com.ruoyi.**.mapper")` 在 `ApplicationConfig`
- **别名扫描**：`com.ruoyi.**.domain` → 展开为所有实际存在的包
- **XML 扫描**：`classpath*:mapper/**/*Mapper.xml`

### 13.2 标准 Mapper 接口模式

```java
// 纯接口方式：不继承任何基类
public interface WorkOrderMapper {
    List<WorkOrder> selectWorkOrderList(WorkOrder workOrder);
    WorkOrder selectWorkOrderById(Long orderId);
    int insertWorkOrder(WorkOrder workOrder);
    int updateWorkOrder(WorkOrder workOrder);
    int deleteWorkOrderById(Long orderId);
    int deleteWorkOrderByIds(Long[] orderIds);
}
```

### 13.3 标准 XML 映射模式

```xml
<mapper namespace="com.ruoyi.workorder.mapper.WorkOrderMapper">
    <!-- 1. 基础 ResultMap -->
    <resultMap id="WorkOrderResult" type="WorkOrder">
        <id property="orderId" column="order_id"/>
        <result property="orderNo" column="order_no"/>
        ...
    </resultMap>

    <!-- 2. 可复用 SQL 片段 -->
    <sql id="selectWorkOrderVo">SELECT wo.*, di.device_name ... FROM ...</sql>

    <!-- 3. 分页查询 → 自动被 PageHelper 增强 -->
    <select id="selectWorkOrderList" parameterType="WorkOrder" resultMap="WorkOrderResult">
        <include refid="selectWorkOrderVo"/>
        <where>
            <if test="orderNo != null and orderNo != ''">AND wo.order_no LIKE CONCAT('%', #{orderNo}, '%')</if>
            <if test="deviceName != null and deviceName != ''">AND di.device_name LIKE CONCAT('%', #{deviceName}, '%')</if>
            <if test="params.beginTime != null and params.beginTime != ''">AND wo.create_time &gt;= #{params.beginTime}</if>
            <if test="params.endTime != null and params.endTime != ''">AND wo.create_time &lt;= #{params.endTime}</if>
        </where>
        ORDER BY wo.create_time DESC
    </select>

    <!-- 4. 动态 INSERT（if 非空判断） -->
    <insert id="insertWorkOrder" useGeneratedKeys="true" keyProperty="orderId">
        INSERT INTO work_order (<if test="orderNo != null">order_no,</if> ...) VALUES (...)
    </insert>

    <!-- 5. 动态 UPDATE（set 标签） -->
    <update id="updateWorkOrder">
        UPDATE work_order <set>
            <if test="orderStatus != null">order_status = #{orderStatus},</if>
            update_time = SYSDATE()
        </set> WHERE order_id = #{orderId}
    </update>

    <!-- 6. 批量删除 → foreach 集合遍历 -->
    <delete id="deleteWorkOrderByIds">
        DELETE FROM work_order WHERE order_id IN
        <foreach item="orderId" collection="array" open="(" separator="," close=")">#{orderId}</foreach>
    </delete>
</mapper>
```

### 13.4 分页实现

PageHelper 通过 AOP 拦截，对 Mapper 查询自动增加 `LIMIT` 语句：

```java
// Controller 层调用
@GetMapping("/list")
public TableDataInfo list(WorkOrder workOrder) {
    startPage();  // → PageHelper.startPage(pageNum, pageSize)
    List<WorkOrder> list = workOrderService.selectWorkOrderList(workOrder);
    return getDataTable(list);  // → new PageInfo<>(list), 包装 total 和 rows
}
```

`startPage()` 从请求参数中自动读取 `pageNum` 和 `pageSize`（通过 `TableSupport` 解析）。

---

## 14. 多数据源架构

### 14.1 配置

```yaml
spring.datasource:
  druid:
    master:
      url: jdbc:mysql://localhost:3306/ry-vue?useSSL=true&...
      username: root
      password: password
    slave:
      enabled: false    # 默认关闭从库
```

### 14.2 动态数据源

```java
// 自定义 @DataSource 注解切换数据源
@DataSource(DataSourceType.SLAVE)  // 切换到从库
public List<WorkOrder> selectWorkOrderList(WorkOrder workOrder) { ... }

// AOP 切面自动切换
@Around("@annotation(dataSource)")
public Object around(ProceedingJoinPoint point, DataSource dataSource) {
    DynamicDataSourceContextHolder.setDataSourceType(dataSource.value().name());
    try { return point.proceed(); }
    finally { DynamicDataSourceContextHolder.clearDataSourceType(); }
}
```

---

## 15. 请求处理管道

### 15.1 Filter 链（按顺序）

```
1. XssFilter               → XSS 攻击过滤（/system/*, /monitor/*, /tool/*）
2. RepeatableFilter        → 可重复读取 Request Body
3. RefererFilter           → 防盗链过滤（/profile/*）
4. CorsFilter              → 跨域处理
5. JwtAuthenticationTokenFilter → JWT 认证
6. RepeatSubmitInterceptor → 防重复提交
```

### 15.2 完整请求生命周期

```
HTTP 请求
  ↓
Filter 链（XSS → Repeatable → Referer → Cors → JWT → 防重复提交）
  ↓
DispatcherServlet
  ↓
Interceptor（RepeatSubmitInterceptor 校验）
  ↓
AOP 切面
  ├── @Log → 记录操作日志（异步写入 sys_oper_log）
  ├── @DataScope → 拼接数据权限 SQL
  ├── @DataSource → 切换数据源
  └── @RateLimiter → 限流校验
  ↓
Controller 方法
  ↓
Service 方法（声明式事务 @Transactional）
  ↓
Mapper 接口
  ↓
MyBatis（PageHelper 自动增强 LIMIT）
  ↓
数据库
  ↓
返回 JSON（AjaxResult 或 TableDataInfo）
  ↓
GlobalExceptionHandler 统一异常处理
```

---

## 16. 业务模块设计模式（以 workorder 为例）

### 16.1 新建业务模块的标准步骤

1. **创建 Maven 子模块**：`ruoyi-workorder/pom.xml`，依赖 `ruoyi-common` + `ruoyi-system` + `ruoyi-framework`
2. **在父 POM 注册**：`<module>ruoyi-workorder</module>`
3. **在 ruoyi-admin/pom.xml 引用**：添加 `ruoyi-workorder` 依赖
4. **建表 SQL**：含 DDL + 字典 INSERT + 菜单 INSERT（13 个菜单项）
5. **编写 Domain**：继承 `BaseEntity`，添加业务字段
6. **编写 Mapper**：纯接口 + XML 映射（不含 MyBatis-Plus）
7. **编写 Service**：接口 + 实现，添加事务和业务校验
8. **编写 Controller**：继承 `BaseController`，`@PreAuthorize` 权限注解
9. **注册菜单和权限**：SQL 插入 `sys_menu` 表
10. **注册字典**：SQL 插入 `sys_dict_type` + `sys_dict_data` 表

### 16.2 代码组织规范

```java
// Domain 继承 BaseEntity → 自动获得 createBy/createTime/updateBy/updateTime/remark/params
public class WorkOrder extends BaseEntity {
    private Long orderId;
    // ... 业务字段
    // 非数据库字段（多表联查结果）
    private String deviceName;   // 关联设备名称
    private String reporterName; // 关联报修人姓名
    private String assignName;   // 关联维修员姓名
    private Integer recordCount; // 关联记录数量
}

// Controller 继承 BaseController → 自动获得分页/响应封装/当前用户
public class WorkOrderController extends BaseController {
    // startPage() + getDataTable() + success() + toAjax()
}

// Service 接口定义业务方法
public interface IWorkOrderService {
    List<WorkOrder> selectWorkOrderList(WorkOrder workOrder);
    WorkOrder selectWorkOrderById(Long orderId);
    @Transactional int insertWorkOrder(WorkOrder workOrder);
    int updateWorkOrder(WorkOrder workOrder);
    int deleteWorkOrderByIds(Long[] orderIds);
}

// Mapper 接口，无继承
public interface WorkOrderMapper {
    List<WorkOrder> selectWorkOrderList(WorkOrder workOrder);
    // ...
}
```

### 16.3 状态机设计（工单流转）

```
状态值  状态名称    触发操作          前置状态
─────────────────────────────────────────────
0       未派单      提交工单          -
1       已派单      批量/单个派单     0
2       维修中      接单              1
3       已完成      完成+上传记录      2（含校验）
4       已归档      归档              3
```

状态流转不可逆，Service 层通过校验确保：

```java
// WorkOrderServiceImpl.java
if (!"2".equals(order.getOrderStatus())) throw new ServiceException("仅维修中的工单可以完成");
if (!"3".equals(order.getOrderStatus())) throw new ServiceException("仅已完成的工单可以归档");
```

---

## 17. 分层架构总结

### 总体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP 请求/响应                            │
├─────────────────────────────────────────────────────────────────┤
│                    Filter 链（XSS/CORS/JWT 等）                   │
├─────────────────────────────────────────────────────────────────┤
│    ruoyi-admin       Controller 层（@RestController）             │
│                      ├── system/*      系统管理                   │
│                      ├── monitor/*     系统监控                   │
│                      ├── common/*      通用功能                   │
│                      └── tool/*        工具测试                   │
├─────────────────────────────────────────────────────────────────┤
│    ruoyi-framework   核心框架层                                    │
│    ┌──────────────────────────────────────────────────────────┐  │
│    │ AOP 切面          │ 安全框架    │ 配置中心    │ 多数据源  │  │
│    │ LogAspect         │ Security    │ MyBatis    │ Druid     │  │
│    │ DataScopeAspect   │ JWT Filter  │ Redis       │ Dynamic  │  │
│    │ RateLimiterAspect │ TokenService│ ThreadPool │ Async    │  │
│    └──────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│    ruoyi-system      系统业务层                                   │
│    │ SysUser/Role/Menu/Dept/Dict/Notice/Config/Log              │
│    │ Service → Mapper → XML → sys_xxx 表                        │
├─────────────────────────────────────────────────────────────────┤
│    ruoyi-workorder   自定义业务层                                 │
│    │ DeviceInfo/WorkOrder/WorkOrderRecord                        │
│    │ Service → Mapper → XML → device_info/work_order/record     │
├─────────────────────────────────────────────────────────────────┤
│    ruoyi-quartz      定时任务层                                   │
│    │ SysJob/SysJobLog → Quartz Scheduler                        │
├─────────────────────────────────────────────────────────────────┤
│    ruoyi-generator   代码生成层                                   │
│    │ Velocity 模板 → 生成 Controller/Service/Mapper/Vue         │
├─────────────────────────────────────────────────────────────────┤
│    ruoyi-common      公共基础层                                   │
│    │ BaseEntity/BaseController/AjaxResult/RedisCache/PageHelper │
│    │ ExcelUtil/SpringUtils/SecurityUtils/FileUploadUtils        │
│    │ 异常体系（ServiceException/GlobalException）                │
│    │ 自定义注解（@Log/@DataScope/@Excel/@RateLimiter）           │
└─────────────────────────────────────────────────────────────────┘
```

### 架构设计原则

| 原则 | 体现 |
|------|------|
| **单一职责** | 7 个模块各司其职，不跨职责 |
| **依赖倒置** | Service 依赖接口，Controller 依赖 Service 接口 |
| **开闭原则** | 新增业务只需创建新模块，无需修改现有框架 |
| **AOP 分离关注点** | 日志/数据权限/限流通过注解 + 切面实现 |
| **统一响应** | AjaxResult 统一格式，前端统一处理 |
| **无侵入扩展** | 代码生成器的代码可以自由定制，不锁定 |
| **纯 MyBatis** | 不使用 MyBatis-Plus，保持与官方项目一致 |

### 目录结构总览

```
ruoyi-backend/
├── pom.xml                      # 父 POM，聚合 7 个子模块
├── ruoyi-admin/                 # Web 启动入口 + Controller
├── ruoyi-common/                # 公共工具 + 注解 + 核心抽象
├── ruoyi-framework/             # 框架配置 + AOP + Security
├── ruoyi-system/                # 系统业务 + Mapper + XML
├── ruoyi-workorder/             # 设备工单业务模块
├── ruoyi-quartz/                # 定时任务
├── ruoyi-generator/             # 代码生成器
├── sql/                         # SQL 脚本
│   ├── ry_20260417.sql          # 系统表
│   ├── quartz.sql               # Quartz 表
│   └── device-workorder.sql     # 工单模块表
└── docs/                        # 项目文档
    └── architecture-analysis.md # 本文件
```