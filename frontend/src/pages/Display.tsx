import { useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'

type Called = { code: string; point: string; status: string }
type Waiting = { code: string; service: string; priority: boolean }
type Snapshot = { plan: string; tenantName: string; called: Called[]; waiting: Waiting[] }

export default function Display() {
  const { branchId } = useParams()
  const [params] = useSearchParams()
  const key = params.get('key') ?? ''
  const [snap, setSnap] = useState<Snapshot | null>(null)
  const [err, setErr] = useState('')
  const esRef = useRef<EventSource | null>(null)

  useEffect(() => {
    let cancelled = false
    async function init() {
      try {
        const res = await fetch(`/api/public/display/${branchId}?key=${encodeURIComponent(key)}`)
        if (!res.ok) { setErr('Pantalla no autorizada o sucursal inexistente'); return }
        const data = (await res.json()) as Snapshot
        if (!cancelled) setSnap(data)
        const es = new EventSource(`/api/stream/display/${branchId}?key=${encodeURIComponent(key)}`)
        es.addEventListener('queue', ev => {
          setSnap(JSON.parse((ev as MessageEvent).data) as Snapshot)
        })
        es.onerror = () => { es.close(); setTimeout(init, 3000) }
        esRef.current = es
      } catch { setErr('Error de conexión') }
    }
    void init()
    return () => { cancelled = true; esRef.current?.close() }
  }, [branchId, key])

  if (err) return <div className="center"><div className="card">{err}</div></div>
  if (!snap) return <div className="center"><div className="card">Cargando pantalla…</div></div>

  const last = snap.called[0]
  const isFree = snap.plan === 'FREE'

  return (
    <div className="display">
      <div className="display-main">
        <div className="muted" style={{ fontSize: 20 }}>{snap.tenantName}</div>
        {last ? <>
          <div className="called-big">{last.code}</div>
          <div className="called-point">→ {last.point}</div>
        </> : <div className="called-big" style={{ color: 'var(--muted)' }}>—</div>}
        <div className="called-list">
          {snap.called.slice(1, 5).map((c, i) => (
            <div key={i} className="item"><span>{c.code}</span><span className="muted">{c.point}</span></div>
          ))}
        </div>
        <div className="waiting-strip">
          {snap.waiting.slice(0, 12).map((w, i) => (
            <span key={i} className={`waiting-chip ${w.priority ? 'prio' : ''}`}>{w.code}</span>
          ))}
        </div>
      </div>
      {isFree ? (
        <div className="display-ads">
          <div style={{ fontSize: 28, fontWeight: 700 }}>Espacio publicitario</div>
          <p className="muted" style={{ marginTop: 10 }}>
            Plan Free — este espacio (40% de pantalla) muestra publicidad.<br />
            Los planes pagos lo reemplazan por su branding corporativo.
          </p>
        </div>
      ) : (
        <div className="display-ads" style={{ width: '25%' }}>
          <div style={{ fontSize: 32, fontWeight: 800 }}>{snap.tenantName}</div>
        </div>
      )}
    </div>
  )
}
