import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, setSession, Session } from '../api'

export default function Register() {
  const nav = useNavigate()
  const [tenantName, setTenantName] = useState('')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit() {
    setErr(''); setBusy(true)
    try {
      const s = await api<Session>('POST', '/api/auth/register', { tenantName, name, email, password })
      setSession(s)
      nav('/admin')
    } catch (e) { setErr((e as Error).message) } finally { setBusy(false) }
  }

  return (
    <div className="center">
      <div className="card">
        <h1>Registra tu empresa</h1>
        <p className="muted">Plan Free: 1 sucursal, 3 meses, con publicidad.</p>
        <label>Nombre de la empresa</label>
        <input value={tenantName} onChange={e => setTenantName(e.target.value)} />
        <label>Tu nombre</label>
        <input value={name} onChange={e => setName(e.target.value)} />
        <label>Email</label>
        <input value={email} onChange={e => setEmail(e.target.value)} type="email" />
        <label>Contraseña (mín. 8)</label>
        <input value={password} onChange={e => setPassword(e.target.value)} type="password" />
        {err && <div className="error">{err}</div>}
        <button onClick={submit} disabled={busy}>Crear cuenta</button>
        <p className="muted" style={{ marginTop: 12 }}>
          ¿Ya tienes cuenta? <Link to="/login" style={{ color: 'var(--accent)' }}>Inicia sesión</Link>
        </p>
      </div>
    </div>
  )
}
