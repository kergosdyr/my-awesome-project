import { proxyBackend } from '@/app/api/_lib/backend-proxy'

export function GET(request: Request) {
  return proxyBackend(request, '/api/products')
}
