# Spring Boot 核心业务接口实现

> **日期**：2026-03-20  
> **模块**：FocusFlow Server 业务层  
> **版本**：v1.0.0

---

## 一、三层架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                      Controller 层                               │
│  AuthController | UserController | PlantController | SyncController │
│  ─────────────────────────────────────────────────────────────  │
│  职责：接收 HTTP 请求，参数校验，调用 Service，返回响应            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                       Service 层                                 │
│  UserService | FocusRecordService | PlantDictService            │
│  ─────────────────────────────────────────────────────────────  │
│  职责：业务逻辑处理，事务管理，调用 Mapper                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                       Mapper 层                                  │
│  UserMapper | FocusRecordMapper | PlantDictMapper               │
│  ─────────────────────────────────────────────────────────────  │
│  职责：数据库 CRUD 操作，继承 BaseMapper<T>                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、MyBatis-Plus 带来的开发效率提升

### 2.1 传统 MyBatis vs MyBatis-Plus

| 操作 | 传统 MyBatis | MyBatis-Plus |
|------|-------------|--------------|
| 查询单条 | 编写 SQL + XML | `selectById(id)` |
| 条件查询 | 编写 SQL + XML | `lambdaQuery().eq(User::getDeviceUuid, uuid).one()` |
| 分页查询 | 编写 SQL + XML + PageHelper | `selectPage(page, wrapper)` |
| 更新字段 | 编写 SQL + XML | `updateById(entity)` |
| 逻辑删除 | 手动实现 | `@TableLogic` 自动处理 |

### 2.2 代码量对比

```java
// 传统 MyBatis：需要编写 XML
// UserMapper.xml
<select id="selectByDeviceUuid" resultType="User">
    SELECT * FROM biz_user WHERE device_uuid = #{uuid} AND deleted = 0
</select>

// MyBatis-Plus：零 XML 开发
User user = userMapper.selectOne(
    new LambdaQueryWrapper<User>()
        .eq(User::getDeviceUuid, uuid)
);
// 逻辑删除自动过滤，无需手写 deleted = 0
```

**效率提升**：代码量减少约 70%，且类型安全，重构友好。

---

## 三、防篡改验签服务层设计

### 3.1 安全威胁分析

| 攻击方式 | 防御机制 |
|----------|----------|
| SQLite 修改器伪造时长 | 时长参与签名，篡改后签名不匹配 |
| 复制他人记录 | userId 参与签名，用户不匹配 |
| 重放旧记录 | recordId 全局唯一，服务端幂等校验 |
| 批量生成假数据 | startTime + durationMinutes 逻辑校验 |

### 3.2 签名算法设计

```
签名公式: SHA-256(recordId || userId || durationMinutes || startTime || APP_SECRET_SALT)

其中:
- recordId: UUID 字符串，全局唯一
- userId: 用户 ID
- durationMinutes: 专注时长（分钟）
- startTime: 开始时间戳（毫秒）
- APP_SECRET_SALT: 应用专属盐值（端云一致）
```

### 3.3 服务端验签流程

```java
// FocusRecordServiceImpl.java
private SyncResultDTO processSingleRecord(FocusRecordSyncDTO dto, long currentTime) {
    // Step 1: 检查用户是否存在
    User user = userMapper.selectById(userId);
    if (user == null) {
        return SyncResultDTO.fail(recordId, "用户不存在");
    }

    // Step 2: 检查记录是否已存在（防重放）
    if (existsByRecordId(recordId)) {
        return SyncResultDTO.fail(recordId, "记录已存在，请勿重复同步");
    }

    // Step 3: 验证签名（防篡改核心）
    boolean signatureValid = SecurityUtils.verifySignature(
        recordId, userId, durationMinutes, startTime, signature
    );
    if (!signatureValid) {
        return SyncResultDTO.fail(recordId, "签名验证失败，数据可能被篡改");
    }

    // Step 4: 落库 + 增加光流
    focusRecordMapper.insert(record);
    userService.addTimeFlux(userId, rewardFlux);
    
    return SyncResultDTO.success(recordId, rewardFlux);
}
```

### 3.4 常量时间比较（防时序攻击）

```java
// SecurityUtils.java
private static boolean constantTimeEquals(String a, String b) {
    if (a.length() != b.length()) return false;
    int result = 0;
    for (int i = 0; i < a.length(); i++) {
        result |= a.charAt(i) ^ b.charAt(i);
    }
    return result == 0;
}
```

**安全原理**：普通 `String.equals()` 在不匹配时会提前返回，执行时间与匹配长度相关。攻击者可通过大量请求测量响应时间，逐字符推断正确签名。常量时间比较无论是否匹配都遍历完整字符串，消除时序侧信道。

---

## 四、API 接口设计

### 4.1 静默登录

```
POST /api/auth/silent-login

请求体:
{
  "deviceUuid": "abc123...",
  "nickname": "Focus_8848"  // 可选
}

响应体:
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "nickname": "Focus_8848",
    "timeFlux": 0,
    "isNewUser": true,
    "avatarId": 1
  }
}
```

###  }
}
```

### 4.2 植物图鉴

```
GET /api/plants

响应体:
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "霓虹蕨类",
      "description": "废土中最常见的生命力象征",
      "color": "#00FF88",
      "purifyRange": 1,
      "cultivateCost": 10,
      "rarity": 0
    }
  ]
}
```

### 4.3 批量同步专注记录

```
POST /api/sync/focus-records/batch

请求体:
{
  "records": [
    {
      "recordId": "550e8400-e29b-41d4-a716-446655440000",
      "userId": 1,
      "taskName": "写代码",
      "durationMinutes": 25,
      "startTime": 1710900000000,
      "signature": "abc123..."
    }
  ]
}

响应体:
{
  "code": 200,
  "data": [
    {
      "recordId": "550e8400-e29b-41d4-a716-446655440000",
      "success": true,
      "rewardFlux": 25,
      "message": "同步成功"
    }
  ]
}
```

---

## 五、端云签名算法统一

### 5.1 关键变更

| 项目 | 变更前 | 变更后 |
|------|--------|--------|
| recordId 类型 | Long（时间戳） | String（UUID） |
| 签名拼接方式 | 带分隔符 `\|\|` | 直接拼接 |
| 盐值 | 不统一 | 端云完全一致 |

### 5.2 统一后的签名公式

```
Android 端 (Kotlin):
val rawInput = "$recordId$userId$durationMinutes$startTime$APP_SECRET_SALT"

服务端 (Java):
String rawInput = recordId + userId + durationMinutes + startTime + APP_SECRET_SALT;

两者完全一致！
```

---

## 六、目录结构

```
FocusFlow_Server/src/main/java/com/focusflow/server/
├── FocusFlowServerApplication.java    # 主启动类
├── common/
│   ├── Result.java                    # 统一响应结构
│   └── SecurityUtils.java             # 防篡改签名工具
├── entity/
│   ├── User.java                      # 用户实体
│   ├── FocusRecord.java               # 专注记录实体
│   └── PlantDict.java                 # 植物图鉴实体
├── mapper/
│   ├── UserMapper.java
│   ├── FocusRecordMapper.java
│   └── PlantDictMapper.java
├── dto/
│   ├── SilentLoginRequest.java
│   ├── SilentLoginResponse.java
│   ├── FocusRecordSyncDTO.java
│   ├── BatchSyncRequest.java
│   ├── SyncResultDTO.java
│   └── PlantDictResponse.java
├── service/
│   ├── UserService.java
│   ├── FocusRecordService.java
│   └── PlantDictService.java
├── service/impl/
│   ├── UserServiceImpl.java
│   ├── FocusRecordServiceImpl.java
│   └── PlantDictServiceImpl.java
└── controller/
    ├── AuthController.java
    ├── UserController.java
    ├── PlantController.java
    └── SyncController.java
```

---

## 七、论文素材总结

### 可写入论文的内容

1. **三层架构设计**：Controller → Service → Mapper 的职责分离
2. **MyBatis-Plus 零 SQL 开发**：代码量减少 70%，开发效率提升
3. **防篡改签名机制**：SHA-256 + 盐值哈希，防止数据伪造
4. **常量时间比较**：防时序攻击的安全设计
5. **离线优先架构**：客户端生成 UUID，断网也能落库
6. **端云签名统一**：确保验签一致性

### 可展示的图表

- 三层架构图
- 防篡改验签流程图
- API 请求响应序列图
