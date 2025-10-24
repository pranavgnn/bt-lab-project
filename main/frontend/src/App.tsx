<<<<<<< HEAD
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router'
import { AuthProvider } from './context/AuthContext'
import { Toaster } from 'sonner'
import { ThemeProvider } from 'next-themes'

import { RequireAuth } from './components/auth/RequireAuth'
import { AppLayout } from './components/layout/AppLayout'
import { Login } from './pages/auth/Login'
import { Register } from './pages/auth/Register'
import { Dashboard } from './pages/Dashboard'
import { CustomerProfile } from './pages/customer/CustomerProfile'
import { ProductList } from './pages/products/ProductList'
import { ProductForm } from './pages/products/ProductForm'
import { FdCalculator } from './pages/fd/FdCalculator'
import { AccountsList } from './pages/accounts/AccountsList'
import { AccountDetails } from './pages/accounts/AccountDetails'
import { ErrorBoundary } from './components/ErrorBoundary'

function App() {
  return (
    <ErrorBoundary>
      <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
        <AuthProvider>
          <Router>
            <div className="min-h-screen bg-background">
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route
                  path="/*"
                  element={
                    <RequireAuth>
                      <AppLayout>
                        <Routes>
                          <Route path="/" element={<Navigate to="/dashboard" replace />} />
                          <Route path="/dashboard" element={<Dashboard />} />
                          <Route path="/profile" element={<CustomerProfile />} />
                          <Route path="/products" element={<ProductList />} />
                          <Route path="/products/new" element={<ProductForm />} />
                          <Route path="/products/:id/edit" element={<ProductForm />} />
                          <Route path="/fd-calculator" element={<FdCalculator />} />
                          <Route path="/accounts" element={<AccountsList />} />
                          <Route path="/accounts/:id" element={<AccountDetails />} />
                        </Routes>
                      </AppLayout>
                    </RequireAuth>
                  }
                />
              </Routes>
              <Toaster position="top-right" richColors />
            </div>
          </Router>
        </AuthProvider>
      </ThemeProvider>
    </ErrorBoundary>
=======
import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <div>
        <a href="https://vite.dev" target="_blank">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <h1>Vite + React</h1>
      <div className="card">
        <button onClick={() => setCount((count) => count + 1)}>
          count is {count}
        </button>
        <p>
          Edit <code>src/App.tsx</code> and save to test HMR
        </p>
      </div>
      <p className="read-the-docs">
        Click on the Vite and React logos to learn more
      </p>
    </>
>>>>>>> origin/master
  )
}

export default App
