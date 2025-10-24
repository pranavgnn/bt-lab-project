import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { jwtDecode } from 'jwt-decode'
import { api } from '@/lib/api'

interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  role: 'CUSTOMER' | 'BANK_OFFICER' | 'ADMIN'
}

interface AuthContextType {
  user: User | null
  token: string | null
  login: (email: string, password: string) => Promise<void>
  register: (userData: RegisterData) => Promise<void>
  logout: () => void
  isLoading: boolean
  isAuthenticated: boolean
}

interface RegisterData {
  email: string
  password: string
  firstName: string
  lastName: string
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'))
  const [isLoading, setIsLoading] = useState(true)

  const isAuthenticated = !!user && !!token

  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem('token')
      if (storedToken) {
        try {
          const decoded = jwtDecode<{ sub: string; role: string; email: string; firstName: string; lastName: string }>(storedToken)
          setUser({
            id: decoded.sub,
            email: decoded.email,
            firstName: decoded.firstName,
            lastName: decoded.lastName,
            role: decoded.role as 'CUSTOMER' | 'BANK_OFFICER' | 'ADMIN'
          })
          setToken(storedToken)
          api.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`
        } catch (error) {
          localStorage.removeItem('token')
          setToken(null)
        }
      }
      setIsLoading(false)
    }

    initializeAuth()
  }, [])

  const login = async (email: string, password: string) => {
    try {
      const response = await api.post('/api/auth/login', { email, password })
      const { token: newToken } = response.data
      
      localStorage.setItem('token', newToken)
      setToken(newToken)
      api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
      
      const decoded = jwtDecode<{ sub: string; role: string; email: string; firstName: string; lastName: string }>(newToken)
      setUser({
        id: decoded.sub,
        email: decoded.email,
        firstName: decoded.firstName,
        lastName: decoded.lastName,
        role: decoded.role as 'CUSTOMER' | 'BANK_OFFICER' | 'ADMIN'
      })
    } catch (error) {
      throw new Error('Login failed')
    }
  }

  const register = async (userData: RegisterData) => {
    try {
      const response = await api.post('/api/auth/register', userData)
      const { token: newToken } = response.data
      
      localStorage.setItem('token', newToken)
      setToken(newToken)
      api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
      
      const decoded = jwtDecode<{ sub: string; role: string; email: string; firstName: string; lastName: string }>(newToken)
      setUser({
        id: decoded.sub,
        email: decoded.email,
        firstName: decoded.firstName,
        lastName: decoded.lastName,
        role: decoded.role as 'CUSTOMER' | 'BANK_OFFICER' | 'ADMIN'
      })
    } catch (error) {
      throw new Error('Registration failed')
    }
  }

  const logout = () => {
    localStorage.removeItem('token')
    setToken(null)
    setUser(null)
    delete api.defaults.headers.common['Authorization']
  }

  return (
    <AuthContext.Provider value={{
      user,
      token,
      login,
      register,
      logout,
      isLoading,
      isAuthenticated
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
