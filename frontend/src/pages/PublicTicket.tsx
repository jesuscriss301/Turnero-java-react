import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'

type Status = { code: string; status: string; ahead?: number; point?: string; priority: boolean }

const LABELS: Record<string, string> = {
  WAITING: 'En espera', CALLED: '¡Es tu turno!', IN_SERVICE: 'En atención',
  FINISHED: 'Atención finalizada', ABSENT: 'Marcado como ausente', CANCELLED: 'Cancelado'
}

export default function PublicTicket() {
  const { token } = useParams()
  const [st, setSt] = useState<Status | null>(null)
  const [err, setErr] = useState('')
  const esRef = useRef<EventSource | null>(null)

  useEffect(() => {
    let cancelled = false
    async function init() {
      try {
        const res = await fetch(`/api/public/ticket/${token}`)
        if (!res.ok) { setErr('Turno no encontrado'); return }
        const data = (await res.json()) as Status
        if (!cancelled) setSt(data)
        if (['FINISHED', 'ABSENT', 'CANCELLED'].includes(data.status)) return
        const es = new EventSource(`/api/stream/ticket/${token}`)
        es.addEventListener('status', ev => {
          const s = JSON.parse((ev as MessageEvent).data) as Status
          setSt(s)
          if (['FINISHED', 'ABSENT', 'CANCELLED'].includes(s.status)) es.close()
        })
        es.onerror = () => { es.close(); setTimeout(init, 4000) }
        esRef.current = es
      } catch { setErr('Error de conexión') }
    }
    void init()
    return () => { cancelled = true; esRef.current?.close() }
  }, [token])

  if (err) return <div className="center"><div className="card">{err}</div></div>
  if (!st) return <div className="center"><div className="card">Cargando…</div></div>

  const called = st.status === 'CALLED'

  return (
    <div className="center">
      <div className="card" style={{ textAlign: 'center' }}>
        <div className="muted">Tu turno {st.priority && '· prioritario'}</div>
        <div className="ticket-code" style={{ color: called ? 'var(--ok)' : 'var(--text)' }}>{st.code}</div>
        <h1 style={{ color: called ? 'var(--ok)' : 'var(--text)' }}>{LABELS[st.status] ?? st.status}</h1>
        {st.status === 'WAITING' && st.ahead !== undefined && (
          <p style={{ marginTop: 10, fontSize: 18 }}>
            Personas delante de ti: <strong>{st.ahead}</strong>
          </p>
        )}
        {called && st.point && (
          <p style={{ marginTop: 10, fontSize: 22 }}>Dirígete a: <strong>{st.point}</strong></p>
        )}
        <div style={{ marginTop: 20, borderTop: '1px solid #334155', paddingTop: 12 }}>
          <p className="muted" style={{ fontSize: 12 }}>Espacio publicitario (plan Free)</p>
        </div>
      </div>
    </div>
  )
}
