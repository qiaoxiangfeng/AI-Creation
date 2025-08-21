const codeToI18nKey: Record<string, string> = {
  '00000000': 'common.success',
  CM01: 'error.system',
  CM02: 'error.param',
  CM03: 'user.notFound',
  CM04: 'user.usernameOrPasswordError',
  CM05: 'user.alreadyExists',
  CM06: 'auth.noPermission',
  CM07: 'auth.tokenInvalid',
  CM08: 'auth.tokenExpired',
};

export function mapErrorCodeToMessage(code: string): string {
  const key = codeToI18nKey[code] ?? 'error.unknown';
  // 这里可接入真实 i18n 实现
  return key;
}


