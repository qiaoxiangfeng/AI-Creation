-- 字典表增加创建人（已有库执行一次）
ALTER TABLE dictionary ADD COLUMN IF NOT EXISTS create_user_id BIGINT;
COMMENT ON COLUMN dictionary.create_user_id IS '创建人用户ID（NULL 表示全局字典，所有用户可见）';
CREATE INDEX IF NOT EXISTS idx_dictionary_create_user_id ON dictionary(create_user_id);
