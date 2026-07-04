# 开发环境配置指南

> 文档版本：v1.1 | 编写日期：2026-07-03 | 适用项目：RuoYi 后台管理系统

---

## 目录

1. [项目模块概览](#1-项目模块概览)
2. [Java 环境要求](#2-java-环境要求)
3. [MinIO 对象存储环境配置](#3-minio-对象存储环境配置)
4. [Maven 依赖仓库配置](#4-maven-依赖仓库配置)
5. [数据库与缓存配置](#5-数据库与缓存配置)
6. [应用启动与构建](#6-应用启动与构建)
7. [完整配置 checklist](#7-完整配置-checklist)

---

## 1. 项目模块概览

| 模块 | 路径 | 说明 |
|------|------|------|
| `ruoyi-admin` | `ruoyi-backend/ruoyi-admin` | 启动入口、Controller 层、配置、静态资源 |
| `ruoyi-common` | `ruoyi-backend/ruoyi-common` | 公共工具类、注解、异常、枚举、配置类 |
| `ruoyi-framework` | `ruoyi-backend/ruoyi-framework` | 框架核心：安全认证、AOP、拦截器、线程池 |
| `ruoyi-system` | `ruoyi-backend/ruoyi-system` | 系统管理模块（用户、角色、菜单、字典等） |
| `ruoyi-quartz` | `ruoyi-backend/ruoyi-quartz` | 定时任务调度 |
| `ruoyi-generator` | `ruoyi-backend/ruoyi-generator` | 代码生成器 |
| `ruoyi-workorder` | `ruoyi-backend/ruoyi-workorder` | 工单管理业务模块 |

> 前端项目在 `ruoyi/` 目录（Vue 3 + Vite）。

---

## 2. Java 环境要求

### 2.1 JDK 版本

POM 中声明 `java.version=17`，兼容 JDK 17～25。**注意：**

- 若使用 JDK 25（当前沙箱环境），byte 字面量需要显式 `(byte)` 转换：

```java
// JDK 17 可省略 (byte) 转换
byte[] header = new byte[] { 0xD0, 0xCF, 0x11, 0xE0 };

// JDK 25 必须显式 (byte) 转换（0xD0=208, 0xCF=207, 0xE0=224 均 > 127）
byte[] header = new byte[] { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0 };
```

### 2.2 安装方式

```bash
# 查看当前 JDK 版本
java -version

# 推荐使用 SDKMAN 管理多版本 JDK
curl -s "https://get.sdkman.io" | bash
sdk install java 17.0.14-tem
sdk use java 17.0.14-tem
```

---

## 3. MinIO 对象存储环境配置

### 3.1 Docker 部署（推荐）

```bash
# 拉取镜像
docker pull minio/minio

# 创建数据目录
mkdir -p /data/minio

# 启动 MinIO 容器
docker run -d --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -v /data/minio:/data \
  minio/minio server /data --console-address ":9001"
```

**参数说明：**

| 参数 | 值 | 说明 |
|------|-----|------|
| `-p 9000:9000` | API 端口 | MinIO S3 兼容 API |
| `-p 9001:9001` | 控制台端口 | Web 管理界面 |
| `MINIO_ROOT_USER` | `minioadmin` | 管理员用户名 |
| `MINIO_ROOT_PASSWORD` | `minioadmin` | 管理员密码 |
| `-v /data/minio:/data` | 持久化数据卷 | 映射到宿主机目录 |

### 3.2 验证 MinIO 服务

```bash
# 查看容器状态
docker ps | grep minio

# 查看启动日志
docker logs minio

# 控制台访问
# 浏览器打开 http://localhost:9001，使用 minioadmin/minioadmin 登录
```

### 3.3 创建存储桶

登录 MinIO 控制台后，手动创建 `ruoyi-files` 存储桶，或将 `ruoyi.file.strategy` 切换为 `minio` 后启动应用，`MinIoUtils.init()` 会自动创建不存在的 bucket。

### 3.4 应用配置

`application.yml` 中已预置 MinIO 配置段：

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

> **切换说明**：`strategy` 默认为 `local`，不配置 MinIO 时行为不变。将 `strategy` 改为 `minio` 后，上传文件自动写入 MinIO。

### 3.5 非 Docker 环境部署

如果宿主机没有 Docker，也可直接下载二进制部署：

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

---

## 4. Maven 依赖仓库配置

### 4.1 问题现象

```bash
mvn compile
# ERROR: Non-resolvable import POM: Could not transfer artifact
# org.springframework.boot:spring-boot-dependencies:pom:4.0.6
# from aliyun (https://maven.aliyun.com/repository/public)
```

### 4.2 根因分析

该问题由 3 层嵌套原因导致：

| 层次 | 原因 | 说明 |
|------|------|------|
| 第 1 层 | 仓库不可达 | POM 硬编码阿里云仓库，沙箱/部分网络环境可能超时或返回 404 |
| 第 2 层 | Maven 不读环境变量代理 | 环境变量 `HTTP_PROXY` 已设置，但 Maven 不读取该变量 |
| 第 3 层 | Mirror 映射缺失 | 即使能连 Maven Central，POM 的仓库声明 `<id>public</id>` 指向阿里云，需用 `<mirror>` 覆盖 |

### 4.3 解决方案

#### 步骤 1：配置 ~/.m2/settings.xml

创建 `~/.m2/settings.xml`，**同时配置代理和镜像（两者缺一不可）**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <!-- 代理配置：Maven 不读环境变量 HTTP_PROXY，必须显式配置 -->
  <proxies>
    <proxy>
      <id>default-proxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>127.0.0.1</host>
      <port>18080</port>
      <nonProxyHosts>localhost|127.0.0.1|*.svc|*.cluster.local|::1</nonProxyHosts>
    </proxy>
  </proxies>

  <!-- 镜像配置：将 POM 声明的 aliyun 仓库映射到 Maven Central -->
  <mirrors>
    <mirror>
      <id>central-mirror</id>
      <name>Maven Central Mirror</name>
      <url>https://repo1.maven.org/maven2</url>
      <mirrorOf>public</mirrorOf>
    </mirror>
  </mirrors>

</settings>
```

> **注意**：如果网络环境需要 HTTPS 代理，`<proxies>` 中还需加一个 `<protocol>https</protocol>` 的 proxy 条目。兜底所有仓库也可用 `<mirrorOf>*</mirrorOf>`，但 `mirrorOf` 范围越大 Maven 解析越慢，推荐精确匹配 `public`。

#### 步骤 2：清理失败缓存并重试

```bash
# 删除上次失败的下载缓存
find ~/.m2/repository -name "*.lastUpdated" -delete

# 强制更新快照并编译
mvn compile -U
```

### 4.4 验证配置生效

```bash
# 查看 Maven 实际使用的代理和仓库（Debug 模式下提取相关行）
mvn compile -X 2>&1 | grep -E "(proxy|mirror|repository)"

# 正常输出应包含：
# - Proxy: 127.0.0.1:18080 (http)
# - Mirror: central-mirror (public -> https://repo1.maven.org/maven2)
```

### 4.5 常见错误与应对

| 错误信息 | 原因 | 解决 |
|----------|------|------|
| `PKIX path building failed` | SSL 证书验证失败（自签名或代理中间人） | 添加 `-Dmaven.wagon.http.ssl.insecure=true` |
| `Connect timed out` | 代理未正确配置或仓库地址不可达 | 检查 settings.xml 的 `<proxies>` 和 `<mirrors>` |
| `Received fatal alert: protocol_version` | TLS 版本不兼容（JDK 25 默认 TLS 1.3） | 添加 `-Dhttps.protocols=TLSv1.2,TLSv1.3` |
| `Checksum validation failed` | 下载的 JAR 包损坏 | 删除 `~/.m2/repository` 中对应的目录后重试 |

### 4.6 通用排查方法论

当遇到 Maven 网络相关问题时，可按以下流程排查：

| 步骤 | 命令/方法 | 判断标准 |
|------|-----------|----------|
| ① 确认域名是否可达 | `curl -v https://repo1.maven.org` | HTTP 200 = 可达 |
| ② 确认是否需要代理 | 观察 `curl -v` 输出中是否有 `via` / CF-Ray 等代理标识 | 有代理标头 → 环境走代理 |
| ③ 确认工具是否使用代理 | Maven 不读 `HTTP_PROXY` 环境变量 | 需显式配置 `settings.xml` |
| ④ 确认仓库 URL 是否被覆盖 | 查找 POM 中硬编码的 `<repository>` / `<pluginRepository>` | 需用 `<mirror>` 覆盖 |
| ⑤ 验证实际生效情况 | `mvn compile -X \| grep -i proxy` | 确认使用 `127.0.0.1:18080` |

### 4.7 关键结论

Maven 网络问题的正确解决路径是：**`settings.xml` 中同时配置 `<proxies>`（解决代理问题）和 `<mirrors>`（解决仓库地址覆盖问题），两者缺一不可。**

---

## 5. 数据库与缓存配置

### 5.1 MySQL 数据库

项目使用 MySQL，默认连接配置在 `application-druid.yml`（由 `spring.profiles.active: druid` 激活）：

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/ry-vue?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
      username: root
      password: root
```

**初始化步骤：**

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ry-vue CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 导入表结构（脚本在项目根目录）
mysql -u root -p ry-vue < ruoyi-backend/sql/ry_20240629.sql

# 3. （可选）导入工单模块业务表
mysql -u root -p ry-vue < ruoyi-backend/sql/workorder.sql
```

### 5.2 Redis 缓存

项目使用 Redis 存储会话、Token、防重复提交锁等。

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password:      # 无密码则留空
      timeout: 10s
      lettuce:
        pool:
          min-idle: 0
          max-idle: 8
          max-active: 8
```

**启动 Redis：**

```bash
# Docker 方案
docker run -d --name redis -p 6379:6379 redis:7-alpine

# 或直接启动（已安装 redis-server）
redis-server --daemonize yes
```

### 5.3 关键配置项速查

| 配置项 | 路径 | 默认值 | 说明 |
|--------|------|--------|------|
| 数据库连接 | `application-druid.yml` | `localhost:3306/ry-vue` | 按需修改地址和密码 |
| Redis 地址 | `application.yml` | `localhost:6379` | 按需修改 host/port |
| Redis Database | `application.yml` | 0 | 避免与其它项目冲突 |
| Token 有效期 | `application.yml` | 30 min | accessToken 有效期 |
| RefreshToken 有效期 | `application.yml` | 7 天 | 双 Token 续期使用 |
| 文件存储策略 | `application.yml` | local | 可选 local / minio |
| 异步线程池核心数 | `application.yml` | 10 | 根据服务规格调整 |
| 日志级别 | `application.yml` | debug | 生产环境建议 info |

---

## 6. 应用启动与构建

### 6.1 完整构建命令

```bash
# 进入后端项目目录
cd ruoyi-backend

# 清理并全量编译
mvn clean compile

# 跳过测试打包
mvn clean package -DskipTests

# 仅编译某个模块
mvn compile -pl ruoyi-framework -am

# 运行单元测试
mvn test

# 运行特定测试类
mvn test -Dtest=WorkOrderServiceImplTest

# 编译过程中发现问题时，从失败的模块继续
mvn compile -rf :ruoyi-framework
```

### 6.2 启动应用

```bash
# 方法一：IDE 启动
# 在 IDE 中运行 RuoYiApplication.java（main 方法）

# 方法二：Maven Spring Boot 插件
cd ruoyi-backend
mvn spring-boot:run -pl ruoyi-admin

# 方法三：直接运行打包后的 JAR
cd ruoyi-backend/ruoyi-admin/target
java -jar ruoyi-admin.jar
```

启动成功后：
- 后端 API：`http://localhost:8080`
- 前端页面：`http://localhost:80`（需启动前端 `cd ruoyi && npm run dev`）

### 6.3 异步线程池配置

线程池参数已外部化，可按需调整：

```yaml
ruoyi:
  async:
    core-pool-size: 10       # 核心线程数
    max-pool-size: 50        # 最大线程数
    queue-capacity: 100      # 工作队列容量
    keep-alive-seconds: 300  # 空闲线程存活时间
```

> 拒绝策略使用 `CallerRunsPolicy`：当线程池满时，由调用者线程执行任务，达到背压（back-pressure）效果，防止任务丢失。

---

## 7. 完整配置 checklist

| # | 项目 | 说明 | 完成 |
|---|------|------|------|
| 1 | JDK 已安装（17+） | `java -version` 确认 | ☐ |
| 2 | MySQL 已安装并运行 | 数据库 `ry-vue` 已创建并导入 SQL | ☐ |
| 3 | Redis 已安装并运行 | `redis-cli ping` 返回 PONG | ☐ |
| 4 | ~/.m2/settings.xml 已配置 | 含 `<proxies>` 和 `<mirrors>` | ☐ |
| 5 | Maven 全量编译通过 | `mvn clean compile` 无错误 | ☐ |
| 6 | Docker 已安装（可选） | 使用 MinIO 时需要 | ☐ |
| 7 | MinIO 容器已启动 | `docker ps` 确认 | ☐ |
| 8 | MinIO 控制台可访问 | `http://localhost:9001` | ☐ |
| 9 | `ruoyi-files` 存储桶已创建 | MinIO 控制台创建或应用自动创建 | ☐ |
| 10 | `application.yml` 配置已按需调整 | 数据源、Redis、文件策略等 | ☐ |
| 11 | 后端启动成功 | 访问 `http://localhost:8080` 无报错 | ☐ |
| 12 | 前端启动成功（可选） | `cd ruoyi && npm run dev` |
| 13 | MySQL 已安装（无 Docker 环境） | `apt install mysql-server` + `mysqld --user=mysql --datadir=/var/lib/mysql` | ☐ |
| 14 | Redis 已安装（无 Docker 环境） | `apt install redis-server` + `redis-server --daemonize yes` | ☐ |

---

## 8. 沙箱环境补充说明

### 8.1 适用场景

当开发环境为**远程沙箱（无 Docker、无 systemd）** 时，无法使用 Docker 部署 MySQL 和 Redis，需要直接通过包管理器安装并手动启动服务。

### 8.2 MySQL 安装与启动

```bash
# 安装 MySQL 8.0
sudo apt update
sudo apt install -y mysql-server

# 手动初始化数据目录（首次启动需要）
# 注意：沙箱无 systemd，不能使用 systemctl start mysql
sudo mysqld --initialize-insecure --user=mysql

# 启动 MySQL（前台启动，占用终端）
# 生产建议使用 nohup 或 tmux 管理
sudo mysqld --user=mysql --datadir=/var/lib/mysql &

# 设置 root 密码（初始无密码）
sudo mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'password';"

# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ry-vue CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入 SQL 脚本
mysql -u root -p ry-vue < ruoyi-backend/sql/ry_20260417.sql
mysql -u root -p ry-vue < ruoyi-backend/sql/device-workorder.sql
```

### 8.3 Redis 安装与启动

```bash
# 安装 Redis 7.0
sudo apt install -y redis-server

# 启动 Redis（守护进程模式）
# 注意：沙箱无 systemd，不能使用 systemctl start redis-server
redis-server --daemonize yes

# 验证连接
redis-cli ping
# 应返回 PONG

# 连接 Redis
redis-cli
```

### 8.4 验证服务状态

```bash
# 检查 MySQL 进程
ps aux | grep mysqld

# 检查 Redis 进程
ps aux | grep redis-server

# 检查 MySQL 端口
ss -tlnp | grep 3306

# 检查 Redis 端口
ss -tlnp | grep 6379
```

### 8.5 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| `mysqld: Can't create directory` | 数据目录权限不足 | `sudo chown -R mysql:mysql /var/lib/mysql` |
| `auth_socket` 插件导致无法登录 | 使用 `--initialize-insecure` 初始化，auth_socket 插件未正确配置 | 使用 `--skip-grant-tables` 启动后手动修改密码，或重新 `--initialize-insecure` |
| `redis-server: command not found` | Redis 未安装或 PATH 未更新 | `apt install -y redis-server` 后，使用 `/usr/bin/redis-server` |
| `DruidDriver MBean` 警告 | JDK 17+ Cgroup 兼容性问题 | 无害警告，不影响应用正常运行 |

### 8.6 测试验证

```bash
# 执行全部单元测试（109 个，沙箱已验证通过）
cd ruoyi-backend
mvn test

# 验证 Spring Boot 应用启动
mvn spring-boot:run -pl ruoyi-admin

# 仅运行特定模块测试
mvn test -pl ruoyi-workorder
``` ☐ |