import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'

type Branch = { id: number; name: string }
type Point = { id: number; name: string; serviceIds: number[] }
type Ticket = { id: number; code: string; status: string; visitorName: string; priority: boolean }

export default function Operator() {
  const [branches, setBranches] = useState<Branch[]>([])
  const [branchId, setBranchId] = useState<number | null>(null)
  const [points, setPoints] = useState<Point[]>([])
  const [pointId, setPointId] = useState<number | null>(null)
  const [current, setCurrent] = useState<Ticket | null>(null)
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    void api<Branch[]>('GET', '/api/branches').then(bs => {
      setBranches(bs)
      if (bs.length) setBranchId(bs[0].id)
    })
  }, [])

  useEffect(() => {
    if (branchId == null) return
    void api<Point[]>('GET', `/api/branches/${branchId}/points`).then(ps => {
      setPoints(ps)
      setPointId(ps.length ? ps[0].id : null)
    })
  }, [branchId])

  async function next() {
    if (!pointId) return
    setErr(''); setMsg(''); setBusy(true)
    try {
      const t = await api<Ticket | null>('POST', `/api/points/${pointId}/next`)
      if (t) setCurrent(t)
      else { setCurrent(null); setMsg('No hay turnos en espera compatibles con este punto.') }
    } catch (e) { setErr((e as Error).message) } finally { setBusy(false) }
  }

  async function action(a: string) {
    if (!current) return
    setErr(''); setBusy(true)
    try {
      const t = await api<Ticket>('POST', `/api/tickets/${current.id}/${a}`)
      if (t.status === 'FINISHED' || t.status === 'ABSENT') setCurrent(null)
      else setCurrent(t)
    } catch (e) { setErr((e as Error).message) } finally { setBusy(false) }
  }

  return (
    <div>
      <div className="topbar"><strong>Panel de operador</strong><Link to="/admin">← Admin</Link></div>
      <div className="center" style={{ minHeight: 'calc(100vh - 54px)' }}>
        <div className="card" style={{ maxWidth: 520 }}>
          <label>Sucursal</label>
          <select value={branchId ?? ''} onChange={e => setBranchId(Number(e.target.value))}>
            {branches.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
          <label>Punto de atención</label>
          <select value={pointId ?? ''} onChange={e => setPointId(Number(e.target.value))}>
            {points.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>

          {!current && <button className="big-btn" onClick={next} disabled={busy || !pointId}>Siguiente ▶</button>}
          {msg && <p className="muted" style={{ marginTop: 10 }}>{msg}</p>}
          {err && <div className="error">{err}</div>}

          {current && (
            <div style={{ marginTop: 16 }}>
              <div className="muted">Atendiendo {current.priority && '· PRIORITARIO'}</div>
              <div className="ticket-code">{current.code}</div>
              {current.visitorName && <p style={{ textAlign: 'center' }}>{current.visitorName}</p>}
              <p className="muted" style={{ textAlign: 'center' }}>Estado: {current.status}</p>
              <div className="row" style={{ marginTop: 10 }}>
                {current.status === 'CALLED' && <>
                  <button onClick={() => void action('start')} disabled={busy}>Iniciar atención</button>
                  <button className="secondary" onClick={() => void action('recall')} disabled={busy}>Re-llamar</button>
                  <button className="danger" onClick={() => void action('absent')} disabled={busy}>Ausente</button>
                </>}
                {current.status === 'IN_SERVICE' &&
                  <button onClick={() => void action('finish')} disabled={busy}>Finalizar</button>}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
