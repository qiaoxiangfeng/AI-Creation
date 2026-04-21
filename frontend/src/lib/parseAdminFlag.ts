/** 统一字段 isAdmin，兼容布尔与字符串 */
export function parseAdminFlag(payload: Record<string, unknown>): boolean {
  const v = payload.isAdmin;
  if (v === true || v === 1) return true;
  if (typeof v === 'string') {
    const s = v.trim().toLowerCase();
    return s === 'true' || s === '1' || s === 'yes';
  }
  return false;
}
