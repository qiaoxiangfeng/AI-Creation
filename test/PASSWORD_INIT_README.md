# 用户密码初始化功能说明

## 功能概述

本功能提供批量初始化用户密码的能力，支持以下场景：
1. 初始化所有用户密码为默认值"123456"
2. 初始化所有用户密码为自定义值
3. 初始化指定用户密码为默认值"123456"
4. 初始化指定用户密码为自定义值

## API接口

### 接口地址
```
POST /users/init-password
```

### 请求参数
```json
{
  "userIds": [1, 2, 3],  // 可选，用户ID列表，为空时初始化所有用户
  "newPassword": "888888"  // 可选，新密码，为空时使用默认密码"123456"
}
```

### 响应格式
```json
{
  "code": "200",
  "message": "success",
  "data": 5  // 成功初始化的用户数量
}
```

## 使用示例

### 1. 初始化所有用户密码为默认值"123456"
```bash
curl -X POST "http://localhost:8080/users/init-password" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 2. 初始化所有用户密码为自定义值
```bash
curl -X POST "http://localhost:8080/users/init-password" \
  -H "Content-Type: application/json" \
  -d '{"newPassword": "888888"}'
```

### 3. 初始化指定用户密码为默认值"123456"
```bash
curl -X POST "http://localhost:8080/users/init-password" \
  -H "Content-Type: application/json" \
  -d '{"userIds": [1, 2, 3]}'
```

### 4. 初始化指定用户密码为自定义值
```bash
curl -X POST "http://localhost:8080/users/init-password" \
  -H "Content-Type: application/json" \
  -d '{"userIds": [1, 2, 3], "newPassword": "666666"}'
```

## 测试脚本

项目提供了测试脚本 `test-password-init.sh` 来验证功能：

```bash
# 使用默认URL (localhost:8080)
./test/test-password-init.sh

# 使用自定义URL
./test/test-password-init.sh http://your-server:8080
```

## 安全特性

1. **密码加密**: 所有密码都使用BCrypt算法进行加密存储
2. **参数验证**: 支持密码长度验证（6-20位）
3. **批量限制**: 单次最多支持1000个用户ID
4. **事务支持**: 使用数据库事务确保数据一致性

## 注意事项

1. 密码初始化操作不可逆，请谨慎使用
2. 建议在生产环境中添加管理员权限验证
3. 初始化完成后，用户需要使用新密码重新登录
4. 密码更新会同时更新用户的update_time字段

## 技术实现

- **Controller层**: `UserController.initializeUserPasswords()`
- **Service层**: `UserService.initializeUserPasswords()`
- **Mapper层**: `UserMapper.updateUserPasswords()`
- **DTO类**: `UserPasswordInitRequest`
- **数据库**: 支持PostgreSQL等关系型数据库
