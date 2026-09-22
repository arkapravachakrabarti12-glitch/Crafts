import { Link } from 'react-router-dom'
import { Award, Briefcase, School, Users } from 'lucide-react'
import { Logo } from '../components/Layout'

const FEATURES = [
  {
    icon: <Award className="h-6 w-6" />,
    title: 'A teaching portfolio, not just a CV',
    text: 'Show subjects, boards and grades you teach, demo lessons, lesson plans and student results.',
  },
  {
    icon: <Users className="h-6 w-6" />,
    title: 'Connect with educators',
    text: 'Build your network, share classroom ideas and learn from teachers across the country.',
  },
  {
    icon: <Briefcase className="h-6 w-6" />,
    title: 'Find the right teaching job',
    text: 'Full-time, part-time, substitute and tutoring roles, matched to your subjects and city.',
  },
  {
    icon: <School className="h-6 w-6" />,
    title: 'Hire great teachers',
    text: 'Schools and coaching centres can post openings, search verified teachers and manage applicants.',
  },
]

export default function Landing() {
  return (
    <div className="min-h-screen bg-white">
      <header className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Logo />
        <div className="flex items-center gap-2">
          <Link to="/jobs" className="btn-ghost hidden sm:inline-flex">
            Browse jobs
          </Link>
          <Link to="/register" className="btn-ghost">
            Join now
          </Link>
          <Link to="/login" className="btn-outline">
            Sign in
          </Link>
        </div>
      </header>

      <section className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-12 md:grid-cols-2 md:py-20">
        <div>
          <h1 className="text-4xl font-light leading-tight text-brand-800 md:text-5xl">
            The professional community for <span className="font-semibold">teachers</span>
          </h1>
          <p className="mt-5 text-lg text-slate-600">
            Build your teaching profile, connect with fellow educators, and discover schools that are looking for someone
            exactly like you.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/register?type=TEACHER" className="btn-primary px-6 py-3 text-base">
              I'm a teacher
            </Link>
            <Link to="/register?type=INSTITUTION" className="btn-outline px-6 py-3 text-base">
              I'm hiring teachers
            </Link>
          </div>
        </div>
        <div className="relative hidden md:block">
          <div className="absolute -inset-4 rounded-3xl bg-gradient-to-br from-brand-100 via-white to-emerald-50" />
          <div className="relative space-y-3 p-6">
            {[
              ['Priya Sharma', 'Physics · CBSE · Class 11–12', 'bg-indigo-500'],
              ['Ananya Iyer', 'English · IB DP', 'bg-emerald-500'],
              ['Rahul Verma', 'Mathematics · Olympiad coach', 'bg-amber-500'],
            ].map(([name, line, color]) => (
              <div key={name} className="card flex items-center gap-3 p-4">
                <div className={`flex h-12 w-12 items-center justify-center rounded-full ${color} font-semibold text-white`}>
                  {name.split(' ').map((w) => w[0]).join('')}
                </div>
                <div>
                  <div className="font-semibold">{name}</div>
                  <div className="text-sm text-slate-500">{line}</div>
                </div>
                <span className="ml-auto rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">
                  Open to work
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-slate-50 py-14">
        <div className="mx-auto grid max-w-6xl gap-5 px-4 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((f) => (
            <div key={f.title} className="card p-5">
              <div className="mb-3 inline-flex rounded-lg bg-brand-50 p-2 text-brand-700">{f.icon}</div>
              <h3 className="font-semibold">{f.title}</h3>
              <p className="mt-1 text-sm text-slate-600">{f.text}</p>
            </div>
          ))}
        </div>
      </section>

      <footer className="py-8 text-center text-sm text-slate-500">© {new Date().getFullYear()} TeachNet</footer>
    </div>
  )
}
