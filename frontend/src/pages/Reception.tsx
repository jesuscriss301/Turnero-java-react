import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'

type Branch = { id: number; name: string }
type Service = { id: number; name: string; priorityAllowed: boolean; active: boolean }
type Ticket = { code: string; publicToken: string }

export default function Reception() {
  const [branches, setBranches] = useState<Branch[]>([])
  const [branchId, setBranchId] = useState<number | null>(null)
  const [services, setServices] = useState<Service[]>([])
  const [serviceId, setServiceId] = useState<number | null>(null)
  const [visitorName, setVisitorName] = useState('')
  const [priority, setPriority] = useState(false)
  const [issued, setIssued] = useState<Ticket | null>(null)
  const [err, setErr] = useState('')

  useEffect(() => {
    void api<Branch[]>('GET', '/api/branches').then(bs => {
      setBranches(bs)
      if (bs.length) setBranchId(bs[0].id)
    })
  }, [])

  useEffect(() => {
    if (branchId == null) return
    void api<Service[]>('GET', `/api/branches/${branchId}/services`).then(ss => {
      const active = ss.filter(s => s.active)
      setServices(active)
      setServiceId(active.length ? active[0].id : null)
    })
  }, [branchId])

  const selService = services.find(s => s.id === serviceId)

  async function issue() {
    if (!branchId || !serviceId) return
    setErr(''); setIssued(null)
    try {
      const t = await api<Ticket>('POST', `/api/branches/${branchId}/tickets`,
        { serviceId, visitorName, priority: priority && !!selService?.priorityAllowed })
      setIssued(t); setVisitorName(''); setPriority(false)
    } catch (e) { setErr((e as Error).message) }
  }

  const link = issued ? `${window.location.origin}/q/${issued.publicToken}` : ''

  return (
    <div>
      <div className="topbar"><strong>Recepción</strong><Link to="/admin">← Admin</Link></div>
      <div className="center" style={{ minHeight: 'calc(100vh - 54px)' }}>
        <div className="card" style={{ maxWidth: 520 }}>
          <h1>Emitir turno</h1>
          <label>Sucursal</label>
          <select value={branchId ?? ''} onChange={e => setBranchId(Number(e.target.value))}>
            {branches.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
          <label>Servicio</label>
          <select value={serviceId ?? ''} onChange={e => setServiceId(Number(e.target.value))}>
            {services.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
          <label>Nombre del cliente (opcional)</label>
          <input value={visitorName} onChange={e => setVisitorName(e.target.value)} />
          {selService?.priorityAllowed && (
            <label className="row" style={{ marginTop: 8 }}>
              <input type="checkbox" style={{ width: 'auto' }} checked={priority}
                     onChange={e => setPriority(e.target.checked)} /> Turno prioritario
            </label>
          )}
          {err && <div className="error">{err}</div>}
          <button className="big-btn" onClick={issue}>Emitir turno</button>
          {issued && (
            <div style={{ marginTop: 16, textAlign: 'center' }}>
              <div className="muted">Turno emitido</div>
              <div className="ticket-code">{issued.code}</div>
              <div className="muted">Seguimiento móvil:</div>
              <div style={{ wordBreak: 'break-all', fontSize: 13 }}>{link}</div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
