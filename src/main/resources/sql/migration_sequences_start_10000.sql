-- =====================================================================
-- 将所有 BIGSERIAL 主键序列的“下一个值”对齐为：
--   max(表中已有最大 id, 9999) 之后（即新插入 id >= 10000，且不会与已有 id 冲突）
--
-- 背景：若初始化脚本用显式 id 插入数据，序列可能仍停留在较小值，
--       后续 INSERT 不显式指定 id 时会与已有主键冲突（如 dictionary_pkey）。
--
-- 用法：在已有库上执行一次即可（可重复执行）。
-- =====================================================================

DO $$
DECLARE
  tbl text;
BEGIN
  FOREACH tbl IN ARRAY ARRAY[
    'article',
    'article_chapter',
    'article_generation_config',
    'dictionary',
    'plot',
    'user_info',
    'audio_file_record'
  ]
  LOOP
    EXECUTE format(
      'SELECT setval(pg_get_serial_sequence(%L, ''id''), GREATEST(COALESCE((SELECT MAX(id) FROM %I), 0), 9999), true)',
      tbl,
      tbl
    );
  END LOOP;
END $$;
