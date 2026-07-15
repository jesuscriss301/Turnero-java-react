import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, getSession, setSession } from '../api'

type Branch = { id: number; name: string; displayKey: string }
type Service = { id: number; name: string; prefix: string; priorityAllowed: boolean; active: boolean }
type Point = { id: number; name: string; status: string; serviceIds: number[] }

export default function Admin() {
  const nav = useNavigate()
  const session = getSession()!
  const [branches, setBranches] = useState<Branch[]>([])
  const [sel, setSel] = useState<Branch | null>(null)
  const [services, setServices] = useState<Service[]>([])
  const [points, setPoints] = useState<Point[]>([])
  const [err, setErr] = useState('')

  const [branchName, setBranchName] = useState('')
  const [svcName, setSvcName] = useState('')
  const [svcPrefix, setSvcPrefix] = useState('')
  const [svcPrio, setSvcPrio] = useState(false)
  const [ptName, setPtName] = useState('')
  const [ptServices, setPtServices] = useState<number[]>([])

  useEffect(() => { void loadBranches() }, [])

  async function loadBranches() {
    try {
      const bs = await api<Branch[]>('GET', '/api/branches')
      setBranches(bs)
      if (bs.length && !sel) void selectBranch(bs[0])
    } catch (e) { setErr((e as Error).message) }
  }

  async function selectBranch(b: Branch) {
    setSel(b)
    const [ss, ps] = await Promise.all([
      api<Service[]>('GET', `/api/branches/${b.id}/services`),
      api<Point[]>('GET', `/api/branches/${b.id}/points`)
    ])
    setServices(ss); setPoints(ps)
  }

  async function createBranch() {
    setErr('')
    try {
      await api('POST', '/api/branches', { name: branchName })
      setBranchName(''); await loadBranches()
    } catch (e) { setErr((e as Error).message) }
  }

  async function createService() {
    if (!sel) return
    setErr('')
    try {
      await api('POST', `/api/branches/${sel.id}/services`, { name: svcName, prefix: svcPrefix, priorityAllowed: svcPrio })
      setSvcName(''); setSvcPrefix(''); setSvcPrio(false)
      await selectBranch(sel)
    } catch (e) { setErr((e as Error).message) }
  }

  async function createPoint() {
    if (!sel) return
    setErr('')
    try {
      await api('POST', `/api/branches/${sel.id}/points`, { name: ptName, serviceIds: ptServices })
      setPtName(''); setPtServices([])
      await selectBranch(sel)
    } catch (e) { setErr((e as Error).message) }
  }

  function togglePtService(id: number) {
    setPtServices(p => p.includes(id) ? p.filter(x => x !== id) : [...p, id])
  }

  function logout() { setSession(null); nav('/login') }

  return (
    <div>
      <div className="topbar">
        <div><strong>{session.tenant.name}</strong> <span className="tag">{session.tenant.plan}</span>
          {session.tenant.plan === 'FREE' && <span className="muted"> vence {session.tenant.freeExpiresAt}</span>}
        </div>
        <div className="row">
          <Link to="/reception">Recepción</Link>
          <Link to="/operator">Operador</Link>
          <button className="secondary" onClick={logout}>Salir</button>
        </div>
      </div>
      <div className="page">
        {err && <div className="error">{err}</div>}
        <div className="grid">
          <div className="panel">
            <h2>Sucursales</h2>
            {branches.map(b => (
              <div key={b.id} className="item" style={{ cursor: 'pointer', outline: sel?.id === b.id ? '1px solid var(--accent)' : 'none' }}
                   onClick={() => void selectBranch(b)}>
                <span>{b.name}</span>
                <a className="tag ok" href={`/display/${b.id}?key=${b.displayKey}`} target="_blank" rel="noreferrer"
                   onClick={e => e.stopPropagation()}>Pantalla ↗</a>
              </div>
            ))}
            <label>Nueva sucursal</label>
            <input value={branchName} onChange={e => setBranchName(e.target.value)} placeholder="Sede Centro" />
            <button onClick={createBranch}>Crear sucursal</button>
          </div>

          <div className="panel">
            <h2>Servicios {sel && <span className="muted">· {sel.name}</span>}</h2>
            {services.map(s => (
              <div key={s.id} className="item">
                <span>{s.name} <span className="tag">{s.prefix}</span>{s.priorityAllowed && <span className="tag warn">prioridad</span>}</span>
                <span className={`tag ${s.active ? 'ok' : ''}`}>{s.active ? 'activo' : 'inactivo'}</span>
              </div>
            ))}
            {sel && <>
              <label>Nombre</label>
              <input value={svcName} onChange={e => setSvcName(e.target.value)} placeholder="Pagos" />
              <label>Prefijo (1-3 letras)</label>
              <input value={svcPrefix} onChange={e => setSvcPrefix(e.target.value)} placeholder="P" maxLength={3} />
              <label className="row" style={{ marginTop: 8 }}>
                <input type="checkbox" style={{ width: 'auto' }} checked={svcPrio} onChange={e => setSvcPrio(e.target.checked)} />
                Admite prioridad
              </label>
              <button onClick={createService}>Crear servicio</button>
            </>}
          </div>

          <div className="panel">
            <h2>Puntos de atención {sel && <span className="muted">· {sel.name}</span>}</h2>
            {points.map(p => (
              <div key={p.id} className="item">
                <span>{p.name}</span>
                <span className="muted">{p.serviceIds.length} servicio(s)</span>
              </div>
            ))}
            {sel && <>
              <label>Nombre</label>
              <input value={ptName} onChange={e => setPtName(e.target.value)} placeholder="Caja 1" />
              <label>Servicios compatibles</label>
              {services.map(s => (
                <label key={s.id} className="row" style={{ margin: '4px 0' }}>
                  <input type="checkbox" style={{ width: 'auto' }}
                         checked={ptServices.includes(s.id)} onChange={() => togglePtService(s.id)} />
                  {s.name}
                </label>
              ))}
              <button onClick={createPoint}>Crear punto</button>
            </>}
          </div>
        </div>
      </div>
    </div>
  )
}
