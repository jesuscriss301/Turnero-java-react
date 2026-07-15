export type Session = {
  token: string
  user: { id: number; name: string; email: string; role: string }
  tenant: { id: number; name: string; plan: string; freeExpiresAt: string }
}

export function getSession(): Session | null {
  const raw = localStorage.getItem('session')
  return raw ? (JSON.parse(raw) as Session) : null
}

export function setSession(s: Session | null) {
  if (s) localStorage.setItem('session', JSON.stringify(s))
  else localStorage.removeItem('session')
}

export async function api<T = unknown>(method: string, url: string, body?: unknown): Promise<T> {
  const s = getSession()
  try {
    const res = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(s ? { Authorization: `Bearer ${s.token}` } : {})
      },
      body: body !== undefined ? JSON.stringify(body) : undefined
    })

    if (res.status === 204) return null as T

    if (!res.ok) {
      if (res.status === 401) {
        setSession(null)
        window.location.href = '/login'
      }
      // Try to parse error body for a message
      const text = await res.text().catch(() => '')
      let serverMsg = ''
      try {
        const parsed = text ? JSON.parse(text) : null
        serverMsg = parsed && (parsed.message || parsed.error) ? (parsed.message || parsed.error) : ''
      } catch (e) {
        serverMsg = text
      }
      const msg = serverMsg || `Error ${res.status} ${res.statusText}`
      throw new Error(msg)
    }

    const data = await res.json().catch(() => ({}))
    return data as T
  } catch (e) {
    // Network or unexpected error
    const errMsg = e instanceof Error ? e.message : String(e)
    throw new Error(`No se pudo conectar con el backend: ${errMsg}`)
  }
}
