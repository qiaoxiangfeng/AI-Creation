# AI智造平台

基于Spring Boot + Vue3的前后端集成AI智造项目

## 项目简介

AI智造平台是一个现代化的智能制造数字化解决方案，采用前后端分离架构，后端使用Spring Boot框架，前端使用Vue3框架，提供用户管理、项目管理、任务管理等核心功能。

## 技术栈

### 后端技术
- **框架**: Spring Boot 3.2.0
- **数据库**: PostgreSQL 15
- **ORM**: MyBatis 3.0.2
- **缓存**: Redis
- **对象映射**: MapStruct
- **API文档**: Swagger 3.0
- **认证**: JWT
- **构建工具**: Maven

### 前端技术
- **框架**: Vue 3.3.8
- **路由**: Vue Router 4.2.5
- **状态管理**: Pinia 2.1.7
- **UI组件库**: Element Plus 2.4.4
- **HTTP客户端**: Axios 1.6.0
- **构建工具**: Vite 5.0.0

## 项目结构

```
AI-Creation/
├── src/                          # 后端源码
│   ├── main/
│   │   ├── java/com/aicreation/
│   │   │   ├── controller/       # 控制器层
│   │   │   ├── service/          # 服务层
│   │   │   ├── mapper/           # 数据访问层
│   │   │   ├── entity/           # 实体类
│   │   │   ├── dto/              # 数据传输对象
│   │   │   ├── bo/               # 业务对象
│   │   │   └── common/           # 公共组件
│   │   └── resources/
│   │       ├── mapper/           # MyBatis映射文件
│   │       ├── sql/              # 数据库脚本
│   │       └── application.yml   # 配置文件
│   └── test/                     # 测试代码
├── frontend/                     # 前端源码
│   ├── src/
│   │   ├── components/           # 公共组件
│   │   ├── views/                # 页面组件
│   │   ├── router/               # 路由配置
│   │   ├── store/                # 状态管理
│   │   ├── api/                  # API接口
│   │   ├── utils/                # 工具函数
│   │   └── assets/               # 静态资源
│   ├── package.json              # 依赖配置
│   └── vite.config.js            # 构建配置
├── pom.xml                       # Maven配置
└── README.md                     # 项目说明
```

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- PostgreSQL 15+
- Redis 6.0+ (可选，用于缓存)
- Node.js 16+ (前端开发)

### 本地开发环境搭建

**1. 安装PostgreSQL**
```bash
# macOS
brew install postgresql
brew services start postgresql

# Windows
# 下载并安装PostgreSQL官方安装包

# Linux (Ubuntu/Debian)
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

**2. 配置数据库**
```bash
# 连接到PostgreSQL
psql -U postgres

# 创建数据库
CREATE DATABASE ai_creation_dev;

# 退出
\q

# 执行初始化脚本
psql -U postgres -d ai_creation_dev -f src/main/resources/sql/init-postgres.sql
```

**3. 配置Redis (可选)**
```bash
# macOS
brew install redis
brew services start redis

# Windows
# 下载并安装Redis for Windows

# Linux
sudo apt-get install redis-server
sudo systemctl start redis-server
```

### 后端启动

1. **克隆项目**
```bash
git clone <repository-url>
cd AI-Creation
```

2. **启动应用**
```bash
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

**注意**: 确保PostgreSQL服务已启动，数据库已创建并执行了初始化脚本

### 前端启动

1. **安装依赖**
```bash
cd frontend
npm install
```

2. **启动开发服务器**
```bash
npm run dev
```

前端应用将在 `http://localhost:3000` 启动

3. **构建生产版本**
```bash
npm run build
```

## 功能特性

### 用户管理
- 用户注册、登录、注销
- 用户信息管理（增删改查）
- 用户状态管理
- 权限控制

### 项目管理
- 项目创建、编辑、删除
- 项目状态跟踪
- 项目进度管理
- 项目成员管理

### 任务管理
- 任务创建、分配、跟踪
- 任务优先级管理
- 任务状态管理
- 任务进度监控

### 系统功能
- 响应式设计
- 国际化支持
- 主题切换
- 数据可视化

## API文档

启动后端服务后，访问 `http://localhost:8080/swagger-ui.html` 查看API文档

## 开发规范

项目严格遵循以下开发规范：

### 代码规范
- 使用大驼峰命名法命名类
- 使用小驼峰命名法命名方法和变量
- 所有类必须有Javadoc注释
- 方法参数不得超过5个
- 类不超过1000行，方法不超过一屏

### 数据库规范
- 表名使用小写字母和下划线
- 字段名使用小写字母和下划线
- 主键统一使用`id`
- 创建时间使用`create_time`
- 更新时间使用`update_time`
- 删除标记使用`res_state`

### API设计规范
- 使用RESTful API设计
- 统一响应格式
- 使用HTTP状态码
- 参数验证和错误处理

## 部署说明

### 生产环境配置
1. 修改 `application-prod.yml` 配置
2. 设置环境变量
3. 使用 `mvn clean package` 打包
4. 部署到服务器

### 本地打包运行
```bash
# 打包应用
mvn clean package -DskipTests

# 运行jar包
java -jar target/ai-creation-1.0.0.jar --spring.profiles.active=prod
```

## 测试

### 后端测试
```bash
mvn test
```

### 前端测试
```bash
cd frontend
npm run test
```

## 故障排除

### 常见问题

**1. 数据库连接失败**
```bash
# 检查PostgreSQL服务状态
# macOS
brew services list | grep postgresql

# Linux
sudo systemctl status postgresql

# 检查端口是否被占用
netstat -an | grep 5432
```

**2. 端口被占用**
```bash
# 查看端口占用
lsof -i :8080
lsof -i :3000

# 杀死进程
kill -9 <PID>
```

**3. 前端依赖安装失败**
```bash
# 清除npm缓存
npm cache clean --force

# 删除node_modules重新安装
rm -rf node_modules package-lock.json
npm install
```

**4. 后端编译失败**
```bash
# 清理Maven缓存
mvn clean

# 重新下载依赖
mvn dependency:resolve
```

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- 项目维护者: AI-Creation Team
- 邮箱: team@aicreation.com
- 项目地址: [GitHub Repository](https://github.com/your-username/ai-creation)

## 更新日志

### v1.0.0 (2024-01-01)
- 初始版本发布
- 基础用户管理功能
- 基础项目管理功能
- 基础任务管理功能
- 响应式前端界面 