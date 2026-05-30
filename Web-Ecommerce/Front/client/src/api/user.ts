import request from './request'
import type { ApiResponse } from '@shared/types'
import type { LoginForm, RegisterForm, User, Address, AddressForm } from '@shared/types/user'

export function login(data: LoginForm): Promise<ApiResponse<{ token: string; user: User }>> {
  return request.post('/auth/login', data)
}

export function register(data: RegisterForm): Promise<ApiResponse<null>> {
  return request.post('/auth/register', data)
}

export function sendRegisterCode(email: string): Promise<ApiResponse<null>> {
  return request.post('/auth/send-register-code', { email })
}

export function getUserInfo(): Promise<ApiResponse<User>> {
  return request.get('/user/info')
}

export function updateUserInfo(data: Partial<User>): Promise<ApiResponse<null>> {
  return request.put('/user/info', data)
}

export function getAddressList(): Promise<ApiResponse<Address[]>> {
  return request.get('/user/addresses')
}

export function createAddress(data: AddressForm): Promise<ApiResponse<Address>> {
  return request.post('/user/addresses', data)
}

export function updateAddress(id: number, data: AddressForm): Promise<ApiResponse<null>> {
  return request.put(`/user/addresses/${id}`, data)
}

export function deleteAddress(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/user/addresses/${id}`)
}
