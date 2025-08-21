#!/bin/bash

# AI智造项目停止脚本
# 只停止前后端服务，保持数据库和Redis运行

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
    
    # 查找运行在8080端口的进程
    BACKEND_PID=$(lsof -ti:8080 2>/dev/null || echo "")
    
    if [ ! -z "$BACKEND_PID" ]; then
        print_info "找到后端进程 (PID: $BACKEND_PID)，正在停止..."
        kill $BACKEND_PID 2>/dev/null || true
        
        # 等待进程结束
        for i in {1..10}; do
            if ! lsof -ti:8080 >/dev/null 2>&1; then
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

# 停止前端服务
stop_frontend() {
    print_info "停止前端服务..."
    
    # 查找运行在5173和5174端口的进程（前端可能使用不同端口）
    FRONTEND_PID_5173=$(lsof -ti:5173 2>/dev/null || echo "")
    FRONTEND_PID_5174=$(lsof -ti:5174 2>/dev/null || echo "")
    
    if [ ! -z "$FRONTEND_PID_5173" ]; then
        print_info "找到前端进程 (端口5173, PID: $FRONTEND_PID_5173)，正在停止..."
        kill $FRONTEND_PID_5173 2>/dev/null || true
        
        # 等待进程结束
        for i in {1..10}; do
            if ! lsof -ti:5173 >/dev/null 2>&1; then
                print_success "前端服务(端口5173)已停止"
                break
            fi
            sleep 1
        done
        
        # 强制杀死进程
        if lsof -ti:5173 >/dev/null 2>&1; then
            print_warning "强制停止前端服务(端口5173)..."
            kill -9 $FRONTEND_PID_5173 2>/dev/null || true
            print_success "前端服务(端口5173)已强制停止"
        fi
    fi
    
    if [ ! -z "$FRONTEND_PID_5174" ]; then
        print_info "找到前端进程 (端口5174, PID: $FRONTEND_PID_5174)，正在停止..."
        kill $FRONTEND_PID_5174 2>/dev/null || true
        
        # 等待进程结束
        for i in {1..10}; do
            if ! lsof -ti:5174 >/dev/null 2>&1; then
                print_success "前端服务(端口5174)已停止"
                break
            fi
            sleep 1
        done
        
        # 强制杀死进程
        if lsof -ti:5174 >/dev/null 2>&1; then
            print_warning "强制停止前端服务(端口5174)..."
            kill -9 $FRONTEND_PID_5174 2>/dev/null || true
            print_success "前端服务(端口5174)已强制停止"
        fi
    fi
    
    if [ -z "$FRONTEND_PID_5173" ] && [ -z "$FRONTEND_PID_5174" ]; then
        print_info "前端服务未运行"
    fi
}

# 清理日志文件
cleanup_logs() {
    print_info "清理日志文件..."
    
    if [ -f "logs/backend.out.log" ]; then
        rm "logs/backend.out.log"
        print_info "已删除 logs/backend.out.log"
    fi
    
    if [ -f "logs/frontend.out.log" ]; then
        rm "logs/frontend.out.log"
        print_info "已删除 logs/frontend.out.log"
    fi
    
    print_success "日志清理完成"
}

# 显示服务状态
show_status() {
    print_info "服务状态检查："
    
    # 检查后端
    if lsof -ti:8080 >/dev/null 2>&1; then
        print_error "❌ 后端服务: 仍在运行"
    else
        print_success "✅ 后端服务: 已停止"
    fi
    
    # 检查前端
    if lsof -ti:5173 >/dev/null 2>&1 || lsof -ti:5174 >/dev/null 2>&1; then
        print_error "❌ 前端服务: 仍在运行"
    else
        print_success "✅ 前端服务: 已停止"
    fi
    
    # 检查PostgreSQL
    if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
        print_success "✅ PostgreSQL: 仍在运行 (保持运行)"
    else
        print_warning "⚠️  PostgreSQL: 未运行"
    fi
    
    # 检查Redis
    if redis-cli ping >/dev/null 2>&1; then
        print_success "✅ Redis: 仍在运行 (保持运行)"
    else
        print_warning "⚠️  Redis: 未运行"
    fi
}

# 主函数
main() {
    print_info "🛑 AI智造项目停止脚本"
    print_info "================================"
    print_info "只停止前后端服务，保持数据库和Redis运行"
    print_info "================================"
    
    stop_backend
    stop_frontend
    cleanup_logs
    
    print_info ""
    show_status
    
    print_success "🎉 前后端服务已停止！"
    print_info "💡 数据库和Redis服务保持运行，可以继续使用"
}

main "$@"

