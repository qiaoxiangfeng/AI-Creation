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
