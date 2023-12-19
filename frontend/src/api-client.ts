import axios, { AxiosError } from 'axios'

export const apiClient = axios.create({
  baseURL: '/api',
  xsrfCookieName: 'oppivelvollisuus.xsrf'
})

apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err instanceof AxiosError) {
      if (err.response?.status === 401) {
        window.location.reload()
      }
    }

    return Promise.reject(err)
  }
)
