-- 删除已存在的表
DROP TABLE IF EXISTS dictionary;

-- 创建字典表
CREATE TABLE dictionary (
    id BIGSERIAL PRIMARY KEY,
    dict_key VARCHAR(100) NOT NULL,
    dict_value VARCHAR(500) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    res_state SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
-- 索引命名规范：idx_表名_字段名
CREATE INDEX IF NOT EXISTS idx_dictionary_key ON dictionary(dict_key);
CREATE INDEX IF NOT EXISTS idx_dictionary_create_time ON dictionary(create_time);
CREATE INDEX IF NOT EXISTS idx_dictionary_res_state ON dictionary(res_state);

-- 添加表和字段注释
-- PostgreSQL使用COMMENT ON语句添加注释
COMMENT ON TABLE dictionary IS '字典表';
COMMENT ON COLUMN dictionary.id IS '主键ID';
COMMENT ON COLUMN dictionary.dict_key IS '字典键';
COMMENT ON COLUMN dictionary.dict_value IS '字典值';
COMMENT ON COLUMN dictionary.sort_order IS '排序顺序';
COMMENT ON COLUMN dictionary.res_state IS '删除标记（1-有效，0-无效）';
COMMENT ON COLUMN dictionary.create_time IS '创建时间';
COMMENT ON COLUMN dictionary.update_time IS '更新时间';

-- 添加约束（可选）
-- ALTER TABLE dictionary ADD CONSTRAINT chk_res_state CHECK (res_state IN (0, 1));
-- ALTER TABLE dictionary ADD CONSTRAINT chk_dict_key_length CHECK (LENGTH(dict_key) >= 1);
-- ALTER TABLE dictionary ADD CONSTRAINT chk_dict_value_length CHECK (LENGTH(dict_value) >= 1);