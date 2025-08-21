#!/bin/bash

# AI智造项目快速清理脚本

set -e

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

# 显示帮助信息
show_help() {
    echo "AI智造项目快速清理脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -a, --all          清理所有内容（包括Maven缓存和node_modules）"
    echo "  -b, --build        清理构建目录和编译文件"
    echo "  -l, --logs         清理日志文件"
    echo "  -m, --maven        清理Maven本地仓库缓存"
    echo "  -n, --node         清理node_modules和前端缓存"
    echo "  -h, --help         显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 -b              # 只清理构建目录"
    echo "  $0 -a              # 清理所有内容"
    echo "  $0 -m -n           # 清理Maven缓存和node_modules"
}

# 清理构建目录
clean_build() {
    print_info "清理构建目录和编译文件..."
    
    # 清理Maven构建目录
    if [ -d "target" ]; then
        rm -rf target
        print_success "已清理Maven构建目录"
    fi
    
    # 清理IDE生成的文件
    find . -name "*.class" -delete 2>/dev/null || true
    find . -name "*.jar" -delete 2>/dev/null || true
    find . -name "bin" -type d -exec rm -rf {} + 2>/dev/null || true
    
    print_success "构建目录清理完成"
}

# 清理日志文件
clean_logs() {
    print_info "清理日志文件..."
    
    # 清理根目录的日志文件
    rm -f *.log 2>/dev/null || true
    
    # 清理logs目录的日志文件
    if [ -d "logs" ]; then
        rm -f logs/*.log 2>/dev/null || true
    fi
    
    print_success "日志文件清理完成"
}

# 清理Maven缓存
clean_maven() {
    print_info "清理Maven本地仓库缓存..."
    
    if command -v mvn &> /dev/null; then
        mvn dependency:purge-local-repository -q
        print_success "Maven缓存清理完成"
    else
        print_error "Maven未安装"
    fi
}

# 清理前端缓存
clean_node() {
    print_info "清理前端缓存和依赖..."
    
    if [ -d "frontend" ]; then
        cd frontend
        
        # 清理构建缓存
        rm -rf dist .vite .cache 2>/dev/null || true
        
        # 清理node_modules
        if [ -d "node_modules" ]; then
            rm -rf node_modules package-lock.json
            print_success "已清理node_modules"
        fi
        
        cd ..
        print_success "前端缓存清理完成"
    else
        print_warning "前端目录不存在"
    fi
}

# 清理所有内容
clean_all() {
    print_info "开始全面清理..."
    
    clean_build
    clean_logs
    clean_maven
    clean_node
    
    print_success "所有内容清理完成！"
}

# 主函数
main() {
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi
    
    print_info "🧹 AI智造项目快速清理脚本"
    print_info "================================"
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -a|--all)
                clean_all
                shift
                ;;
            -b|--build)
                clean_build
                shift
                ;;
            -l|--logs)
                clean_logs
                shift
                ;;
            -m|--maven)
                clean_maven
                shift
                ;;
            -n|--node)
                clean_node
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                print_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    print_success "🎉 清理完成！"
}

# 运行主函数
main "$@"
