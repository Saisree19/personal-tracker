import type { FormEvent } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { PieChart, Pie, Cell, Tooltip } from 'recharts'
import './App.css'
import logo from './assets/image.png'

type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'CLOSED'
type TaskComplexity = 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH'

type TaskNoteResponse = {
  id: string
  content: string
  authorId: string
  createdAt: string
}

type TaskResponse = {
  id: string
  title: string
  description: string
  application: string
  complexity: TaskComplexity
  deadlineDate: string
  status: TaskStatus
  createdAt: string
  updatedAt: string
  closedAt?: string
  startedAt?: string
  archivedAt?: string
  notes: TaskNoteResponse[]
}

type ReportResponse = {
  applicationSummaries: { application: string; completedCount: number }[]
  complexityDistribution: {
    application: string
    complexity: TaskComplexity
    completedCount: number
  }[]
  productivityTrend: { periodStart: string; completedCount: number }[]
  statusDistribution: { status: TaskStatus; count: number }[]
}

type BasicResponse = {
  message: string
  resetToken?: string
  otp?: string
  resetUrl?: string
}

type AuthMode = 'login' | 'signup' | 'forgot' | 'reset'
type Theme = 'dark' | 'light'

const AUTH_URL = import.meta.env.VITE_AUTH_URL ?? 'http://localhost:8081'
const TASK_URL = import.meta.env.VITE_TASK_URL ?? 'http://localhost:8082'
const REPORT_URL = import.meta.env.VITE_REPORT_URL ?? 'http://localhost:8083'

const complexityPalette: Record<TaskComplexity, string> = {
  LOW: '#80E0A7',
  MEDIUM: '#F4D35E',
  HIGH: '#F28F3B',
  VERY_HIGH: '#E85F5C',
}

const statusPalette: Record<TaskStatus, string> = {
  OPEN: '#7C8CE1',
  IN_PROGRESS: '#F2C14E',
  CLOSED: '#80E0A7',
}

const accentColors = ['#7C8CE1', '#4AC6B7', '#E86F9E', '#F2C14E', '#6A4C93']
const TASKS_PER_PAGE = 10
const complexityOrder: TaskComplexity[] = ['VERY_HIGH', 'HIGH', 'MEDIUM', 'LOW']

const toDateInputValue = (value?: string | null) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toISOString().slice(0, 10)
}

const formatDisplayDate = (value?: string | null) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-GB')
}

type SortField = 'COMPLETION_DATE' | 'COMPLEXITY'
type SortDirection = 'ASC' | 'DESC'
type TimeWindow = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY'

function App() {
  const [token, setToken] = useState<string>('')
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [theme, setTheme] = useState<Theme>(() => (localStorage.getItem('tracker_theme') === 'light' ? 'light' : 'dark'))
  const [loginForm, setLoginForm] = useState({ username: '', password: '' })
  const [signupForm, setSignupForm] = useState({ username: '', email: '', password: '' })
  const [forgotForm, setForgotForm] = useState({ email: '' })
  const [resetForm, setResetForm] = useState({ email: '', token: '', otp: '', newPassword: '', confirmPassword: '' })
  const [showResetPassword, setShowResetPassword] = useState(false)
  const [authError, setAuthError] = useState<string>('')
  const [authInfo, setAuthInfo] = useState<string>('')
  const [otpTimer, setOtpTimer] = useState(0)
  const [isResending, setIsResending] = useState(false)
  const [tasks, setTasks] = useState<TaskResponse[]>([])
  const [taskPage, setTaskPage] = useState(1)
  const [taskError, setTaskError] = useState<string>('')
  const [taskLoading, setTaskLoading] = useState(false)
  const [expandedTaskId, setExpandedTaskId] = useState<string | null>(null)
  const [editingTaskId, setEditingTaskId] = useState<string | null>(null)
  const [editForm, setEditForm] = useState({ title: '', description: '', application: '', complexity: 'MEDIUM' as TaskComplexity })
  const [sortField, setSortField] = useState<'due' | 'complexity'>('due')
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc')
  const [includeArchived, setIncludeArchived] = useState(false)
  const [taskForm, setTaskForm] = useState({
    title: '',
    description: '',
    application: '',
    complexity: 'MEDIUM' as TaskComplexity,
    deadlineDate: '',
  })
  const [reportFilter, setReportFilter] = useState({
    window: 'MONTHLY' as TimeWindow,
    application: '',
    complexity: '' as '' | TaskComplexity,
    sortField: 'COMPLETION_DATE' as SortField,
    sortDirection: 'DESC' as SortDirection,
  })
  const [report, setReport] = useState<ReportResponse | null>(null)
  const [reportError, setReportError] = useState<string>('')
  const [reportLoading, setReportLoading] = useState(false)

  const isAuthenticated = Boolean(token)

  useEffect(() => {
    const saved = localStorage.getItem('tracker_token')
    if (saved) {
      setToken(saved)
    }
  }, [])

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('tracker_theme', theme)
  }, [theme])

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const tokenParam = params.get('token')
    const emailParam = params.get('email')
    if (tokenParam || emailParam) {
      setAuthMode('reset')
      setResetForm((prev) => ({
        ...prev,
        token: tokenParam ?? prev.token,
        email: emailParam ?? prev.email,
      }))
      setAuthInfo('Enter the OTP from your email to reset your password.')
      const url = new URL(window.location.href)
      url.search = ''
      window.history.replaceState(null, '', url.toString())
    }
  }, [])

  const formattedOtpTimer = useMemo(() => {
    const safeSeconds = Math.max(0, otpTimer)
    const minutes = Math.floor(safeSeconds / 60)
    const seconds = safeSeconds % 60
    const paddedMinutes = minutes.toString().padStart(2, '0')
    const paddedSeconds = seconds.toString().padStart(2, '0')
    return `${paddedMinutes}:${paddedSeconds}`
  }, [otpTimer])

  const applicationTotals = useMemo(() => {
    if (!report) return []
    return report.applicationSummaries.map((item, idx) => ({
      name: item.application || 'Unspecified',
      value: Number(item.completedCount),
      color: accentColors[idx % accentColors.length],
    }))
  }, [report])

  const complexityTotals = useMemo(() => {
    if (!report) return []
    const grouped = report.complexityDistribution.reduce<Record<TaskComplexity, number>>(
      (acc, curr) => {
        acc[curr.complexity] = (acc[curr.complexity] ?? 0) + Number(curr.completedCount)
        return acc
      },
      { LOW: 0, MEDIUM: 0, HIGH: 0, VERY_HIGH: 0 },
    )
    return (Object.entries(grouped) as [TaskComplexity, number][])
      .filter(([, value]) => value > 0)
      .map(([complexity, value]) => ({ name: complexity, value, color: complexityPalette[complexity] }))
  }, [report])

  const statusTotals = useMemo(() => {
    if (!report) return []
    return report.statusDistribution.map((item) => ({
      name: item.status,
      value: Number(item.count),
      color: statusPalette[item.status],
    }))
  }, [report])

  const applicationOptions = useMemo(() => {
    const names = new Set<string>()
    tasks.forEach((t) => {
      if (t.application) names.add(t.application)
    })
    return Array.from(names).sort((a, b) => a.localeCompare(b))
  }, [tasks])

  const trendPoints = useMemo(() => {
    if (!report) return []
    return report.productivityTrend.map((item) => ({
      name: item.periodStart,
      value: item.completedCount,
    }))
  }, [report])

  const totalTaskPages = Math.max(1, Math.ceil(tasks.length / TASKS_PER_PAGE))
  const sortedTasks = useMemo(() => {
    const rank = (c: TaskComplexity) => complexityOrder.indexOf(c)
    return [...tasks].sort((a, b) => {
      if (sortField === 'due') {
        const aDate = a.deadlineDate ? new Date(a.deadlineDate).getTime() : Number.POSITIVE_INFINITY
        const bDate = b.deadlineDate ? new Date(b.deadlineDate).getTime() : Number.POSITIVE_INFINITY
        const diff = aDate - bDate
        return sortDirection === 'asc' ? diff : -diff
      }
      // complexity
      const diff = rank(a.complexity) - rank(b.complexity)
      return sortDirection === 'asc' ? diff : -diff
    })
  }, [tasks, sortField, sortDirection])
  const paginatedTasks = useMemo(() => {
    const start = (taskPage - 1) * TASKS_PER_PAGE
    return sortedTasks.slice(start, start + TASKS_PER_PAGE)
  }, [taskPage, sortedTasks])

  useEffect(() => {
    if (!token) return
    void loadTasks()
    void loadReport()
    localStorage.setItem('tracker_token', token)
  }, [token, includeArchived])

  useEffect(() => {
    setAuthError('')
    if (authMode !== 'reset') {
      setAuthInfo('')
      setOtpTimer(0)
    }
  }, [authMode])

  useEffect(() => {
    if (authMode !== 'reset' || otpTimer <= 0) return
    const timerId = window.setInterval(() => {
      setOtpTimer((prev) => (prev > 0 ? prev - 1 : 0))
    }, 1000)
    return () => window.clearInterval(timerId)
  }, [authMode, otpTimer])

  useEffect(() => {
    const maxPage = Math.max(1, Math.ceil(tasks.length / TASKS_PER_PAGE))
    if (taskPage > maxPage) {
      setTaskPage(maxPage)
    }
    if (tasks.length === 0 && taskPage !== 1) {
      setTaskPage(1)
    }
  }, [taskPage, tasks])

  function handleUnauthorized() {
    setToken('')
    localStorage.removeItem('tracker_token')
    setAuthError('Session expired. Please sign in again.')
  }

  function toggleTheme() {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'))
  }

  function handleSignOut() {
    setToken('')
    localStorage.removeItem('tracker_token')
    setAuthMode('login')
    setAuthError('')
    setAuthInfo('')
    setResetForm({ email: '', token: '', otp: '', newPassword: '', confirmPassword: '' })
    setLoginForm({ username: '', password: '' })
    setSignupForm({ username: '', email: '', password: '' })
    setForgotForm({ email: '' })
    setTaskForm({ title: '', description: '', application: '', complexity: 'MEDIUM', deadlineDate: '' })
    setIncludeArchived(false)
    setTasks([])
    setReport(null)
    setTaskError('')
    setReportError('')
    setTaskPage(1)
    const url = new URL(window.location.href)
    url.search = ''
    window.history.replaceState(null, '', url.toString())
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault()
    setAuthError('')
    setAuthInfo('')
    try {
      const res = await fetch(`${AUTH_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginForm),
      })
      if (!res.ok) {
        // Try to parse error message from response
        let errorMessage = 'Login failed'
        try {
          const errorData = await res.json()
          // Always use the message from backend if available - this includes retry count
          if (errorData && errorData.message) {
            errorMessage = errorData.message
          } else {
            // Fallback to status-based messages only if no message field
            if (res.status === 404) {
              errorMessage = 'Username is wrong'
            } else if (res.status === 401) {
              errorMessage = 'Invalid credentials'
            }
          }
        } catch (parseError) {
          // If JSON parsing fails, use status-based messages
          console.error('Failed to parse error response:', parseError)
          if (res.status === 404) {
            errorMessage = 'Username is wrong'
          } else if (res.status === 401) {
            errorMessage = 'Invalid credentials'
          }
        }
        throw new Error(errorMessage)
      }
      const data = await res.json()
      setToken(data.token)
      localStorage.setItem('tracker_token', data.token)
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Login failed')
    }
  }

  async function handleSignup(event: FormEvent) {
    event.preventDefault()
    setAuthError('')
    setAuthInfo('')
    try {
      const res = await fetch(`${AUTH_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(signupForm),
      })
      if (res.status === 400) {
        const body = await res.json()
        throw new Error(body.message || 'Unable to register')
      }
      if (!res.ok) throw new Error('Unable to register')
      const data = await res.json()
      setToken(data.token)
      localStorage.setItem('tracker_token', data.token)
      setAuthInfo('Account created. You are now signed in.')
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Unable to register')
    }
  }

  async function requestPasswordReset(email: string) {
    const res = await fetch(`${AUTH_URL}/api/auth/forgot`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
    })
    let data: BasicResponse | null = null
    try {
      data = await res.json()
    } catch (err) {
      console.error('Failed to parse forgot-password response', err)
    }

    if (!res.ok) {
      const message = data?.message ?? (res.status === 404 ? 'Email not available' : 'Unable to send reset email')
      throw new Error(message)
    }

    setResetForm((prev) => ({
      ...prev,
      email,
      token: data?.resetToken ?? prev.token,
      otp: '',
      newPassword: '',
      confirmPassword: '',
    }))
    setAuthMode('reset')
    setOtpTimer(120)
    setAuthInfo(data?.message ?? 'Check your email for the OTP to reset your password.')
    return data
  }

  async function handleForgot(event: FormEvent) {
    event.preventDefault()
    setAuthError('')
    setAuthInfo('')
    try {
      await requestPasswordReset(forgotForm.email)
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Request failed')
    }
  }

  async function handleResend() {
    if (otpTimer > 0 || isResending) return
    if (!resetForm.email) {
      setAuthError('Enter the email associated with your account to resend the code.')
      return
    }
    setAuthError('')
    setIsResending(true)
    try {
      await requestPasswordReset(resetForm.email)
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Unable to resend code')
    } finally {
      setIsResending(false)
    }
  }

  async function handleReset(event: FormEvent) {
    event.preventDefault()
    setAuthError('')
    setAuthInfo('')
    if (!resetForm.otp) {
      setAuthError('Enter the OTP sent to your email.')
      return
    }
    if (!resetForm.newPassword || !resetForm.confirmPassword) {
      setAuthError('Enter and confirm your new password.')
      return
    }
    if (resetForm.newPassword !== resetForm.confirmPassword) {
      setAuthError('Passwords do not match.')
      return
    }
    try {
      const res = await fetch(`${AUTH_URL}/api/auth/reset`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: resetForm.email,
          token: resetForm.token,
          otp: resetForm.otp,
          newPassword: resetForm.newPassword,
        }),
      })
      let body: BasicResponse | { message?: string } | null = null
      try {
        body = await res.json()
      } catch (err) {
        console.error('Failed to parse reset response', err)
      }
      if (!res.ok) {
        throw new Error(body?.message || 'Reset failed')
      }
      setAuthInfo(body?.message ?? 'Password reset successful')
      setAuthMode('login')
      setOtpTimer(0)
      setResetForm({ email: '', token: '', otp: '', newPassword: '', confirmPassword: '' })
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Reset failed')
    }
  }

  async function loadTasks() {
    if (!token) return
    setTaskLoading(true)
    setTaskError('')
    try {
      const res = await fetch(`${TASK_URL}/api/tasks?includeArchived=${includeArchived}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.status === 401) {
        handleUnauthorized()
        throw new Error('Session expired')
      }
      if (res.status === 204 || res.status === 404) {
        setTasks([])
        return
      }
      if (!res.ok) throw new Error('Failed to load tasks')
      const data = await res.json()
      setTasks(data)
      setTaskPage(1)
    } catch (err) {
      if (err instanceof TypeError) {
        setTaskError('Task service is unreachable. Please try again.')
        return
      }
      setTaskError(err instanceof Error ? err.message : 'Unable to fetch tasks')
    } finally {
      setTaskLoading(false)
    }
  }

  async function createTask(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    setTaskError('')
    try {
      const res = await fetch(`${TASK_URL}/api/tasks`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ ...taskForm, status: 'OPEN' }),
      })
      if (res.status === 401) {
        handleUnauthorized()
        throw new Error('Session expired')
      }
      if (!res.ok) throw new Error('Failed to create task')
      await loadTasks()
      setTaskForm({ title: '', description: '', application: '', complexity: 'MEDIUM', deadlineDate: '' })
    } catch (err) {
      setTaskError(err instanceof Error ? err.message : 'Unable to create task')
    }
  }

  async function updateTask(
    taskId: string,
    updates: Partial<Pick<TaskResponse, 'title' | 'description' | 'application' | 'complexity'>>,
  ) {
    if (!token) return
    const current = tasks.find((t) => t.id === taskId)
    if (!current) {
      setTaskError('Task not found')
      return
    }
    setTaskError('')
    const payload = {
      title: updates.title ?? current.title,
      description: updates.description ?? current.description,
      application: updates.application ?? current.application,
      complexity: updates.complexity ?? current.complexity,
      deadlineDate: current.deadlineDate,
      status: current.status,
    }
    try {
      const res = await fetch(`${TASK_URL}/api/tasks/${taskId}`, {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })
      if (res.status === 401) {
        handleUnauthorized()
        throw new Error('Session expired')
      }
      if (!res.ok) throw new Error('Failed to update task')
      await loadTasks()
    } catch (err) {
      setTaskError(err instanceof Error ? err.message : 'Unable to update task')
    }
  }

  async function updateStatus(taskId: string, status: TaskStatus, startDate?: string, closeDate?: string) {
    if (!token) return

    const task = tasks.find((t) => t.id === taskId)
    const existingStart = toDateInputValue(task?.startedAt ?? null)
    const startDateValue = startDate?.trim() || existingStart
    const closeDateValue = closeDate?.trim() || ''

    if (status === 'CLOSED') {
      if (!task || task.status !== 'IN_PROGRESS') {
        setTaskError('Start the task before closing it.')
        return
      }
    }

    if (status === 'IN_PROGRESS' && !startDateValue) {
      setTaskError('Select a start date to start the task.')
      return
    }

    if (status === 'CLOSED') {
      if (!startDateValue) {
        setTaskError('Select a start date before closing the task.')
        return
      }
      if (!closeDateValue) {
        setTaskError('Select a close date before closing the task.')
        return
      }
      if (new Date(closeDateValue) <= new Date(startDateValue)) {
        setTaskError('Close date must be after the start date.')
        return
      }
    }

    setTaskError('')
    try {
      const payload: { status: TaskStatus; startDate?: string; closeDate?: string } = { status }
      if (startDateValue) payload.startDate = startDateValue
      if (closeDateValue) payload.closeDate = closeDateValue

      const res = await fetch(`${TASK_URL}/api/tasks/${taskId}/status`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      if (res.status === 401) {
        handleUnauthorized()
        throw new Error('Session expired')
      }
      if (!res.ok) throw new Error('Failed to update status')
      await loadTasks()
    } catch (err) {
      setTaskError(err instanceof Error ? err.message : 'Unable to update status')
    }
  }

  async function appendNote(taskId: string, content: string) {
    if (!token || !content.trim()) return
    setTaskError('')
    try {
      const res = await fetch(`${TASK_URL}/api/tasks/${taskId}/notes`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ content }),
      })
      if (res.status === 401) {
        handleUnauthorized()
        throw new Error('Session expired')
      }
      if (!res.ok) throw new Error('Failed to append note')
      await loadTasks()
    } catch (err) {
      setTaskError(err instanceof Error ? err.message : 'Unable to append note')
    }
  }

  async function loadReport() {
    if (!token) return
    setReportLoading(true)
    setReportError('')
    try {
      const params = new URLSearchParams()
      params.set('window', reportFilter.window)
      if (reportFilter.application) params.set('application', reportFilter.application)
      if (reportFilter.complexity) params.set('complexity', reportFilter.complexity)
      params.set('sortField', reportFilter.sortField)
      params.set('sortDirection', reportFilter.sortDirection)
      const res = await fetch(`${REPORT_URL}/api/reports/tasks?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.status === 401) {
        handleUnauthorized()
        throw new Error('Session expired')
      }
      if (res.status === 204 || res.status === 404) {
        setReport({
          applicationSummaries: [],
          complexityDistribution: [],
          productivityTrend: [],
          statusDistribution: [],
        })
        return
      }
      if (!res.ok) throw new Error('Failed to load report')
      const data = await res.json()
      setReport(data)
    } catch (err) {
      if (err instanceof TypeError) {
        setReportError('Reporting service is unreachable. Please try again.')
        return
      }
      setReportError(err instanceof Error ? err.message : 'Unable to fetch report')
    } finally {
      setReportLoading(false)
    }
  }

  return (
    <div className="page">
      <header className="hero">
        <div className="hero-brand">
          <div className="brand-title">
            <div className="logo-mark" aria-hidden>
              <img src={logo} alt="Personal Tracker logo" />
            </div>
            <div className="title-copy">
              <h1>Personal Tracker</h1>
              <p className="lede">One place to plan your work, track execution, and measure your productivity.</p>
            </div>
          </div>
        </div>
        <div className="hero-actions">
          <button className="theme-toggle" type="button" onClick={toggleTheme}>
            <span aria-hidden>🌓</span>
            <span>{theme === 'dark' ? 'Dark' : 'Light'} theme</span>
          </button>
        </div>
      </header>

      <section className={`grid ${!isAuthenticated ? 'grid-center' : ''}`}>
        {!isAuthenticated && (
          <article className="card auth-card">
            <div className="card-head">
              <div>
                <p className="eyebrow">Authentication</p>
                <h2>
                  {authMode === 'login' && 'Sign in to start'}
                  {authMode === 'signup' && 'Create your account'}
                  {authMode === 'forgot' && 'Forgot password'}
                  {authMode === 'reset' && 'Enter the OTP to reset'}
                </h2>
              </div>
            </div>
            <div className="auth-tabs">
              <button className={authMode === 'login' ? 'ghost active' : 'ghost'} onClick={() => setAuthMode('login')} type="button">Sign in</button>
              <button className={authMode === 'signup' ? 'ghost active' : 'ghost'} onClick={() => setAuthMode('signup')} type="button">Create account</button>
              <button className={authMode === 'forgot' || authMode === 'reset' ? 'ghost active' : 'ghost'} onClick={() => setAuthMode('forgot')} type="button">Forgot/Reset Password</button>
            </div>

            {authMode === 'login' && (
              <form className="form auth-form" onSubmit={handleLogin}>
                <div className="field">
                  <label>Username</label>
                  <input
                    value={loginForm.username}
                    onChange={(e) => setLoginForm((prev) => ({ ...prev, username: e.target.value }))}
                    placeholder="username"
                  />
                </div>
                <div className="field">
                  <label>Password</label>
                  <input
                    type="password"
                    value={loginForm.password}
                    onChange={(e) => setLoginForm((prev) => ({ ...prev, password: e.target.value }))}
                    placeholder="password"
                  />
                </div>
                <button type="submit" className="primary">Sign in</button>
              </form>
            )}

            {authMode === 'signup' && (
              <form className="form auth-form" onSubmit={handleSignup}>
                <div className="field">
                  <label>Username</label>
                  <input
                    required
                    value={signupForm.username}
                    onChange={(e) => setSignupForm((prev) => ({ ...prev, username: e.target.value }))}
                    placeholder="your-handle"
                  />
                </div>
                <div className="field">
                  <label>Email</label>
                  <input
                    required
                    type="email"
                    value={signupForm.email}
                    onChange={(e) => setSignupForm((prev) => ({ ...prev, email: e.target.value }))}
                    placeholder="you@example.com"
                  />
                </div>
                <div className="field">
                  <label>Password</label>
                  <input
                    required
                    type="password"
                    value={signupForm.password}
                    onChange={(e) => setSignupForm((prev) => ({ ...prev, password: e.target.value }))}
                    placeholder="Choose a password"
                  />
                  <small className="hint" style={{ display: 'block', marginTop: '0.5rem' }}>
                    Password must be at least 8 characters with at least 1 uppercase, 1 lowercase, 1 number, and 1 special character
                  </small>
                </div>
                <button type="submit" className="primary">Create account</button>
              </form>
            )}

            {authMode === 'forgot' && (
              <form className="form auth-form" onSubmit={handleForgot}>
                <div className="field">
                  <label>Email</label>
                  <input
                    required
                    type="email"
                    value={forgotForm.email}
                    onChange={(e) => setForgotForm({ email: e.target.value })}
                    placeholder="you@example.com"
                  />
                </div>
                <button type="submit" className="primary">Send OTP</button>
                <small className="hint">We will send a one-time code to reset your password.</small>
              </form>
            )}

            {authMode === 'reset' && (
              <form className="form auth-form" onSubmit={handleReset}>
                <div className="field">
                  <label>Email</label>
                  <input
                    required
                    type="email"
                    value={resetForm.email}
                    onChange={(e) => setResetForm((prev) => ({ ...prev, email: e.target.value }))}
                    placeholder="you@example.com"
                  />
                </div>
                <div className="field-group compact">
                  <div className="field">
                    <label>One-time code</label>
                    <input
                      required
                      value={resetForm.otp}
                      onChange={(e) => setResetForm((prev) => ({ ...prev, otp: e.target.value }))}
                      placeholder="6-digit code"
                    />
                    <small className="hint" style={{ display: 'block', marginTop: '0.25rem' }}>
                      Enter the code sent to your email. We reuse it for the first 2 minutes.
                    </small>
                  </div>
                  <div className="field">
                    <label>Timer & resend</label>
                    <div className="otp-meta">
                      <button
                        type="button"
                        className="ghost"
                        disabled={otpTimer > 0 || isResending}
                        onClick={() => void handleResend()}
                      >
                        {otpTimer > 0 ? `Resend in ${formattedOtpTimer}` : isResending ? 'Resending…' : 'Resend OTP'}
                      </button>
                    </div>
                  </div>
                </div>
                <div className="field">
                  <label>New password</label>
                  <input
                    required
                    type={showResetPassword ? 'text' : 'password'}
                    value={resetForm.newPassword}
                    onChange={(e) => setResetForm((prev) => ({ ...prev, newPassword: e.target.value }))}
                    placeholder="Enter a new password"
                  />
                  <small className="hint" style={{ display: 'block', marginTop: '0.5rem' }}>
                    Password must be at least 8 characters with at least 1 uppercase, 1 lowercase, 1 number, and 1 special character
                  </small>
                </div>
                <div className="field">
                  <label>Confirm new password</label>
                  <input
                    required
                    type={showResetPassword ? 'text' : 'password'}
                    value={resetForm.confirmPassword}
                    onChange={(e) => setResetForm((prev) => ({ ...prev, confirmPassword: e.target.value }))}
                    placeholder="Re-enter new password"
                  />
                </div>
                <label className="inline-checkbox">
                  <input
                    type="checkbox"
                    checked={showResetPassword}
                    onChange={(e) => setShowResetPassword(e.target.checked)}
                  />
                  <span>Show password</span>
                </label>
                <button type="submit" className="primary">Reset password</button>
                <button
                  type="button"
                  className="ghost"
                  onClick={() => {
                    setAuthMode('forgot')
                    setAuthInfo('')
                    setResetForm({ email: resetForm.email, token: '', otp: '', newPassword: '', confirmPassword: '' })
                  }}
                  style={{ marginTop: '0.5rem' }}
                >
                  Back to request reset
                </button>
              </form>
            )}

            {authError && <p className="error">{authError}</p>}
            {authInfo && <p className="hint">{authInfo}</p>}
          </article>
        )}

        {isAuthenticated && (
          <>
            <article className="card tall">
              <div className="card-head">
                <div>
                  <p className="eyebrow">Tasks</p>
                  <h2>Create and manage</h2>
                </div>
                <div className="actions-inline">
                  <button className="ghost" onClick={handleSignOut}>Sign out</button>
                  <label className="switch">
                    <input
                      type="checkbox"
                      checked={includeArchived}
                      onChange={(e) => setIncludeArchived(e.target.checked)}
                    />
                    <span>Show archived</span>
                  </label>
                </div>
              </div>
              <form className="form task-form" onSubmit={createTask}>
                <div className="field">
                  <label>Title</label>
                  <input
                    required
                    value={taskForm.title}
                    onChange={(e) => setTaskForm((prev) => ({ ...prev, title: e.target.value }))}
                    placeholder="Task title"
                  />
                </div>
                <div className="field">
                  <label>Description</label>
                  <textarea
                    value={taskForm.description}
                    onChange={(e) => setTaskForm((prev) => ({ ...prev, description: e.target.value }))}
                    placeholder="What needs to happen?"
                  />
                </div>
                <div className="field">
                  <label>Application / Project</label>
                  <input
                    required
                    value={taskForm.application}
                    onChange={(e) => setTaskForm((prev) => ({ ...prev, application: e.target.value }))}
                    placeholder="Application name"
                  />
                </div>
                <div className="field-group">
                  <div className="field">
                    <label>Complexity</label>
                    <select
                      value={taskForm.complexity}
                      onChange={(e) => setTaskForm((prev) => ({ ...prev, complexity: e.target.value as TaskComplexity }))}
                    >
                      <option value="LOW">Low</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HIGH">High</option>
                      <option value="VERY_HIGH">Very High</option>
                    </select>
                  </div>
                  <div className="field">
                    <label>Deadline</label>
                    <input
                      type="date"
                      required
                      value={taskForm.deadlineDate}
                      onChange={(e) => setTaskForm((prev) => ({ ...prev, deadlineDate: e.target.value }))}
                    />
                  </div>
                </div>
                <div className="form-actions">
                  <button type="submit" className="primary">Create task</button>
                </div>
              </form>
              {taskError && <p className="error">{taskError}</p>}
              <div className="task-table-wrapper">
                {taskLoading && <p className="hint">Loading tasks…</p>}
                {!taskLoading && tasks.length === 0 && <p className="hint">No tasks yet. Create one to get started.</p>}
                {!taskLoading && tasks.length > 0 && (
                  <table className="task-table">
                    <thead>
                      <tr>
                        <th>Title</th>
                        <th>Application</th>
                        <th>
                          <button
                            type="button"
                            className="sort-button"
                            onClick={() => {
                              if (sortField === 'complexity') {
                                setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'))
                              } else {
                                setSortField('complexity')
                                setSortDirection('asc')
                              }
                            }}
                          >
                            Complexity {sortField === 'complexity' ? (sortDirection === 'asc' ? '↑' : '↓') : '↕'}
                          </button>
                        </th>
                        <th>
                          <button
                            type="button"
                            className="sort-button"
                            onClick={() => {
                              if (sortField === 'due') {
                                setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'))
                              } else {
                                setSortField('due')
                                setSortDirection('asc')
                              }
                            }}
                          >
                            Due date (dd/mm/yyyy) {sortField === 'due' ? (sortDirection === 'asc' ? '↑' : '↓') : '↕'}
                          </button>
                        </th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {paginatedTasks.map((task) => {
                        const isExpanded = expandedTaskId === task.id
                        const isEditing = editingTaskId === task.id
                        const isArchived = task.status === 'CLOSED'
                        return (
                          <TaskRow
                            key={task.id}
                            task={task}
                            isExpanded={isExpanded}
                            isEditing={isEditing}
                            isArchived={isArchived}
                            editForm={editForm}
                            onToggleExpand={() => setExpandedTaskId(isExpanded ? null : task.id)}
                            onStartEdit={() => {
                              setEditingTaskId(task.id)
                              setExpandedTaskId(task.id)
                              setEditForm({
                                title: task.title,
                                description: task.description ?? '',
                                application: task.application ?? '',
                                complexity: task.complexity,
                              })
                            }}
                            onCancelEdit={() => setEditingTaskId(null)}
                            onSaveEdit={async () => {
                              await updateTask(task.id, editForm)
                              setEditingTaskId(null)
                            }}
                            onEditFormChange={setEditForm}
                            onUpdateStatus={updateStatus}
                            onAppendNote={appendNote}
                          />
                        )
                      })}
                    </tbody>
                  </table>
                )}
              </div>
              {tasks.length > TASKS_PER_PAGE && (
                <div className="pagination">
                  <strong>
                    Page {taskPage} of {totalTaskPages}
                  </strong>
                  <div className="pagination-controls">
                    <button className="ghost" disabled={taskPage === 1} onClick={() => setTaskPage((p) => Math.max(1, p - 1))} type="button">
                      Previous
                    </button>
                    <button className="ghost" disabled={taskPage === totalTaskPages} onClick={() => setTaskPage((p) => Math.min(totalTaskPages, p + 1))} type="button">
                      Next
                    </button>
                  </div>
                </div>
              )}
            </article>

            <article className="card wide">
              <div className="card-head">
                <div>
                  <p className="eyebrow">Reporting</p>
                  <h2>Filters and trends</h2>
                </div>
                <button className="ghost" onClick={() => void loadReport()} disabled={reportLoading}>
                  {reportLoading ? 'Refreshing…' : 'Refresh report'}
                </button>
              </div>

              <div className="filters">
                <div className="field">
                  <label>Window</label>
                  <select
                    value={reportFilter.window}
                    onChange={(e) => setReportFilter((prev) => ({ ...prev, window: e.target.value as TimeWindow }))}
                  >
                    <option value="WEEKLY">Weekly</option>
                    <option value="MONTHLY">Monthly</option>
                    <option value="QUARTERLY">Quarterly</option>
                    <option value="HALF_YEARLY">Half-yearly</option>
                    <option value="YEARLY">Yearly</option>
                  </select>
                </div>
                <div className="field">
                  <label>Application / Project</label>
                  <select
                    value={reportFilter.application}
                    onChange={(e) => setReportFilter((prev) => ({ ...prev, application: e.target.value }))}
                  >
                    <option value="">Any application</option>
                    {applicationOptions.map((app) => (
                      <option key={app} value={app}>
                        {app}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>Complexity</label>
                  <select
                    value={reportFilter.complexity}
                    onChange={(e) => setReportFilter((prev) => ({ ...prev, complexity: e.target.value as TaskComplexity }))}
                  >
                    <option value="">Any</option>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="VERY_HIGH">Very High</option>
                  </select>
                </div>
                <div className="field">
                  <label>Sort field</label>
                  <select
                    value={reportFilter.sortField}
                    onChange={(e) => setReportFilter((prev) => ({ ...prev, sortField: e.target.value as SortField }))}
                  >
                    <option value="COMPLETION_DATE">Completion date</option>
                    <option value="COMPLEXITY">Complexity</option>
                  </select>
                </div>
                <div className="field">
                  <label>Sort direction</label>
                  <select
                    value={reportFilter.sortDirection}
                    onChange={(e) => setReportFilter((prev) => ({ ...prev, sortDirection: e.target.value as SortDirection }))}
                  >
                    <option value="DESC">Desc</option>
                    <option value="ASC">Asc</option>
                  </select>
                </div>
              </div>

              {reportError && <p className="error">{reportError}</p>}
              {!report && !reportLoading && <p className="hint">Run a report to see distribution and trends.</p>}
              {reportLoading && <p className="hint">Refreshing report…</p>}

              {report && (
                <div className="report-grid">
                  <div className="chart">
                    <div className="chart-head">
                      <div>
                        <p className="eyebrow">Status</p>
                        <h3>Open vs closed</h3>
                      </div>
                      <button className="ghost" onClick={() => setReport(null)}>Clear</button>
                    </div>
                    <div className="chart-body">
                      {statusTotals.length === 0 ? (
                        <p className="hint">No tasks in this window.</p>
                      ) : (
                        <PieChart width={260} height={240}>
                          <Pie data={statusTotals} dataKey="value" nameKey="name" innerRadius={60} outerRadius={90}>
                            {statusTotals.map((entry, index) => (
                              <Cell key={`status-${entry.name}-${index}`} fill={entry.color} />
                            ))}
                          </Pie>
                          <Tooltip
                            formatter={(value, _name, props) => [`${value ?? 0} tasks`, props?.payload?.name ?? '']}
                            contentStyle={{ background: 'var(--tooltip-bg)', border: '1px solid var(--card-border)', color: 'var(--text-primary)' }}
                            labelStyle={{ color: 'var(--text-primary)' }}
                            itemStyle={{ color: 'var(--text-primary)' }}
                          />
                        </PieChart>
                      )}
                    </div>
                    <div className="summary">
                      <ul>
                        {statusTotals.length === 0 && <li>No data yet.</li>}
                        {statusTotals.map((item) => (
                          <li key={item.name}>
                            <span className="dot" style={{ backgroundColor: item.color }} />
                            <span>{item.name}</span>
                            <strong>{item.value}</strong>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  <div className="chart">
                    <div className="chart-head">
                      <div>
                        <p className="eyebrow">Application mix</p>
                        <h3>Completed tasks per app</h3>
                      </div>
                      <button className="ghost" onClick={() => setReport(null)}>Clear</button>
                    </div>
                    <div className="chart-body">
                      {applicationTotals.length === 0 ? (
                        <p className="hint">No completed tasks yet.</p>
                      ) : (
                        <PieChart width={260} height={240}>
                          <Pie data={applicationTotals} dataKey="value" nameKey="name" innerRadius={60} outerRadius={90}>
                            {applicationTotals.map((entry, index) => (
                              <Cell key={`app-slice-${entry.name}-${index}`} fill={entry.color} />
                            ))}
                          </Pie>
                          <Tooltip
                            formatter={(value, _name, props) => [`${value ?? 0} tasks`, props?.payload?.name ?? '']}
                            contentStyle={{ background: 'var(--tooltip-bg)', border: '1px solid var(--card-border)', color: 'var(--text-primary)' }}
                            labelStyle={{ color: 'var(--text-primary)' }}
                            itemStyle={{ color: 'var(--text-primary)' }}
                          />
                        </PieChart>
                      )}
                    </div>
                    <div className="summary">
                      <ul>
                        {applicationTotals.length === 0 && <li>No data yet.</li>}
                        {applicationTotals.map((item) => (
                          <li key={item.name}>
                            <span className="dot" style={{ backgroundColor: item.color }} />
                            <span>{item.name}</span>
                            <strong>{item.value}</strong>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  <div className="chart">
                    <div className="chart-head">
                      <div>
                        <p className="eyebrow">Complexity spread</p>
                        <h3>Completed tasks by complexity</h3>
                      </div>
                      <button className="ghost" onClick={() => setReport(null)}>Clear</button>
                    </div>
                    <div className="chart-body">
                      {complexityTotals.length === 0 ? (
                        <p className="hint">No completed tasks yet.</p>
                      ) : (
                        <PieChart width={260} height={240}>
                          <Pie data={complexityTotals} dataKey="value" nameKey="name" innerRadius={60} outerRadius={90}>
                            {complexityTotals.map((entry, index) => (
                              <Cell key={`complexity-${entry.name}-${index}`} fill={entry.color} />
                            ))}
                          </Pie>
                          <Tooltip
                            formatter={(value, _name, props) => [`${value ?? 0} tasks`, props?.payload?.name ?? '']}
                            contentStyle={{ background: 'var(--tooltip-bg)', border: '1px solid var(--card-border)', color: 'var(--text-primary)' }}
                            labelStyle={{ color: 'var(--text-primary)' }}
                            itemStyle={{ color: 'var(--text-primary)' }}
                          />
                        </PieChart>
                      )}
                    </div>
                    <div className="summary">
                      <ul>
                        {complexityTotals.length === 0 && <li>No data yet.</li>}
                        {complexityTotals.map((item) => (
                          <li key={item.name}>
                            <span className="dot" style={{ backgroundColor: item.color }} />
                            <span>{item.name}</span>
                            <strong>{item.value}</strong>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>

                  <div className="trend">
                    <div className="chart-head">
                      <div>
                        <p className="eyebrow">Productivity</p>
                        <h3>Completion trend</h3>
                      </div>
                      <button className="ghost" onClick={() => setReport(null)}>Clear</button>
                    </div>
                    {trendPoints.length === 0 && <p className="hint">No completions in the selected window.</p>}
                    {trendPoints.length > 0 && (
                      <div className="trend-list">
                        {trendPoints.map((point, idx) => {
                          const max = Math.max(...trendPoints.map((p) => p.value)) || 1
                          const width = (point.value / max) * 100
                          return (
                            <div className="trend-row" key={`${point.name}-${idx}`}>
                              <span>{point.name}</span>
                              <div className="bar"><span style={{ width: `${width}%` }} /></div>
                              <strong>{point.value}</strong>
                            </div>
                          )
                        })}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </article>
          </>
        )}
      </section>
    </div>
  )
}

type TaskRowProps = {
  task: TaskResponse
  isExpanded: boolean
  isEditing: boolean
  isArchived: boolean
  editForm: { title: string; description: string; application: string; complexity: TaskComplexity }
  onToggleExpand: () => void
  onStartEdit: () => void
  onCancelEdit: () => void
  onSaveEdit: () => Promise<void>
  onEditFormChange: (form: { title: string; description: string; application: string; complexity: TaskComplexity }) => void
  onUpdateStatus: (taskId: string, status: TaskStatus, startDate?: string, closeDate?: string) => Promise<void>
  onAppendNote: (taskId: string, content: string) => Promise<void>
}

function TaskRow({
  task,
  isExpanded,
  isEditing,
  isArchived,
  editForm,
  onToggleExpand,
  onStartEdit,
  onCancelEdit,
  onSaveEdit,
  onEditFormChange,
  onUpdateStatus,
  onAppendNote,
}: TaskRowProps) {
  const [note, setNote] = useState('')
  const [startDateInput, setStartDateInput] = useState(() => toDateInputValue(task.startedAt ?? null))
  const [closeDateInput, setCloseDateInput] = useState(() => toDateInputValue(task.closedAt ?? null))
  const [statusError, setStatusError] = useState('')

  useEffect(() => {
    setStartDateInput(toDateInputValue(task.startedAt ?? null))
    setCloseDateInput(toDateInputValue(task.closedAt ?? null))
    setStatusError('')
  }, [task.id, task.startedAt, task.closedAt])

  const handleStart = async () => {
    const chosenStart = startDateInput || new Date().toISOString().slice(0, 10)
    setStartDateInput(chosenStart)
    setStatusError('')
    await onUpdateStatus(task.id, 'IN_PROGRESS', chosenStart)
  }

  const handleClose = async () => {
    const effectiveStart = startDateInput || toDateInputValue(task.startedAt ?? null)
    if (!effectiveStart) {
      setStatusError('Select a start date before closing.')
      return
    }
    if (!closeDateInput) {
      setStatusError('Select a close date to close the task.')
      return
    }
    if (new Date(closeDateInput) <= new Date(effectiveStart)) {
      setStatusError('Close date must be after the start date.')
      return
    }
    setStatusError('')
    await onUpdateStatus(task.id, 'CLOSED', effectiveStart, closeDateInput)
  }

  return (
    <>
      <tr className={`task-row ${isExpanded ? 'expanded' : ''}`} onClick={onToggleExpand}>
        <td className="task-title-cell">{task.title}</td>
        <td>{task.application || '—'}</td>
        <td>
          <span className="chip-sm" style={{ backgroundColor: complexityPalette[task.complexity] }}>
            {task.complexity}
          </span>
        </td>
        <td>{formatDisplayDate(task.deadlineDate)}</td>
        <td>
          <span className={`status-badge ${task.status.toLowerCase().replace('_', '-')}`}>
            {task.status.replace('_', ' ')}
          </span>
        </td>
        <td className="actions-cell" onClick={(e) => e.stopPropagation()}>
          <button className="btn-sm ghost" onClick={onStartEdit}>Edit</button>
          {!isArchived && task.status !== 'IN_PROGRESS' && (
            <button className="btn-sm ghost" onClick={() => { void handleStart() }}>Start</button>
          )}
          {!isArchived && task.status === 'IN_PROGRESS' && (
            <button className="btn-sm primary" onClick={() => { void handleClose() }}>Close</button>
          )}
        </td>
      </tr>
      {isExpanded && (
        <tr className="task-details-row">
          <td colSpan={6}>
            <div className="task-details">
              {isEditing ? (
                <div className="task-edit-inline">
                  <div className="field-row">
                    <div className="field">
                      <label>Title</label>
                      <input
                        value={editForm.title}
                        onChange={(e) => onEditFormChange({ ...editForm, title: e.target.value })}
                        onClick={(e) => e.stopPropagation()}
                      />
                    </div>
                    <div className="field">
                      <label>Application</label>
                      <input
                        value={editForm.application}
                        onChange={(e) => onEditFormChange({ ...editForm, application: e.target.value })}
                        onClick={(e) => e.stopPropagation()}
                      />
                    <div className="status-controls">
                      <div className="status-control">
                        <label>Start date</label>
                        <input
                          type="date"
                          value={startDateInput}
                          onChange={(e) => setStartDateInput(e.target.value)}
                          onClick={(e) => e.stopPropagation()}
                        />
                        <button className="btn-sm ghost" disabled={isArchived} onClick={(e) => { e.stopPropagation(); void handleStart() }}>
                          {task.status === 'IN_PROGRESS' ? 'Update start' : 'Start task'}
                        </button>
                        {task.startedAt && <small className="muted">Started on {toDateInputValue(task.startedAt ?? null)}</small>}
                      </div>
                      <div className="status-control">
                        <label>Close date</label>
                        <input
                          type="date"
                          min={startDateInput || toDateInputValue(task.startedAt ?? null) || undefined}
                          value={closeDateInput}
                          onChange={(e) => setCloseDateInput(e.target.value)}
                          onClick={(e) => e.stopPropagation()}
                        />
                        <button
                          className="btn-sm primary"
                          disabled={isArchived || task.status !== 'IN_PROGRESS'}
                          onClick={(e) => { e.stopPropagation(); void handleClose() }}
                        >
                          Close task
                        </button>
                        {task.closedAt && <small className="muted">Closed on {toDateInputValue(task.closedAt ?? null)}</small>}
                      </div>
                    </div>
                    {statusError && <p className="error inline-error">{statusError}</p>}
                    </div>
                    <div className="field">
                      <label>Complexity</label>
                      <select
                        value={editForm.complexity}
                        onChange={(e) => onEditFormChange({ ...editForm, complexity: e.target.value as TaskComplexity })}
                        onClick={(e) => e.stopPropagation()}
                      >
                        <option value="LOW">Low</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="HIGH">High</option>
                        <option value="VERY_HIGH">Very High</option>
                      </select>
                    </div>
                  </div>
                  <div className="field">
                    <label>Description</label>
                    <textarea
                      value={editForm.description}
                      onChange={(e) => onEditFormChange({ ...editForm, description: e.target.value })}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </div>
                  <div className="edit-actions">
                    <button className="ghost" onClick={onCancelEdit}>Cancel</button>
                    <button className="primary" onClick={() => void onSaveEdit()}>Save</button>
                  </div>
                </div>
              ) : (
                <>
                  <p className="detail-description">{task.description || 'No description provided.'}</p>
                  {task.archivedAt && (
                    <p className="detail-archived">Archived on {task.archivedAt.slice(0, 10)}</p>
                  )}
                </>
              )}
              <div className="detail-notes">
                <h4>Notes ({task.notes.length})</h4>
                {task.notes.length === 0 && <p className="hint">No notes yet.</p>}
                {task.notes.map((n) => (
                  <div key={n.id} className="note-item">
                    <p>{n.content}</p>
                    <small>{n.authorId} · {n.createdAt.replace('T', ' ').slice(0, 16)}</small>
                  </div>
                ))}
                {!isArchived && (
                  <div className="note-input" onClick={(e) => e.stopPropagation()}>
                    <input
                      value={note}
                      onChange={(e) => setNote(e.target.value)}
                      placeholder="Add a quick note"
                    />
                    <button className="ghost" onClick={() => { void onAppendNote(task.id, note); setNote('') }}>Add</button>
                  </div>
                )}
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  )
}

export default App
