#!/bin/bash

# AI智造项目重启脚本
# 先停止所有服务，再重新启动

set -e  # 遇到错误时退出

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

# 检查脚本是否存在
check_scripts() {
    if [ ! -f "stop.sh" ]; then
        print_error "stop.sh 脚本不存在"
        exit 1
    fi

    if [ ! -f "start.sh" ]; then
        print_error "start.sh 脚本不存在"
        exit 1
    fi

    if [ ! -x "stop.sh" ]; then
        print_warning "stop.sh 没有执行权限，正在添加..."
        chmod +x stop.sh
    fi

    if [ ! -x "start.sh" ]; then
        print_warning "start.sh 没有执行权限，正在添加..."
        chmod +x start.sh
    fi
}

# 主函数
main() {
    print_info "🔄 AI智造项目重启脚本"
    print_info "================================"
    print_info "将依次执行: 停止服务 → 启动服务"
    print_info "================================"

    check_scripts

    print_info "第一步: 停止所有服务..."
    print_info ""

    # 执行停止脚本
    if ./stop.sh; then
        print_success "✅ 服务停止完成"
    else
        print_error "❌ 服务停止失败"
        exit 1
    fi

    print_info ""
    print_info "第二步: 重新启动所有服务..."
    print_info ""

    # 执行启动脚本（后台运行）
    print_info "启动服务中，请稍候..."
    ./start.sh &
    START_PID=$!

    # 等待一小段时间让服务启动
    sleep 5

    # 检查服务是否正在启动
    if ps -p $START_PID > /dev/null 2>&1; then
        print_success "✅ 服务重启完成"
        print_info ""
        print_info "🎉 项目重启成功！"
        print_info "服务正在后台运行 (PID: $START_PID)"
        print_info "使用 './stop.sh' 停止服务"
    else
        print_error "❌ 服务启动失败"
        exit 1
    fi
}

main "$@"