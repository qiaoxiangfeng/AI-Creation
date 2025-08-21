#!/bin/bash

# 测试添加image_desc字段的脚本
# 使用方法：./test-add-image-desc-field.sh

echo "=== 测试添加image_desc字段 ==="

# 检查数据库连接
echo "1. 检查数据库连接..."
# 这里需要根据实际的数据库配置来执行SQL
# 示例：psql -h localhost -U username -d database_name -c "SELECT version();"

echo "2. 执行添加字段SQL..."
echo "ALTER TABLE article ADD COLUMN image_desc TEXT;"
echo "COMMENT ON COLUMN article.image_desc IS '形象描述';"

echo "3. 验证字段添加..."
echo "SELECT column_name, data_type, is_nullable, column_default, character_maximum_length"
echo "FROM information_schema.columns"
echo "WHERE table_name = 'article' AND column_name = 'image_desc';"

echo "4. 测试插入数据..."
echo "INSERT INTO article (article_name, article_outline, image_desc, article_content, voice_tone, publish_status, res_state)"
echo "VALUES ('测试文章', '测试简介', '测试形象描述', '测试内容', 'alex', 1, 1);"

echo "5. 测试查询数据..."
echo "SELECT id, article_name, article_outline, image_desc, article_content FROM article WHERE article_name = '测试文章';"

echo "=== 测试完成 ==="
echo "注意：请根据实际的数据库配置来执行上述SQL语句"
