#!/bin/bash

# AI智造项目启动脚本
# 同时启动前端和后端，并自动打开网页

set -e  # 遇到错误时退出

# 环境变量配置（可通过export设置）
# CLEAN_MAVEN_CACHE=true    # 是否清理Maven本地仓库缓存（默认false）
# CLEAN_NODE_MODULES=true   # 是否清理node_modules重新安装（默认false）
# CLEAN_LOGS=true          # 是否清理日志文件（默认true）
# CLEAN_BUILD=true          # 是否清理构建目录（默认true）

# 设置默认值
CLEAN_MAVEN_CACHE=${CLEAN_MAVEN_CACHE:-false}
CLEAN_NODE_MODULES=${CLEAN_NODE_MODULES:-false}
CLEAN_LOGS=${CLEAN_LOGS:-true}
CLEAN_BUILD=${CLEAN_BUILD:-true}

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查必要的工具
check_requirements() {
    print_info "检查系统环境..."
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        print_error "Java未安装，请先安装Java 17或更高版本"
        exit 1
    fi
    
    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven未安装，请先安装Maven"
        exit 1
    fi
    
    # 检查Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js未安装，请先安装Node.js"
        exit 1
    fi
    
    # 检查npm
    if ! command -v npm &> /dev/null; then
        print_error "npm未安装，请先安装npm"
        exit 1
    fi
    
    print_success "环境检查通过"
}

# 检查端口是否被占用
check_port() {
    local port=$1
    local service=$2
    
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_warning "端口 $port 已被占用，$service 可能已经在运行"
        return 1
    else
        return 0
    fi
}

# 启动PostgreSQL（如果使用Docker）
start_postgres() {
    print_info "检查PostgreSQL状态..."
    
    # 检查PostgreSQL是否运行
    if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
        print_success "PostgreSQL已在运行"
        return 0
    fi
    
    # 尝试使用Docker启动PostgreSQL
    if command -v docker &> /dev/null; then
        print_info "尝试使用Docker启动PostgreSQL..."
        
        # 检查是否已有PostgreSQL容器
        if docker ps -q -f name=ai-creation-postgres | grep -q .; then
            print_info "启动现有PostgreSQL容器..."
            docker start ai-creation-postgres
        else
            print_info "创建并启动PostgreSQL容器..."
            docker run -d \
                --name ai-creation-postgres \
                -e POSTGRES_DB=ai_creation \
                -e POSTGRES_USER=postgres \
                -e POSTGRES_PASSWORD=123456 \
                -p 5432:5432 \
                postgres:15
        fi
        
        # 等待PostgreSQL启动
        print_info "等待PostgreSQL启动..."
        for i in {1..30}; do
            if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
                print_success "PostgreSQL启动成功"
                return 0
            fi
            sleep 1
        done
        
        print_error "PostgreSQL启动超时"
        return 1
    else
        print_warning "Docker未安装，请手动启动PostgreSQL"
        print_info "PostgreSQL配置: localhost:5432, 数据库: ai_creation, 用户: postgres, 密码: 123456"
        return 1
    fi
}

# 启动Redis（如果使用Docker）
start_redis() {
    print_info "检查Redis状态..."
    
    # 检查Redis是否运行
    if redis-cli ping >/dev/null 2>&1; then
        print_success "Redis已在运行"
        return 0
    fi
    
    # 尝试使用Docker启动Redis
    if command -v docker &> /dev/null; then
        print_info "尝试使用Docker启动Redis..."
        
        # 检查是否已有Redis容器
        if docker ps -q -f name=ai-creation-redis | grep -q .; then
            print_info "启动现有Redis容器..."
            docker start ai-creation-redis
        else
            print_info "创建并启动Redis容器..."
            docker run -d \
                --name ai-creation-redis \
                -p 6379:6379 \
                redis:7-alpine
        fi
        
        # 等待Redis启动
        print_info "等待Redis启动..."
        for i in {1..10}; do
            if redis-cli ping >/dev/null 2>&1; then
                print_success "Redis启动成功"
                return 0
            fi
            sleep 1
        done
        
        print_error "Redis启动超时"
        return 1
    else
        print_warning "Docker未安装，请手动启动Redis"
        print_info "Redis配置: localhost:6379"
        return 1
    fi
}

# 启动后端
start_backend() {
    print_info "启动后端服务..."
    
    # 检查后端端口
    if ! check_port 8080 "后端服务"; then
        print_warning "后端服务可能已在运行，跳过启动"
        return 0
    fi
    
    # 清理旧包和缓存
    print_info "清理旧包和缓存..."
    
    # 清理Maven本地仓库缓存（可选）
    if [ "$CLEAN_MAVEN_CACHE" = "true" ]; then
        print_info "清理Maven本地仓库缓存..."
        mvn dependency:purge-local-repository -q
    fi
    
    # 清理项目构建目录
    if [ "$CLEAN_BUILD" = "true" ]; then
        print_info "清理项目构建目录..."
        mvn clean -q
        
        # 清理IDE生成的文件
        print_info "清理IDE生成的文件..."
        find . -name "*.class" -delete 2>/dev/null || true
        find . -name "*.jar" -delete 2>/dev/null || true
        find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
        find . -name "bin" -type d -exec rm -rf {} + 2>/dev/null || true
    fi
    
    # 日志轮转/归档
    if [ "$CLEAN_LOGS" = "true" ]; then
        print_info "处理历史日志（归档到 logs/archive）..."
        TS=$(date +%Y%m%d-%H%M%S)
        mkdir -p logs/archive/$TS 2>/dev/null || true
        # 归档根目录与 logs 目录下的日志
        find . -maxdepth 1 -type f -name "*.log*" -exec mv {} logs/archive/$TS/ \; 2>/dev/null || true
        find logs -maxdepth 1 -type f -name "*.log*" -exec mv {} logs/archive/$TS/ \; 2>/dev/null || true
        print_success "日志已归档到 logs/archive/$TS"
    fi
    
    # 编译项目
    print_info "编译后端项目..."
    if ! mvn compile -q; then
        print_error "后端编译失败"
        return 1
    fi
    
    # 启动后端（后台运行）
    print_info "启动Spring Boot应用..."
    mkdir -p logs
    nohup mvn spring-boot:run > logs/backend.out.log 2>&1 &
    BACKEND_PID=$!
    
    # 等待后端启动
    print_info "等待后端服务启动..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
            print_success "后端服务启动成功 (PID: $BACKEND_PID)"
            return 0
        fi
        sleep 1
    done
    
    print_error "后端服务启动超时"
    return 1
}

# 启动前端
start_frontend() {
    print_info "启动前端服务..."
    
    # 检查前端端口
    if ! check_port 5173 "前端服务"; then
        print_warning "前端服务可能已在运行，跳过启动"
        return 0
    fi
    
    # 清理前端缓存和旧文件
    print_info "清理前端缓存和旧文件..."
    
    # 进入前端目录
    cd frontend
    
    # 清理node_modules（可选，通过环境变量控制）
    if [ "$CLEAN_NODE_MODULES" = "true" ]; then
        print_info "清理node_modules，重新安装依赖..."
        rm -rf node_modules package-lock.json
        print_info "重新安装前端依赖..."
        if ! npm install --silent; then
            print_error "前端依赖安装失败"
            return 1
        fi
    fi
    
    # 清理构建缓存
    print_info "清理构建缓存..."
    rm -rf dist .vite .cache 2>/dev/null || true
    
    # 检查依赖是否已安装
    if [ ! -d "node_modules" ]; then
        print_info "安装前端依赖..."
        if ! npm install --silent; then
            print_error "前端依赖安装失败"
            return 1
        fi
    fi
    
    # 启动前端（后台运行）
    print_info "启动Vue前端应用..."
    nohup npm run dev > ../logs/frontend.out.log 2>&1 &
    FRONTEND_PID=$!
    
    # 回到根目录
    cd ..
    
    # 等待前端启动
    print_info "等待前端服务启动..."
    for i in {1..30}; do
        if curl -s http://localhost:5173 >/dev/null 2>&1; then
            print_success "前端服务启动成功 (PID: $FRONTEND_PID)"
            return 0
        fi
        sleep 1
    done
    
    print_error "前端服务启动超时"
    return 1
}

# 自动打开浏览器
open_browser() {
    print_info "自动打开浏览器..."
    
    # 等待一下确保服务完全启动
    sleep 2
    
    # 尝试打开前端页面
    if command -v open &> /dev/null; then
        # macOS
        open "http://localhost:5173/login"
        print_success "已打开浏览器访问后台管理登录页面"
    elif command -v xdg-open &> /dev/null; then
        # Linux
        xdg-open "http://localhost:5173/login"
        print_success "已打开浏览器访问后台管理登录页面"
    elif command -v start &> /dev/null; then
        # Windows
        start "http://localhost:5173/login"
        print_success "已打开浏览器访问后台管理登录页面"
    else
        print_warning "无法自动打开浏览器，请手动访问以下地址："
    fi
    
    print_info "🌐 访问地址："
    print_info "   后台管理: http://localhost:5173/login"
    print_info "   前台页面: http://localhost:5173"
    print_info "   后端API: http://localhost:8080/api"
    print_info "   API文档: http://localhost:8080/api/swagger-ui/index.html"
    print_info ""
    print_info "🔑 默认账号: admin"
    print_info "🔑 默认密码: 123456"
}

# 显示清理配置
show_clean_config() {
    print_info "清理配置："
    print_info "  Maven缓存清理: $([ "$CLEAN_MAVEN_CACHE" = "true" ] && echo "✅ 启用" || echo "❌ 禁用")"
    print_info "  Node模块清理: $([ "$CLEAN_NODE_MODULES" = "true" ] && echo "✅ 启用" || echo "❌ 禁用")"
    print_info "  日志文件清理: $([ "$CLEAN_LOGS" = "true" ] && echo "✅ 启用" || echo "❌ 禁用")"
    print_info "  构建目录清理: $([ "$CLEAN_BUILD" = "true" ] && echo "✅ 启用" || echo "❌ 禁用")"
    echo
}

# 显示状态信息
show_status() {
    print_info "服务状态："
    
    # 检查后端
    if curl -s http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
        print_success "✅ 后端服务: 运行中 (http://localhost:8080/api)"
    else
        print_error "❌ 后端服务: 未运行"
    fi
    
    # 检查前端
    if curl -s http://localhost:5173 >/dev/null 2>&1; then
        print_success "✅ 前端服务: 运行中 (http://localhost:5173)"
    else
        print_error "❌ 前端服务: 未运行"
    fi
    
    # 检查PostgreSQL
    if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
        print_success "✅ PostgreSQL: 运行中 (localhost:5432)"
    else
        print_error "❌ PostgreSQL: 未运行"
    fi
    
    # 检查Redis
    if redis-cli ping >/dev/null 2>&1; then
        print_success "✅ Redis: 运行中 (localhost:6379)"
    else
        print_error "❌ Redis: 未运行"
    fi
}

# 清理函数
cleanup() {
    print_info "清理资源..."
    
    # 停止后端
    if [ ! -z "$BACKEND_PID" ]; then
        print_info "停止后端服务 (PID: $BACKEND_PID)..."
        kill $BACKEND_PID 2>/dev/null || true
    fi
    
    # 停止前端
    if [ ! -z "$FRONTEND_PID" ]; then
        print_info "停止前端服务 (PID: $FRONTEND_PID)..."
        kill $FRONTEND_PID 2>/dev/null || true
    fi
    
    print_success "清理完成"
}

# 主函数
main() {
    print_info "🚀 AI智造项目启动脚本"
    print_info "================================"
    
    # 设置信号处理
    trap cleanup EXIT INT TERM
    
    # 检查环境
    check_requirements
    
    # 显示清理配置
    show_clean_config
    
    # 启动数据库服务
    start_postgres
    start_redis
    
    # 启动应用服务
    start_backend
    start_frontend
    
    # 显示状态
    show_status
    
    # 自动打开浏览器
    open_browser
    
    print_success "🎉 所有服务启动完成！"
    print_info "按 Ctrl+C 停止所有服务"
    
    # 等待用户中断
    wait
}

# 运行主函数
main "$@"
