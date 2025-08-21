-- 更新admin用户密码为123456的BCrypt哈希值
UPDATE user_info 
SET user_password = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    update_time = CURRENT_TIMESTAMP
WHERE user_name = 'admin';

-- 验证更新结果
SELECT user_name, user_password, update_time 
FROM user_info 
WHERE user_name = 'admin';
