import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import Toaster from '../common/Toaster'

export default function Layout() {
  return (
    <div className="app-shell h-screen flex flex-col">
      <Navbar />
      <main className="flex-1 flex overflow-hidden relative">
        <Outlet />
      </main>
      <Toaster />
    </div>
  )
}
