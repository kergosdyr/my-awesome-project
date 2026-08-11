import type { ApiEnvelope } from '@/features/storefront/types'
import { backendUrl } from '@/lib/backend-url'

function requestHeaders(request: Request) {
  const headers = new Headers({ Accept: 'application/json' })
  const contentType = request.headers.get('content-type')

  if (contentType) headers.set('Content-Type', contentType)

  return headers
}

function responseHeaders(response: Response) {
  const headers = new Headers()
  const contentType = response.headers.get('content-type')
  const requestId = response.headers.get('x-request-id')

  if (contentType) headers.set('Content-Type', contentType)
  if (requestId) headers.set('X-Request-Id', requestId)

  return headers
}

export async function proxyBackend(request: Request, path: string) {
  try {
    const body =
      request.method === 'GET' || request.method === 'HEAD'
        ? undefined
        : await request.arrayBuffer()
    const response = await fetch(backendUrl(path), {
      method: request.method,
      headers: requestHeaders(request),
      body,
      cache: 'no-store',
      signal: request.signal,
    })

    return new Response(response.body, {
      status: response.status,
      headers: responseHeaders(response),
    })
  } catch {
    const payload: ApiEnvelope<never> = {
      data: null,
      error: {
        code: 'BACKEND_UNAVAILABLE',
        message: '백엔드에 연결하지 못했습니다.',
      },
    }

    return Response.json(payload, { status: 502 })
  }
}
