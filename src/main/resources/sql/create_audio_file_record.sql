-- 删除已存在的表
DROP TABLE IF EXISTS audio_file_record;

-- 创建音频文件记录表
CREATE TABLE audio_file_record (
    id BIGSERIAL PRIMARY KEY,
    req_id VARCHAR(64) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    voice_type VARCHAR(100),
    text_content TEXT,
    synthesis_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64)
);

-- 创建索引
CREATE INDEX idx_audio_file_record_req_id ON audio_file_record(req_id);
CREATE INDEX idx_audio_file_record_create_time ON audio_file_record(create_time);
CREATE INDEX idx_audio_file_record_status ON audio_file_record(status);

-- 添加注释
COMMENT ON TABLE audio_file_record IS '音频文件记录表';
COMMENT ON COLUMN audio_file_record.id IS '主键ID';
COMMENT ON COLUMN audio_file_record.req_id IS '请求ID';
COMMENT ON COLUMN audio_file_record.file_name IS '文件名';
COMMENT ON COLUMN audio_file_record.file_path IS '文件存储路径';
COMMENT ON COLUMN audio_file_record.file_size IS '文件大小（字节）';
COMMENT ON COLUMN audio_file_record.file_type IS '文件类型（mp3/wav/pcm等）';
COMMENT ON COLUMN audio_file_record.voice_type IS '音色类型';
COMMENT ON COLUMN audio_file_record.text_content IS '合成文本内容';
COMMENT ON COLUMN audio_file_record.synthesis_type IS '合成类型（HTTP/ASYNC/STREAM）';
COMMENT ON COLUMN audio_file_record.status IS '状态（SUCCESS/FAILED/PROCESSING）';
COMMENT ON COLUMN audio_file_record.error_message IS '错误信息';
COMMENT ON COLUMN audio_file_record.create_time IS '创建时间';
COMMENT ON COLUMN audio_file_record.update_time IS '更新时间';
COMMENT ON COLUMN audio_file_record.created_by IS '创建人';
COMMENT ON COLUMN audio_file_record.updated_by IS '更新人';
