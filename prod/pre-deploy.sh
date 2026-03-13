#!/bin/bash

# AI Creation 生产环境部署前置检查脚本
# 用于在部署前检查和配置服务器环境

set -e  # 遇到错误时退出

# 颜色输出函数
print_info() {
    echo -e "\033[34mℹ️  $1\033[0m"
}

print_success() {
    echo -e "\033[32m✅ $1\033[0m"
}

print_warning() {
    echo -e "\033[33m⚠️  $1\033[0m"
}

print_error() {
    echo -e "\033[31m❌ $1\033[0m"
}

# 检查是否为root用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        print_error "请使用root用户运行此脚本"
        exit 1
    fi
}

# 检查操作系统
check_os() {
    echo ""
    echo "1. 检查操作系统..."

    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
        VERSION=$VERSION_ID
        print_success "操作系统: $PRETTY_NAME"
    else
        print_error "无法识别操作系统"
        exit 1
    fi
}

# 检查并安装JDK
check_and_install_jdk() {
    echo ""
    echo "2. 检查JDK 21+..."

    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 21 ]; then
            print_success "JDK $JAVA_VERSION 已安装"
            return
        else
            print_warning "JDK版本过低: $JAVA_VERSION，需要JDK 21+"
        fi
    fi

    print_info "安装OpenJDK 21..."

    if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        apt update
        apt install -y openjdk-21-jdk
    elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ] || [ "$OS" = "almalinux" ]; then
        yum install -y java-21-openjdk-devel
    else
        print_error "不支持的操作系统: $OS"
        print_info "请手动安装JDK 21+"
        exit 1
    fi

    print_success "JDK 21 安装完成"
}

# 检查并安装Nginx
check_and_install_nginx() {
    echo ""
    echo "3. 检查Nginx..."

    if command -v nginx &> /dev/null; then
        NGINX_VERSION=$(nginx -v 2>&1 | cut -d'/' -f2)
        print_success "Nginx $NGINX_VERSION 已安装"
        return
    fi

    print_info "安装Nginx..."

    if [ "$OS" = "ubuntu" ] || [ "$OS" = "debian" ]; then
        apt update
        apt install -y nginx
    elif [ "$OS" = "centos" ] || [ "$OS" = "rhel" ] || [ "$OS" = "almalinux" ]; then
        yum install -y nginx
    else
        print_error "不支持的操作系统: $OS"
        print_info "请手动安装Nginx"
        exit 1
    fi

    systemctl start nginx
    systemctl enable nginx
    print_success "Nginx安装并启动成功"
}

# 检查网络连接
check_network() {
    echo ""
    echo "4. 检查网络连接..."
    if curl -s --max-time 5 https://www.baidu.com &>/dev/null; then
        print_success "互联网连接正常"
    else
        print_warning "互联网连接异常，可能影响某些功能"
    fi
}

# 检查磁盘空间
check_disk_space() {
    echo ""
    echo "5. 检查磁盘空间..."

    ROOT_SPACE=$(df / | tail -1 | awk '{print $4}')
    ROOT_SPACE_GB=$((ROOT_SPACE / 1024 / 1024))

    if [ $ROOT_SPACE_GB -lt 5 ]; then
        print_warning "根目录可用空间不足: ${ROOT_SPACE_GB}GB (建议至少5GB)"
    else
        print_success "磁盘空间充足: ${ROOT_SPACE_GB}GB 可用"
    fi
}

# 创建部署目录
create_deploy_directory() {
    echo ""
    echo "6. 创建部署目录结构..."

    mkdir -p /opt/AI-Creation/frontend
    mkdir -p /opt/AI-Creation/logs
    mkdir -p /opt/AI-Creation/logs/bk

    # 设置权限
    chown -R $SUDO_USER:$SUDO_USER /opt/AI-Creation/logs 2>/dev/null || true
    chown -R $SUDO_USER:$SUDO_USER /opt/AI-Creation/frontend 2>/dev/null || true

    print_success "部署目录创建完成"
}

# 创建systemd服务
create_systemd_service() {
    echo ""
    echo "7. 创建systemd服务配置..."

    cat > /etc/systemd/system/ai-creation.service << 'EOF'
[Unit]
Description=AI Creation Backend Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/AI-Creation
ExecStart=/usr/bin/java -jar /opt/AI-Creation/ai-creation-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    print_success "systemd服务配置创建完成"
}

# 显示系统信息
show_system_info() {
    echo ""
    echo "8. 系统信息汇总:"
    echo "   操作系统: $PRETTY_NAME"
    echo "   JDK版本: $(java -version 2>&1 | head -1)"
    echo "   Nginx版本: $(nginx -v 2>&1 2>/dev/null || echo '未安装')"
    echo "   部署目录: /opt/AI-Creation/"
    echo "   服务文件: /etc/systemd/system/ai-creation.service"
}

# 主函数
main() {
    print_info "🚀 AI Creation 部署前置检查脚本"
    print_info "==================================="

    check_root
    check_os
    check_and_install_jdk
    check_and_install_nginx
    check_network
    check_disk_space
    create_deploy_directory
    create_systemd_service
    show_system_info

    echo ""
    print_success "🎉 环境检查和配置完成！"
    echo ""
    print_info "接下来请："
    print_info "1. 上传prod目录到 /opt/AI-Creation/"
    print_info "2. 配置Nginx（参考prod/README.md）"
    print_info "3. 运行 ./start.sh 启动服务"
}

main "$@"