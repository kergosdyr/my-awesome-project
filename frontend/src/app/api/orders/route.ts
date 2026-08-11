import { proxyBackend } from '@/app/api/_lib/backend-proxy'

export function POST(request: Request) {
  return proxyBackend(request, '/api/orders')
}
