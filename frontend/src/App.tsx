import { Navigate, Route, Routes } from 'react-router-dom'
import { getSession } from './api'
import Login from './pages/Login'
import Register from './pages/Register'
import Admin from './pages/Admin'
import Reception from './pages/Reception'
import Operator from './pages/Operator'
import Display from './pages/Display'
import PublicTicket from './pages/PublicTicket'

function Private({ children }: { children: JSX.Element }) {
  return getSession() ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to={getSession() ? '/admin' : '/login'} replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/admin" element={<Private><Admin /></Private>} />
      <Route path="/reception" element={<Private><Reception /></Private>} />
      <Route path="/operator" element={<Private><Operator /></Private>} />
      <Route path="/display/:branchId" element={<Display />} />
      <Route path="/q/:token" element={<PublicTicket />} />
    </Routes>
  )
}
