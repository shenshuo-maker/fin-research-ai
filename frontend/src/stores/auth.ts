import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const username = ref<string | null>(localStorage.getItem('username'))
  const userId = ref<string | null>(localStorage.getItem('userId'))

  function setSession(t: string, u: string, id: number) {
    token.value = t
    username.value = u
    userId.value = String(id)
    localStorage.setItem('token', t)
    localStorage.setItem('username', u)
    localStorage.setItem('userId', String(id))
  }

  function logout() {
    token.value = null
    username.value = null
    userId.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('userId')
  }

  async function login(user: string, pass: string) {
    const { data } = await http.post('/api/auth/login', { username: user, password: pass })
    setSession(data.token, data.username, data.userId)
  }

  async function register(user: string, pass: string, displayName?: string) {
    const { data } = await http.post('/api/auth/register', {
      username: user,
      password: pass,
      displayName: displayName || user,
    })
    setSession(data.token, data.username, data.userId)
  }

  return { token, username, userId, login, register, logout, setSession }
})
