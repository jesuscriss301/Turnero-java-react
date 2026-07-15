import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, setSession, Session } from '../api'

export default function Login() {
  const nav = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit() {
    setErr(''); setBusy(true)
    try {
      const s = await api<Session>('POST', '/api/auth/login', { email, password })
      setSession(s)
      nav('/admin')
    } catch (e) { setErr((e as Error).message) } finally { setBusy(false) }
  }

  return (
    <div className="center">
      <div className="card">
        <h1>Turnero SaaS — Iniciar sesión</h1>
        <label>Email</label>
        <input value={email} onChange={e => setEmail(e.target.value)} type="email" />
        <label>Contraseña</label>
        <input value={password} onChange={e => setPassword(e.target.value)} type="password"
               onKeyDown={e => e.key === 'Enter' && submit()} />
        {err && <div className="error">{err}</div>}
        <button onClick={submit} disabled={busy}>Entrar</button>
        <p className="muted" style={{ marginTop: 12 }}>
          ¿Sin cuenta? <Link to="/register" style={{ color: 'var(--accent)' }}>Registra tu empresa</Link>
        </p>
      </div>
    </div>
  )
}
