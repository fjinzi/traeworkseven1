import axios from 'axios'
import type { Result } from '@/types/user'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error)
    if (error.response?.status === 403) {
      return Promise.reject(new Error('无权限访问'))
    }
    return Promise.reject(error)
  }
)

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string | null
  phone: string | null
  roleType: number
}

export interface UserPageResult {
  list: UserInfo[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface UserCreateRequest {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  roleType?: number
}

export interface UserUpdateRequest {
  nickname?: string
  email?: string
  phone?: string
  roleType?: number
  status?: number
}

export interface UserQueryParams {
  username?: string
  nickname?: string
  roleType?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export const userAdminApi = {
  getUsers: async (params: UserQueryParams = {}): Promise<Result<UserPageResult>> => {
    const response = await api.get('/admin/users', { params })
    return response.data
  },

  getUserById: async (id: number): Promise<Result<UserInfo>> => {
    const response = await api.get(`/admin/users/${id}`)
    return response.data
  },

  createUser: async (data: UserCreateRequest): Promise<Result<UserInfo>> => {
    const response = await api.post('/admin/users', data)
    return response.data
  },

  updateUser: async (id: number, data: UserUpdateRequest): Promise<Result<UserInfo>> => {
    const response = await api.put(`/admin/users/${id}`, data)
    return response.data
  },

  deleteUser: async (id: number): Promise<Result<void>> => {
    const response = await api.delete(`/admin/users/${id}`)
    return response.data
  },

  restoreUser: async (id: number): Promise<Result<void>> => {
    const response = await api.post(`/admin/users/${id}/restore`)
    return response.data
  }
}
