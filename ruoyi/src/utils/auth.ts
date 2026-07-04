import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token'
const RefreshTokenKey = 'Admin-Refresh-Token'

export function getToken(): string | undefined {
  return Cookies.get(TokenKey)
}

export function setToken(token: string): string | undefined {
  return Cookies.set(TokenKey, token)
}

export function removeToken(): void {
  Cookies.remove(TokenKey)
}

/** 获取 refreshToken */
export function getRefreshToken(): string | undefined {
  return Cookies.get(RefreshTokenKey)
}

/** 设置 refreshToken */
export function setRefreshToken(token: string): string | undefined {
  return Cookies.set(RefreshTokenKey, token)
}

/** 移除 refreshToken */
export function removeRefreshToken(): void {
  Cookies.remove(RefreshTokenKey)
}
