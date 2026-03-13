#!/bin/bash

# AI Creation 前后端生产环境构建脚本
echo "🏗️ 开始构建 AI Creation 前后端生产环境包..."

# 检查环境
if ! command -v java &> /dev/null; then
    echo "❌ 未找到Java，请先安装JDK 21+"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ 未找到Maven，请先安装Maven"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "❌ 未找到Node.js，请先安装Node.js"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "❌ 未找到npm，请先安装npm"
    exit 1
fi

echo "✅ 环境检查通过"

# 创建构建目录
echo "📁 创建构建目录..."
BUILD_DIR="prod"

# 每次构建只固定清理产物（frontend、frontend.zip 和 jar），其余文件保留
if [ -d "$BUILD_DIR" ]; then
    # 固定删除前端构建产物目录和后端 jar
    rm -rf "$BUILD_DIR/frontend" 2>/dev/null || true
    rm -f "$BUILD_DIR/frontend.zip" 2>/dev/null || true
    rm -f "$BUILD_DIR/ai-creation-1.0.0.jar" 2>/dev/null || true
else
    mkdir -p "$BUILD_DIR"
fi

# 日志目录由程序启动时自动创建

# 构建前端
echo "🎨 构建前端应用..."
cd frontend

echo "📦 安装前端依赖..."
npm ci --production=false

if [ $? -ne 0 ]; then
    echo "⚠️  npm ci失败，尝试使用npm install..."
    npm install --production=false

    if [ $? -ne 0 ]; then
        echo "❌ 前端依赖安装失败"
        echo "💡 建议：升级Node.js到18+版本，或手动构建前端"
        echo "   手动构建命令：cd frontend && npm install && npm run build:prod"
        exit 1
    fi
fi

# 构建生产版本
echo "🏗️ 构建前端生产版本..."
npm run build:prod

if [ $? -ne 0 ]; then
    echo "❌ 前端构建失败"
    exit 1
fi

echo "✅ 前端构建成功"

# 复制前端构建结果
cp -r dist "../$BUILD_DIR/frontend"
cd ..

# 构建后端
echo "⚙️ 构建后端应用..."
mvn clean package -DskipTests -Pprod

if [ $? -ne 0 ]; then
    echo "❌ 后端构建失败"
    exit 1
fi

echo "✅ 后端构建成功"

# 复制构建结果
cp target/ai-creation-1.0.0.jar "$BUILD_DIR/"

# 复制脚本文件（如果prod目录中不存在）
echo "📋 复制脚本文件..."
for script in start.sh stop.sh restart.sh pre-deploy.sh; do
    if [ ! -f "$BUILD_DIR/$script" ]; then
        cp "$script" "$BUILD_DIR/" 2>/dev/null || echo "⚠️  $script 不存在，跳过复制"
    else
        echo "✅ $script 已存在，跳过复制"
    fi
done

# 复制文档
cp README-PROD.md "$BUILD_DIR/"

# 创建部署说明
# 移除部署说明文件生成

# 显示结果
# 打包前端目录为 zip
echo "🗜️ 压缩前端产物为 ZIP..."
cd "$BUILD_DIR"
zip -r frontend.zip frontend > /dev/null
cd ..

echo "🎉 构建完成！"
echo ""
echo "📦 构建结果:"
echo "  - 构建目录: $BUILD_DIR/"
echo "  - 后端包: $BUILD_DIR/ai-creation-1.0.0.jar"
echo "  - 前端目录: $BUILD_DIR/frontend/ (部署到 /opt/AI-Creation/frontend/)"
echo "  - 前端 ZIP 包: $BUILD_DIR/frontend.zip"
echo "  - 管理脚本: $BUILD_DIR/*.sh"
echo "  - 日志目录: $BUILD_DIR/logs/ (部署后在 /opt/AI-Creation/logs/)"
echo ""
echo "🚀 部署位置说明:"
echo "  - 应用目录: /opt/AI-Creation/"
echo "  - 前端文件: /opt/AI-Creation/frontend/"
echo "  - 日志文件: /opt/AI-Creation/logs/"
echo ""
echo "📁 本地prod目录已更新"