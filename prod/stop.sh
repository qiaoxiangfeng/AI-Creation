#!/bin/bash

# AI智造项目停止脚本
# 只停止前后端服务，保持数据库运行（项目未使用 Redis）

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 停止后端服务
stop_backend() {
    print_info "停止后端服务..."
    
    # 查找运行在6666端口的进程（生产环境后端端口）
    BACKEND_PID=$(lsof -ti:6666 2>/dev/null || echo "")
    
    if [ ! -z "$BACKEND_PID" ]; then
        print_info "找到后端进程 (PID: $BACKEND_PID)，正在停止..."
        kill $BACKEND_PID 2>/dev/null || true
        
        # 等待进程结束
        for i in {1..10}; do
            if ! lsof -ti:6666 >/dev/null 2>&1; then
                print_success "后端服务已停止"
                return 0
            fi
            sleep 1
        done
        
        # 强制杀死进程
        print_warning "强制停止后端服务..."
        kill -9 $BACKEND_PID 2>/dev/null || true
        print_success "后端服务已强制停止"
    else
        print_info "后端服务未运行"
    fi
}

# 保留日志文件（不再清理）
keep_logs() {
    print_info "保留日志文件..."
    print_success "日志文件已保留"
}

# 显示服务状态
show_status() {
    print_info "服务状态检查："
    
    # 检查后端
    if lsof -ti:6666 >/dev/null 2>&1; then
        print_error "❌ 后端服务: 仍在运行 (端口6666)"
    else
        print_success "✅ 后端服务: 已停止 (端口6666)"
    fi

    # 检查PostgreSQL
    if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
        print_success "✅ PostgreSQL: 仍在运行 (保持运行)"
    else
        print_warning "⚠️  PostgreSQL: 未运行"
    fi
    
    # 项目未使用 Redis，跳过检查
}

# 主函数
main() {
    print_info "🛑 AI智造项目停止脚本"
    print_info "================================"
    print_info "只停止前后端服务，保持数据库运行（无 Redis）"
    print_info "================================"
    
    stop_backend
    keep_logs
    
    print_info ""
    show_status
    
    print_success "🎉 前后端服务已停止！"
    print_info "💡 数据库和Redis服务保持运行，可以继续使用"
}

main "$@"

