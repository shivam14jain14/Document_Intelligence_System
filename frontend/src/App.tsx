import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import UploadPage from './pages/UploadPage'
import ChatPage from './pages/ChatPage'
import BrowsePage from './pages/BrowsePage'
import HistoryPage from './pages/HistoryPage'
import AdminPage from './pages/AdminPage'
import OnboardingPage from './pages/OnboardingPage'
import Layout from './components/layout/Layout'

function getDefaultRoute(user: { role: string; onboardingCompleted?: boolean } | null) {
  if (!user) return '/login'
  if (!user.onboardingCompleted) return '/onboarding'
  return user.role === 'ADMIN' ? '/admin' : '/upload'
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((s) => s.token)
  const user = useAuthStore((s) => s.user)
  return token && user ? <>{children}</> : <Navigate to="/login" replace />
}

function HomeRoute() {
  const token = useAuthStore((s) => s.token)
  const user = useAuthStore((s) => s.user)
  return token && user ? <Navigate to={getDefaultRoute(user)} replace /> : <LandingPage />
}

function LoginRoute() {
  const token = useAuthStore((s) => s.token)
  const user = useAuthStore((s) => s.user)
  return token && user ? <Navigate to={getDefaultRoute(user)} replace /> : <LoginPage />
}

function AppGate({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user)
  const location = useLocation()
  const needsOnboarding = !!user && !user.onboardingCompleted
  const isOnboardingRoute = location.pathname === '/onboarding'

  if (needsOnboarding && !isOnboardingRoute) {
    return <Navigate to="/onboarding" replace />
  }

  return <>{children}</>
}

function OnboardingProtectedRoute({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user)
  const needsOnboarding = !!user && !user.onboardingCompleted
  return needsOnboarding ? <Navigate to="/onboarding" replace /> : <>{children}</>
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user)
  return user?.role === 'ADMIN' ? <>{children}</> : <Navigate to="/upload" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/" element={<HomeRoute />} />
        <Route path="/login" element={<LoginRoute />} />

        {/* App (auth required) — Layout provides the Navbar + Outlet */}
        <Route element={<ProtectedRoute><AppGate><Layout /></AppGate></ProtectedRoute>}>
          <Route path="/onboarding" element={<OnboardingPage />} />
          <Route path="/upload" element={<OnboardingProtectedRoute><UploadPage /></OnboardingProtectedRoute>} />
          <Route path="/browse" element={<OnboardingProtectedRoute><BrowsePage /></OnboardingProtectedRoute>} />
          <Route path="/chat" element={<OnboardingProtectedRoute><ChatPage /></OnboardingProtectedRoute>} />
          <Route path="/history" element={<OnboardingProtectedRoute><HistoryPage /></OnboardingProtectedRoute>} />
          <Route path="/admin" element={<OnboardingProtectedRoute><AdminRoute><AdminPage /></AdminRoute></OnboardingProtectedRoute>} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
