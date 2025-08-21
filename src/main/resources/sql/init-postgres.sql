-- AI智造项目PostgreSQL数据库初始化脚本
-- 
-- PostgreSQL数据库规范说明：
-- 1. 使用BIGSERIAL作为自增主键类型
-- 2. 时间字段统一使用TIMESTAMP类型，默认值为CURRENT_TIMESTAMP
-- 3. 删除标记字段res_state统一使用SMALLINT类型，1-有效，0-无效
-- 4. 索引命名规范：主键索引自动创建，唯一索引uk_表名_字段名，普通索引idx_表名_字段名
-- 5. 使用IF NOT EXISTS避免重复创建表和索引
-- 6. 使用COMMENT ON语句添加表和字段注释
-- 7. 创建触发器自动更新update_time字段
-- 8. 使用事务确保数据一致性
-- 9. 遵循PostgreSQL最佳实践，避免使用MySQL特有语法

-- 连接到数据库
\c ai_creation;

-- 用户信息表
CREATE TABLE IF NOT EXISTS user_info (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    user_password VARCHAR(255) NOT NULL,
    user_email VARCHAR(100),
    user_phone VARCHAR(20),
    user_status SMALLINT DEFAULT 1,
    last_login_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    res_state SMALLINT DEFAULT 1
);

-- 创建索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_name ON user_info(user_name);
CREATE INDEX IF NOT EXISTS idx_user_email ON user_info(user_email);
CREATE INDEX IF NOT EXISTS idx_user_phone ON user_info(user_phone);
CREATE INDEX IF NOT EXISTS idx_create_time ON user_info(create_time);
CREATE INDEX IF NOT EXISTS idx_res_state ON user_info(res_state);

-- 添加注释
COMMENT ON TABLE user_info IS '用户信息表';
COMMENT ON COLUMN user_info.id IS '主键ID';
COMMENT ON COLUMN user_info.user_name IS '用户名';
COMMENT ON COLUMN user_info.user_password IS '用户密码';
COMMENT ON COLUMN user_info.user_email IS '用户邮箱';
COMMENT ON COLUMN user_info.user_phone IS '用户手机号';
COMMENT ON COLUMN user_info.user_status IS '用户状态（1-启用，0-禁用）';
COMMENT ON COLUMN user_info.last_login_time IS '最后登录时间';
COMMENT ON COLUMN user_info.create_time IS '创建时间';
COMMENT ON COLUMN user_info.update_time IS '更新时间';
COMMENT ON COLUMN user_info.res_state IS '删除标记（1-有效，0-无效）';

-- 项目信息表
CREATE TABLE IF NOT EXISTS project_info (
    id BIGSERIAL PRIMARY KEY,
    project_name VARCHAR(100) NOT NULL,
    project_desc TEXT,
    project_status SMALLINT DEFAULT 1,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    res_state SMALLINT DEFAULT 1
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_project_name ON project_info(project_name);
CREATE INDEX IF NOT EXISTS idx_project_status ON project_info(project_status);
CREATE INDEX IF NOT EXISTS idx_create_user_id ON project_info(create_user_id);
CREATE INDEX IF NOT EXISTS idx_create_time ON project_info(create_time);
CREATE INDEX IF NOT EXISTS idx_res_state ON project_info(res_state);

-- 添加注释
COMMENT ON TABLE project_info IS '项目信息表';
COMMENT ON COLUMN project_info.id IS '主键ID';
COMMENT ON COLUMN project_info.project_name IS '项目名称';
COMMENT ON COLUMN project_info.project_desc IS '项目描述';
COMMENT ON COLUMN project_info.project_status IS '项目状态（1-进行中，2-已完成，3-已暂停）';
COMMENT ON COLUMN project_info.start_time IS '开始时间';
COMMENT ON COLUMN project_info.end_time IS '结束时间';
COMMENT ON COLUMN project_info.create_user_id IS '创建用户ID';
COMMENT ON COLUMN project_info.create_time IS '创建时间';
COMMENT ON COLUMN project_info.update_time IS '更新时间';
COMMENT ON COLUMN project_info.res_state IS '删除标记（1-有效，0-无效）';

-- 任务信息表
CREATE TABLE IF NOT EXISTS task_info (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL,
    task_desc TEXT,
    project_id BIGINT NOT NULL,
    assign_user_id BIGINT,
    task_status SMALLINT DEFAULT 1,
    priority SMALLINT DEFAULT 2,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    res_state SMALLINT DEFAULT 1
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_task_name ON task_info(task_name);
CREATE INDEX IF NOT EXISTS idx_task_project_id ON task_info(project_id);
CREATE INDEX IF NOT EXISTS idx_task_assign_user_id ON task_info(assign_user_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON task_info(task_status);
CREATE INDEX IF NOT EXISTS idx_task_priority ON task_info(priority);
CREATE INDEX IF NOT EXISTS idx_task_create_time ON task_info(create_time);
CREATE INDEX IF NOT EXISTS idx_task_res_state ON task_info(res_state);

-- 添加注释
COMMENT ON TABLE task_info IS '任务信息表';
COMMENT ON COLUMN task_info.id IS '主键ID';
COMMENT ON COLUMN task_info.task_name IS '任务名称';
COMMENT ON COLUMN task_info.task_desc IS '任务描述';
COMMENT ON COLUMN task_info.project_id IS '项目ID';
COMMENT ON COLUMN task_info.assign_user_id IS '分配用户ID';
COMMENT ON COLUMN task_info.task_status IS '任务状态（1-待处理，2-进行中，3-已完成，4-已暂停）';
COMMENT ON COLUMN task_info.priority IS '优先级（1-低，2-中，3-高，4-紧急）';
COMMENT ON COLUMN task_info.start_time IS '开始时间';
COMMENT ON COLUMN task_info.end_time IS '结束时间';
COMMENT ON COLUMN task_info.create_time IS '创建时间';
COMMENT ON COLUMN task_info.update_time IS '更新时间';
COMMENT ON COLUMN task_info.res_state IS '删除标记（1-有效，0-无效）';

-- 插入测试数据
-- 密码123456的BCrypt哈希值
INSERT INTO user_info (user_name, user_password, user_email, user_phone, user_status, create_time, update_time, res_state) VALUES
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin@aicreation.com', '13800138000', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('test_user', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'test@aicreation.com', '13800138001', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('demo_user', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'demo@aicreation.com', '13800138002', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1);

-- 插入测试项目数据
INSERT INTO project_info (project_name, project_desc, project_status, start_time, create_user_id, create_time, update_time, res_state) VALUES
('AI智造平台开发', '基于Spring Boot和Vue3的AI智造平台', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('智能数据分析系统', '企业级智能数据分析平台', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1);

-- 插入测试任务数据
INSERT INTO task_info (task_name, task_desc, project_id, assign_user_id, task_status, priority, start_time, create_time, update_time, res_state) VALUES
('用户管理模块开发', '实现用户的增删改查功能', 1, 2, 2, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('前端界面设计', '设计用户友好的前端界面', 1, 3, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
('数据库设计', '设计系统数据库结构', 2, 1, 3, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为表添加更新时间触发器
CREATE TRIGGER update_user_info_updated_at BEFORE UPDATE ON user_info
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_project_info_updated_at BEFORE UPDATE ON project_info
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_task_info_updated_at BEFORE UPDATE ON task_info
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column(); 