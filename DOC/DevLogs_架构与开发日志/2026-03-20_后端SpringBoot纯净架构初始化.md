# 后端 Spring Boot 纯净架构初始化

> **日期**：2026-03-20  
> **模块**：FocusFlow Server (后端服务)  
> **版本**：v1.0.0

---

## 一、架构决策：放弃 Python FastAPI，转向 Spring Boot

### 1.1 技术栈对比分析

| 维度 | Python FastAPI | Java Spring Boot |
|------|----------------|------------------|
| **类型系统** | 动态类型，运行时错误 | 强类型，编译期检查 |
| **生态成熟度** | 新兴框架，生态较小 | 企业级标准，生态完善 |
| **IDE 支持** | VS Code / PyCharm | IntelliJ IDEA（深度集成） |
| **部署方式** | 依赖虚拟环境 | 独立 JAR 包 |
| **并发模型** | asyncio 协程 | 虚拟线程（JDK 21+）/ 线程池 |
| **ORM 方案** | SQLAlchemy | MyBatis-Plus |

### 1.2 放弃 Python 的核心考量

#### ① 前后端物理隔离

```
传统 Python 全栈架构（已废弃）：
┌─────────────────────────────────────┐
│  Android App (Kotlin)               │
│         ↓ HTTP                      │
│  FastAPI (Python) ─→ MySQL          │
│         ↓                           │
│  智谱 AI SDK (Python)               │
└─────────────────────────────────────┘
问题：Python 脚本与 Android 项目混合存放，职责边界模糊

新架构（Spring Boot）：
┌──────────────────────┐    ┌──────────────────────┐
│  FocusFlow_App       │    │  FocusFlow_Server    │
│  (Android/Kotlin)    │    │  (Spring Boot/Java)  │
│  ────────────────    │    │  ────────────────    │
│  Jetpack Compose     │    │  MyBatis-Plus        │
│  Room Database       │ ←─→│  MySQL               │
│  Retrofit            │    │  Controller/Service  │
└──────────────────────┘    └──────────────────────┘
        两个独立工程，物理隔离，高内聚低耦合
```

#### ② 高内聚低耦合的设计原则

| 原则 | 实现方式 |
|------|----------|
| **单一职责** | Android 专注 UI/交互，Spring Boot 专注业务逻辑 |
| **接口隔离** | RESTful API 作为唯一通信契约 |
| **依赖倒置** | 两端仅通过 DTO 交互，内部实现互不可见 |
| **开闭原则** | 后端可独立扩展新接口，不影响已有 Android 版本 |

#### ③ 毕业论文答辩价值

**论文可写内容**：
- 前后端分离架构设计
- RESTful API 规范设计
- 端云数据同步机制
- 防篡改签名验证
- 惰性状态计算策略

**答辩可展示内容**：
- 两个独立工程的清晰目录结构
- 标准化的接口文档
- 完善的单元测试覆盖

### 1.3 技术选型理由

```
Spring Boot 3.x + MyBatis-Plus 选择理由：

1. 【类型安全】
   Java 强类型系统在编译期捕获错误，
   避免 Python 运行时 AttributeError 等问题

2. 【MyBatis-Plus 零 SQL 开发】
   继承 BaseMapper<T> 即可获得 CRUD 能力，
   减少手写 SQL 的工作量

3. 【Spring 生态成熟】
   - Spring Security：权限控制
   - Spring Validation：参数校验
   - Spring AOP：日志切面

4. 【部署运维简单】
   mvn package → java -jar xxx.jar
   无需配置虚拟环境
```

---

## 二、项目结构设计

### 2.1 目录结构

```
F:\desktop\iflowtest\
├── FocusFlow_App/                    # Android 端（Kotlin）
│   ├── app/
│   │   └── src/main/java/com/example/focusflow/
│   │       ├── api/                  # 网络层
│   │       ├── data/                 # 数据层
│   │       ├── ui/                   # UI 层
│   │       └── service/              # 后台服务
│   └── build.gradle.kts
│
├── FocusFlow_Server/                 # 后端服务（Java）
│   ├── pom.xml                       # Maven 配置
│   └── src/main/
│       ├── java/com/focusflow/server/
│       │   ├── FocusFlowServerApplication.java
│       │   ├── controller/           # 控制器层
│       │   ├── service/              # 业务层
│       │   ├── mapper/               # 数据访问层
│       │   ├── entity/               # 实体类
│       │   └── dto/                  # 数据传输对象
│       └── resources/
│           └── application.yml       # 配置文件
│
└── DOC/
    ├── db_init_focusflow.sql         # 数据库初始化脚本
    └── DevLogs_架构与开发日志/
```

### 2.2 依赖配置（pom.xml 核心依赖）

```xml
<!-- Web 模块 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- MyBatis-Plus：增强版 ORM -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.5</version>
</dependency>

<!-- MySQL 驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>

<!-- Lombok：减少样板代码 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

---

## 三、数据库设计亮点

### 3.1 表结构概览

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `biz_user` | 用户主表 | `device_uuid`, `time_flux` |
| `biz_focus_record` | 专注记录 | `record_id`(UUID), `signature` |
| `biz_plant_dict` | 植物图鉴 | `drop_weight`, `purify_range` |
| `biz_user_bag` | 用户背包 | `status` (种子→成熟→种植) |
| `biz_garden_tile` | 花园地块 | `last_charge_time`, `is_purified` |
| `biz_friendship` | 好友关系 | 联合唯一索引 |
| `biz_visit_log` | 互访日志 | `action_type`, `content` |

### 3.2 设计亮点

```sql
-- ① UUID 主键（专注记录）
-- 由 Android 端生成，端云主键一致，避免 ID 冲突
`record_id` CHAR(36) NOT NULL

-- ② 防篡改签名
-- SHA-256 哈希，防止用户伪造时长
`signature` VARCHAR(64) NOT NULL

-- ③ 软删除标记
-- 所有表统一使用 deleted 字段
`deleted` TINYINT NOT NULL DEFAULT 0

-- ④ 时间戳统一使用 BIGINT
-- 毫秒级，便于跨平台同步
`created_at` BIGINT NOT NULL
```

---

## 四、配置文件设计

### 4.1 application.yml 核心配置

```yaml
server:
  port: 8080
  servlet:
    context-path: /api  # 统一 API 前缀

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/focus_flow
    username: root
    password: 123456
    hikari:
      maximum-pool-size: 20  # 连接池大小

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true  # 驼峰转换
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志
```

---

## 五、踩坑与解决方案

### 5.1 数据库连接问题

**问题**：首次启动时报 `Communications link failure`

**原因**：MySQL 服务未启动或连接参数错误

**解决方案**：
```yaml
# application.yml 添加时区和 SSL 配置
url: jdbc:mysql://127.0.0.1:3306/focus_flow?
      serverTimezone=Asia/Shanghai
      &useSSL=false
      &allowPublicKeyRetrieval=true
```

### 5.2 MyBatis-Plus 扫描问题

**问题**：`Invalid bound statement (not found)`

**原因**：Mapper 接口未被扫描

**解决方案**：
```java
// 主启动类添加注解
@MapperScan("com.focusflow.server.mapper")
public class FocusFlowServerApplication { ... }
```

### 5.3 字符集问题

**问题**：中文数据存储后乱码

**解决方案**：
```sql
-- 建表时指定字符集
CREATE TABLE xxx (
    ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 六、下一步计划

| 优先级 | 任务 | 预计工作量 |
|--------|------|-----------|
| P0 | 实现 UserController（静默登录、用户信息查询） | 中 |
| P0 | 实现 FocusRecordController（批量同步接口） | 高 |
| P0 | 实现 PlantDictController（植物图鉴查询） | 低 |
| P1 | 实现 GardenController（花园地块同步） | 中 |
| P1 | 实现好友系统 API | 高 |
| P2 | 集成 Spring Security + JWT | 高 |

---

## 七、总结

本次架构调整实现了：

1. **前后端物理隔离**：Android 端与后端分离为独立工程
2. **技术栈统一**：后端采用 Java 生态，与 Android Kotlin 形成 JVM 系语言统一
3. **数据库设计完善**：7 张核心表，支持全部业务场景
4. **开发效率提升**：MyBatis-Plus 零 SQL 开发，Lombok 减少样板代码

**论文价值点**：
- 前后端分离架构设计（可写入系统架构章节）
- RESTful API 接口规范（可写入接口设计章节）
- 数据库表设计与优化（可写入数据设计章节）
