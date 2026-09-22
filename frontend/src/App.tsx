import type { ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { Role } from './api/types'
import { useAuth } from './auth/AuthContext'
import Layout from './components/Layout'
import { PageLoader } from './components/ui'
import Admin from './pages/Admin'
import Applicants from './pages/Applicants'
import Feed from './pages/Feed'
import InstitutionEdit from './pages/InstitutionEdit'
import InstitutionPage from './pages/InstitutionPage'
import JobDetail from './pages/JobDetail'
import JobForm from './pages/JobForm'
import Jobs from './pages/Jobs'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Messages from './pages/Messages'
import MyApplications from './pages/MyApplications'
import MyJobs from './pages/MyJobs'
import Network from './pages/Network'
import NotFound from './pages/NotFound'
import Notifications from './pages/Notifications'
import PostPage from './pages/PostPage'
import Profile from './pages/Profile'
import Register from './pages/Register'
import Search from './pages/Search'

function RequireAuth({ children, roles }: { children: ReactNode; roles?: Role[] }) {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  if (roles && !roles.includes(user.role)) return <Navigate to="/feed" replace />
  return <>{children}</>
}

function GuestOnly({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  return user ? <Navigate to="/feed" replace /> : <>{children}</>
}

export default function App() {
  const { user, loading } = useAuth()
  if (loading) return <PageLoader />

  return (
    <Routes>
      <Route path="/" element={user ? <Navigate to="/feed" replace /> : <Landing />} />
      <Route path="/login" element={<GuestOnly><Login /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><Register /></GuestOnly>} />

      {/* Job pages are public, so logged-out visitors (and search engines) can browse them. */}
      {!user && <Route path="/jobs" element={<Jobs />} />}
      {!user && <Route path="/jobs/:id" element={<JobDetail />} />}

      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route path="/feed" element={<Feed />} />
        <Route path="/posts/:id" element={<PostPage />} />
        <Route path="/profile/:userId" element={<Profile />} />
        <Route path="/network" element={<RequireAuth roles={['TEACHER']}><Network /></RequireAuth>} />
        <Route path="/teachers" element={<Search />} />
        <Route path="/institutions/:id" element={<InstitutionPage />} />
        <Route path="/institution/edit" element={<RequireAuth roles={['INSTITUTION']}><InstitutionEdit /></RequireAuth>} />
        <Route path="/jobs" element={<Jobs />} />
        <Route path="/jobs/new" element={<RequireAuth roles={['INSTITUTION']}><JobForm /></RequireAuth>} />
        <Route path="/jobs/:id" element={<JobDetail />} />
        <Route path="/jobs/:id/edit" element={<RequireAuth roles={['INSTITUTION']}><JobForm /></RequireAuth>} />
        <Route path="/jobs/:id/applicants" element={<RequireAuth roles={['INSTITUTION']}><Applicants /></RequireAuth>} />
        <Route path="/my-jobs" element={<RequireAuth roles={['INSTITUTION']}><MyJobs /></RequireAuth>} />
        <Route path="/applications" element={<RequireAuth roles={['TEACHER']}><MyApplications /></RequireAuth>} />
        <Route path="/messages" element={<Messages />} />
        <Route path="/messages/:conversationId" element={<Messages />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/admin" element={<RequireAuth roles={['ADMIN']}><Admin /></RequireAuth>} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
