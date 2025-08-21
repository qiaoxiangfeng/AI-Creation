#!/bin/bash

# 测试用户密码初始化功能
# 使用方法: ./test-password-init.sh [base_url]

BASE_URL=${1:-"http://localhost:8080"}
API_BASE="${BASE_URL}/api/users"

echo "=== 测试用户密码初始化功能 ==="
echo "API基础URL: ${API_BASE}"
echo ""

# 测试1: 初始化所有用户密码为默认值123456
echo "测试1: 初始化所有用户密码为默认值123456"
curl -X POST "${API_BASE}/init-password" \
  -H "Content-Type: application/json" \
  -d '{}' \
  -w "\nHTTP状态码: %{http_code}\n" \
  -s
echo ""

# 测试2: 初始化所有用户密码为自定义密码
echo "测试2: 初始化所有用户密码为自定义密码"
curl -X POST "${API_BASE}/init-password" \
  -H "Content-Type: application/json" \
  -d '{"newPassword": "888888"}' \
  -w "\nHTTP状态码: %{http_code}\n" \
  -s
echo ""

# 测试3: 初始化指定用户密码
echo "测试3: 初始化指定用户密码"
curl -X POST "${API_BASE}/init-password" \
  -H "Content-Type: application/json" \
  -d '{"userIds": [1, 2], "newPassword": "666666"}' \
  -w "\nHTTP状态码: %{http_code}\n" \
  -s
echo ""

# 测试4: 验证密码是否更新成功（尝试登录）
echo "测试4: 验证密码是否更新成功（尝试登录）"
echo "注意: 这里需要先创建用户，然后测试登录功能"
echo ""

echo "=== 测试完成 ==="
echo ""
echo "使用说明:"
echo "1. 不传参数: 初始化所有用户密码为123456"
echo "2. 只传newPassword: 初始化所有用户密码为指定值"
echo "3. 传userIds和newPassword: 初始化指定用户密码"
echo "4. 传userIds但不传newPassword: 初始化指定用户密码为123456"
