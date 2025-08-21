# AI智造项目脚本使用说明

## 脚本概览

本项目提供了两个主要的脚本：
- `start.sh` - 项目启动脚本（包含自动清理功能）
- `clean.sh` - 快速清理脚本

## 1. 启动脚本 (start.sh)

### 功能特性
- 🚀 自动启动前端和后端服务
- 🧹 自动清理旧包和缓存
- 🗄️ 自动启动数据库服务（PostgreSQL、Redis）
- 🌐 自动打开浏览器
- 📊 显示服务状态

### 使用方法

#### 基本启动
```bash
./start.sh
```

#### 自定义清理级别启动
```bash
# 启用Maven缓存清理
export CLEAN_MAVEN_CACHE=true
./start.sh

# 启用Node模块清理
export CLEAN_NODE_MODULES=true
./start.sh

# 禁用日志清理
export CLEAN_LOGS=false
./start.sh

# 禁用构建目录清理
export CLEAN_BUILD=false
./start.sh
```

### 环境变量配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `CLEAN_MAVEN_CACHE` | `false` | 是否清理Maven本地仓库缓存 |
| `CLEAN_NODE_MODULES` | `false` | 是否清理node_modules重新安装 |
| `CLEAN_LOGS` | `true` | 是否清理日志文件 |
| `CLEAN_BUILD` | `true` | 是否清理构建目录 |

### 启动流程
1. 环境检查（Java、Maven、Node.js、npm）
2. 显示清理配置
3. 启动PostgreSQL和Redis
4. 清理后端旧包和缓存
5. 编译后端项目
6. 启动后端服务
7. 清理前端缓存
8. 启动前端服务
9. 显示服务状态
10. 自动打开浏览器

## 2. 清理脚本 (clean.sh)

### 功能特性
- 🧹 快速清理项目文件
- 🔧 支持选择性清理
- 📝 详细的清理日志
- 🎯 针对性的清理选项

### 使用方法

#### 显示帮助信息
```bash
./clean.sh --help
```

#### 清理所有内容
```bash
./clean.sh --all
# 或
./clean.sh -a
```

#### 选择性清理
```bash
# 只清理构建目录
./clean.sh --build
# 或
./clean.sh -b

# 只清理日志文件
./clean.sh --logs
# 或
./clean.sh -l

# 只清理Maven缓存
./clean.sh --maven
# 或
./clean.sh -m

# 只清理前端缓存
./clean.sh --node
# 或
./clean.sh -n

# 组合清理
./clean.sh -m -n  # 清理Maven缓存和前端缓存
```

### 清理内容说明

| 选项 | 清理内容 |
|------|----------|
| `--build` | Maven构建目录、编译文件、IDE生成文件 |
| `--logs` | 日志文件（*.log） |
| `--maven` | Maven本地仓库缓存 |
| `--node` | node_modules、前端构建缓存 |
| `--all` | 以上所有内容 |

## 3. 使用场景

### 日常开发
```bash
# 正常启动，只清理必要的构建文件
./start.sh
```

### 解决依赖问题
```bash
# 清理Maven缓存后启动
export CLEAN_MAVEN_CACHE=true
./start.sh
```

### 解决前端问题
```bash
# 清理前端依赖后启动
export CLEAN_NODE_MODULES=true
./start.sh
```

### 完全重新开始
```bash
# 清理所有内容
./clean.sh --all

# 然后启动
./start.sh
```

### 快速清理
```bash
# 只清理构建文件
./clean.sh --build

# 只清理日志
./clean.sh --logs
```

## 4. 注意事项

### 启动脚本
- 确保PostgreSQL和Redis服务可用
- 首次启动可能需要较长时间（依赖下载）
- 修改环境变量后需要重新启动脚本

### 清理脚本
- 清理操作不可逆，请确认后再执行
- 清理Maven缓存会重新下载依赖，耗时较长
- 清理node_modules会重新安装前端依赖

### 环境要求
- Java 17+
- Maven 3.6+
- Node.js 16+
- npm 8+
- PostgreSQL 12+
- Redis 6+

## 5. 故障排除

### 启动失败
1. 检查环境变量是否正确设置
2. 检查端口是否被占用
3. 查看日志文件了解详细错误
4. 尝试使用清理脚本清理后重新启动

### 清理失败
1. 检查文件权限
2. 确保没有进程正在使用相关文件
3. 手动删除相关目录

### 服务启动慢
1. 检查网络连接
2. 考虑使用国内Maven镜像
3. 使用`CLEAN_MAVEN_CACHE=false`避免清理Maven缓存

## 6. 示例配置

### 开发环境配置
```bash
# 只清理构建文件，保留依赖缓存
export CLEAN_MAVEN_CACHE=false
export CLEAN_NODE_MODULES=false
export CLEAN_LOGS=true
export CLEAN_BUILD=true
```

### 生产环境配置
```bash
# 全面清理，确保代码最新
export CLEAN_MAVEN_CACHE=true
export CLEAN_NODE_MODULES=true
export CLEAN_LOGS=true
export CLEAN_BUILD=true
```

### 调试环境配置
```bash
# 保留所有缓存，快速启动
export CLEAN_MAVEN_CACHE=false
export CLEAN_NODE_MODULES=false
export CLEAN_LOGS=false
export CLEAN_BUILD=false
```

---

**版本**: 1.0.0  
**更新日期**: 2025/08/17  
**维护者**: AI-Creation Team
