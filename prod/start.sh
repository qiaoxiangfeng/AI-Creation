#!/bin/bash

# AI智造项目启动脚本
# 同时启动前端和后端，并自动打开网页

set -e  # 遇到错误时退出

# 指定本项目使用 JDK 21（IDEA 下载的 Microsoft JDK）
export JAVA_HOME="/Users/qiaoxiangfeng/Library/Java/JavaVirtualMachines/ms-21.0.8/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# 预置用户级 Node 安装路径，确保脚本内使用到正确的 Node 版本
export NPM_CONFIG_PREFIX="$HOME/.npm-global"
export N_PREFIX="$HOME/.n"
export PATH="$N_PREFIX/bin:$NPM_CONFIG_PREFIX/bin:$PATH"

# 环境变量配置（可通过 export 设置后执行脚本）
# CLEAN_LOGS=true           # 是否归档历史日志（默认 true）

CLEAN_LOGS=${CLEAN_LOGS:-true}

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

    if ! command -v java &> /dev/null; then
        print_error "Java 未找到，请确认 JDK 21 已安装: $JAVA_HOME"
        exit 1
    fi

    # 生产环境不需要 Maven/Node.js，直接使用预构建的JAR和静态文件

    print_success "环境检查通过 (Java: $(java -version 2>&1 | head -1), Node: $(node -v))"
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

# 启动 PostgreSQL
start_postgres() {
    print_info "检查 PostgreSQL 状态..."

    if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
        print_success "PostgreSQL 已在运行"
        return 0
    fi

    if command -v docker &> /dev/null; then
        print_info "尝试使用 Docker 启动 PostgreSQL..."

        if docker ps -q -f name=ai-creation-postgres | grep -q .; then
            print_info "启动现有 PostgreSQL 容器..."
            docker start ai-creation-postgres
        else
            print_info "创建并启动 PostgreSQL 容器..."
            docker run -d \
                --name ai-creation-postgres \
                -e POSTGRES_DB=ai_creation \
                -e POSTGRES_USER=postgres \
                -e POSTGRES_PASSWORD=123456 \
                -p 5432:5432 \
                postgres:15
        fi

        print_info "等待 PostgreSQL 启动..."
        for i in {1..30}; do
            if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
                print_success "PostgreSQL 启动成功"
                return 0
            fi
            sleep 1
        done

        print_error "PostgreSQL 启动超时"
        return 1
    else
        print_warning "Docker 未安装，请手动启动 PostgreSQL"
        print_info "PostgreSQL 配置: localhost:5432, 数据库: ai_creation, 用户: postgres, 密码: 123456"
        return 1
    fi
}

# 启动后端
start_backend() {
    print_info "启动后端服务..."

    # 检查服务是否已经正常运行
    if curl -s http://localhost:6666/api/v3/api-docs >/dev/null 2>&1; then
        print_success "后端服务已在正常运行，跳过启动"
        return 0
    fi

    if ! check_port 6666 "后端服务"; then
        print_warning "端口 6666 已被占用但服务未响应，可能有其他进程占用"
    fi

    # 生产环境使用预构建的JAR文件，无需Maven清理

    # 归档非当天的日志文件
    if [ "$CLEAN_LOGS" = "true" ]; then
        print_info "归档非当天的日志文件到logs/bk目录..."

        # 创建logs/bk目录（如果不存在）
        mkdir -p logs/bk

        # 归档根目录下的非当天日志文件（修改时间超过0天的文件）
        root_moved_count=$(find . -maxdepth 1 -type f -name "*.log*" -mtime +0 -exec mv {} logs/bk/ \; -print 2>/dev/null | wc -l)

        # 归档logs目录下的非当天日志文件（修改时间超过0天的文件）
        logs_moved_count=$(find logs -maxdepth 1 -type f -name "*.log*" -mtime +0 -exec mv {} logs/bk/ \; -print 2>/dev/null | wc -l)

        total_moved=$((root_moved_count + logs_moved_count))

        # 清理空的归档目录（如果存在）
        if [ -d "logs/archive" ]; then
            archive_moved=$(find logs/archive -type f -name "*.log*" -exec mv {} logs/bk/ \; -print 2>/dev/null | wc -l)
            total_moved=$((total_moved + archive_moved))
            # 删除空的归档目录
            find logs/archive -type d -empty -delete 2>/dev/null || true
        fi

        if [ "$total_moved" -gt 0 ]; then
            print_success "已归档 $total_moved 个非当天日志文件到logs/bk目录"
        else
            print_info "没有找到需要归档的非当天日志文件"
        fi
    fi

    # 生产环境使用预编译的JAR文件，跳过编译步骤

    # 后台启动 Spring Boot JAR
    print_info "启动 Spring Boot 应用..."
    mkdir -p logs
    nohup bash -c "export JAVA_HOME=\"$JAVA_HOME\" && java -jar ai-creation-1.0.0.jar --spring.profiles.active=prod --server.servlet.context-path=/api" > logs/backend.out.log 2>&1 &
    BACKEND_PID=$!

    print_info "等待后端服务启动..."
    for i in {1..60}; do
        if curl -s http://localhost:6666/api/v3/api-docs >/dev/null 2>&1; then
            print_success "后端服务启动成功 (PID: $BACKEND_PID)"
            return 0
        fi
        sleep 1
    done

    print_error "后端服务启动超时，请查看 logs/backend.out.log"
    return 1
}

 
# 如果存在前端压缩包则解压更新
prepare_frontend() {
    if [ -f "frontend.zip" ]; then
        print_info "检测到前端压缩包 frontend.zip，开始更新前端静态资源..."

        if [ -d "frontend" ]; then
            print_info "删除已有的 frontend 目录..."
            rm -rf frontend
        fi

        print_info "解压 frontend.zip..."
        unzip -q frontend.zip
        if [ $? -ne 0 ]; then
            print_error "解压 frontend.zip 失败，请检查压缩包是否正常"
            exit 1
        fi

        print_info "删除已使用的 frontend.zip..."
        rm -f frontend.zip

        print_success "前端静态资源更新完成"
    fi
}

# 显示生产环境访问信息
show_access_info() {
    print_info "🌐 生产环境访问地址："
    print_info "   前端页面: http://121.43.252.250/"
    print_info "   后端API:  http://121.43.252.250/api"
    print_info "   API文档:  http://121.43.252.250/api/swagger-ui/index.html"
    print_info ""
    print_info "🔑 默认账号: admin"
    print_info "🔑 默认密码: 123456"
}

# 显示服务状态
show_status() {
    print_info "服务状态："

    if curl -s http://localhost:6666/api/v3/api-docs >/dev/null 2>&1; then
        print_success "✅ 后端服务: 运行中 (http://localhost:6666/api)"
    else
        print_error "❌ 后端服务: 未运行"
    fi

    # 生产环境前端为静态文件，由Nginx提供，无需检查进程状态
    print_info "前端服务: 静态文件服务（由Nginx提供）"
}

# 启动后台日志清理任务
start_log_cleanup_task() {
    print_info "启动后台日志清理任务..."

    # 创建后台日志清理脚本
    cat > logs_cleanup_task.sh << 'EOF'
#!/bin/bash
while true; do
    # 每小时检查一次，将非当天的日志文件归档到logs/bk目录
    sleep 3600  # 1小时 = 3600秒

    # 创建logs/bk目录（如果不存在）
    mkdir -p logs/bk

    # 归档非当天的日志文件到logs/bk目录
    if [ -d "logs" ]; then
        # 归档logs目录下的非当天日志文件
        find logs -maxdepth 1 -name "*.log*" -type f -mtime +0 -exec mv {} logs/bk/ \; 2>/dev/null || true
        # 归档根目录下的非当天日志文件
        find . -maxdepth 1 -name "*.log*" -type f -mtime +0 -exec mv {} logs/bk/ \; 2>/dev/null || true
    fi
done
EOF

    chmod +x logs_cleanup_task.sh
    nohup ./logs_cleanup_task.sh > /dev/null 2>&1 &
    LOG_CLEANUP_PID=$!

    print_success "日志清理任务已启动 (PID: $LOG_CLEANUP_PID)"
}

# 主函数
main() {
    print_info "🚀 AI智造项目启动脚本"
    print_info "================================"

    check_requirements
    prepare_frontend
    start_backend
    show_status
    start_log_cleanup_task
    show_access_info

    print_success "🎉 所有服务启动完成！"
    print_info "后端与日志清理任务已在后台运行。"
    print_info "如需停止服务，请执行 './stop.sh' 或使用 './restart.sh' 重启。"
}

main "$@"
