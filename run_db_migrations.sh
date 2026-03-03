#!/bin/bash

# 数据库迁移脚本执行器
# 使用方法: ./run_db_migrations.sh [database_url] [username] [database_name]
# 例如: ./run_db_migrations.sh localhost postgres ai_creation

set -e

# 默认配置
DB_HOST=${1:-"localhost"}
DB_USER=${2:-"postgres"}
DB_NAME=${3:-"ai_creation"}
DB_PORT=${4:-"5432"}

echo "🗄️  数据库迁移脚本执行器"
echo "======================================"
echo "数据库主机: $DB_HOST"
echo "数据库端口: $DB_PORT"
echo "数据库用户: $DB_USER"
echo "数据库名称: $DB_NAME"
echo ""

# 检查psql是否可用
if ! command -v psql &> /dev/null; then
    echo "❌ 错误: 未找到psql命令。请确保已安装PostgreSQL客户端。"
    exit 1
fi

# 检查数据库连接
echo "🔍 检查数据库连接..."
if ! PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" &> /dev/null; then
    echo "❌ 错误: 无法连接到数据库。请检查连接参数和权限。"
    echo "💡 提示: 如果需要密码，请设置环境变量: export DB_PASSWORD=your_password"
    exit 1
fi
echo "✅ 数据库连接成功"
echo ""

# 定义脚本执行顺序
SCRIPTS=(
    "src/main/resources/sql/migrate_content_generated_to_generation_status.sql"
    "src/main/resources/sql/add_article_generation_status.sql"
    "src/main/resources/sql/add_article_word_estimate_fields.sql"
    "src/main/resources/sql/add_article_chapter_fields.sql"
    "src/main/resources/sql/add_article_generation_config_word_estimate_fields.sql"
    "src/main/resources/sql/remove_article_content_generated.sql"
)

# 执行脚本
echo "🚀 开始执行数据库迁移脚本..."
echo ""

for script in "${SCRIPTS[@]}"; do
    if [ -f "$script" ]; then
        echo "📄 执行脚本: $script"
        if PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$script"; then
            echo "✅ $script 执行成功"
        else
            echo "❌ $script 执行失败"
            exit 1
        fi
        echo ""
    else
        echo "⚠️  警告: 脚本文件不存在 - $script"
    fi
done

echo "🎉 所有数据库迁移脚本执行完成！"
echo ""
echo "📊 执行结果验证..."
PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT
    'article' as table_name,
    COUNT(*) as total_records,
    COUNT(CASE WHEN generation_status IS NOT NULL THEN 1 END) as has_generation_status,
    COUNT(CASE WHEN total_word_count_estimate IS NOT NULL THEN 1 END) as has_total_estimate,
    COUNT(CASE WHEN chapter_word_count_estimate IS NOT NULL THEN 1 END) as has_chapter_estimate
FROM article
UNION ALL
SELECT
    'article_chapter' as table_name,
    COUNT(*) as total_records,
    COUNT(CASE WHEN core_plot IS NOT NULL THEN 1 END) as has_core_plot,
    COUNT(CASE WHEN word_count_estimate IS NOT NULL THEN 1 END) as has_word_estimate,
    0 as placeholder
FROM article_chapter
UNION ALL
SELECT
    'article_generation_config' as table_name,
    COUNT(*) as total_records,
    COUNT(CASE WHEN total_word_count_estimate IS NOT NULL THEN 1 END) as has_total_estimate,
    COUNT(CASE WHEN chapter_word_count_estimate IS NOT NULL THEN 1 END) as has_chapter_estimate,
    0 as placeholder
FROM article_generation_config;" 2>/dev/null

echo ""
echo "💡 提示: 如果看到执行失败，请检查数据库权限和脚本语法。"
echo "📖 详情请参考: DATABASE_MIGRATION_SCRIPTS_README.md"