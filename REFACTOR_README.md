# PageReqDto 重构说明

## 重构背景

在 `UserServiceImpl.getUserList()` 方法中，存在重复的分页参数验证和默认值设置代码：

```java
// 重构前的冗余代码
Integer pageNum = request.getPageNum();
Integer pageSize = request.getPageSize();
if (Objects.isNull(pageNum) || pageNum < 1) {
    pageNum = 1;
}
if (Objects.isNull(pageSize) || pageSize < 1 || pageSize > 100) {
    pageSize = 10;
}
```

## 重构方案

将分页参数的验证逻辑统一封装到 `PageReqDto` 基类中，提供以下方法：

### 1. `getValidatedPageNo()`
- 验证页码参数
- 如果为空或小于1，返回默认值1
- 避免在每个服务中重复编写验证逻辑

### 2. `getValidatedPageSize()`
- 验证每页记录数参数
- 如果为空、小于1或大于100，返回默认值20
- 统一分页大小限制规则

### 3. `isValid()`
- 验证分页参数是否有效
- 可用于请求参数的前置验证

## 重构后的使用方式

```java
@Override
public PageRespDto<UserRespDto> getUserList(UserListReqDto request) {
    // 使用PageReqDto的验证方法获取分页参数
    Integer pageNum = request.getValidatedPageNo();
    Integer pageSize = request.getValidatedPageSize();
    String userName = request.getUserName();
    
    // ... 其他业务逻辑
}
```

## 重构好处

### 1. **代码复用**
- 避免在每个分页查询服务中重复编写验证逻辑
- 统一的参数验证规则，便于维护

### 2. **职责分离**
- `PageReqDto` 负责参数验证和默认值处理
- 服务层专注于业务逻辑，不关心参数验证细节

### 3. **易于扩展**
- 新增分页参数验证规则时，只需修改基类
- 所有继承的子类自动获得新功能

### 4. **提高可读性**
- 服务方法更加简洁，意图更清晰
- 参数验证逻辑集中管理，便于理解

### 5. **减少错误**
- 避免在不同服务中实现不一致的验证逻辑
- 统一的验证规则减少人为错误

## 使用建议

1. **新开发的分页接口**：直接使用 `getValidatedPageNo()` 和 `getValidatedPageSize()` 方法
2. **现有代码重构**：逐步将重复的分页验证逻辑替换为基类方法
3. **参数验证**：在Controller层可以使用 `isValid()` 方法进行前置验证

## 注意事项

- 默认分页大小从10调整为20，与数据库初始化脚本保持一致
- 分页大小上限设置为100，防止过大的查询影响性能
- 保持了向后兼容性，现有的getter/setter方法仍然可用
