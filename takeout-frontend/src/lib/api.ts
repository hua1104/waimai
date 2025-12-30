const DEFAULT_API_BASE = 'http://localhost:8081'

export const API_BASE: string = String((import.meta as any)?.env?.VITE_API_BASE ?? DEFAULT_API_BASE)

export function api(path: string): string {
  const safePath = path.startsWith('/') ? path : `/${path}`
  return new URL(safePath, API_BASE).toString()
}

