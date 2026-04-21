-- 已有库：用户表增加管理员标志
ALTER TABLE user_info ADD COLUMN IF NOT EXISTS is_admin BOOLEAN DEFAULT FALSE;
COMMENT ON COLUMN user_info.is_admin IS '是否管理员（true-可查看全部文章与配置）';
UPDATE user_info SET is_admin = TRUE WHERE user_name = 'admin' AND res_state = 1;
