import { apiClient } from '../api-client'

export interface HelloResponse {
  rows: number
}

export const apiGetHellos = (): Promise<HelloResponse> =>
  apiClient.get<HelloResponse>('/hello').then((res) => res.data)
