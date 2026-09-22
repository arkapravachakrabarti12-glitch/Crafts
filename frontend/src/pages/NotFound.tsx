import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center text-center">
      <h1 className="text-5xl font-bold text-brand-700">404</h1>
      <p className="mt-2 text-slate-600">This page doesn't exist.</p>
      <Link to="/" className="btn-primary mt-6">Go home</Link>
    </div>
  )
}
