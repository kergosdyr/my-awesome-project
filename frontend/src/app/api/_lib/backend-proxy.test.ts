import { afterEach, describe, expect, it, vi } from 'vitest'
import { proxyBackend } from './backend-proxy'

afterEach(() => {
  delete process.env.BACKEND_ORIGIN
})

describe('proxyBackend', () => {
  it('런타임 백엔드 주소로 요청하고 응답 상태와 envelope를 보존한다', async () => {
    process.env.BACKEND_ORIGIN = 'http://backend:8080'
    const backendPayload = {
      data: null,
      error: { code: 'OUT_OF_STOCK', message: '재고가 부족합니다.' },
    }
    let forwardedRequest: RequestInit | undefined
    const fetchMock = vi.fn(
      (_input: RequestInfo | URL, init?: RequestInit) => {
        forwardedRequest = init
        return Promise.resolve(
          Response.json(backendPayload, {
            status: 409,
            headers: { 'X-Request-Id': 'request-1' },
          }),
        )
      },
    )
    vi.stubGlobal('fetch', fetchMock)

    const requestBody = {
      customerName: '김코덱스',
      lines: [{ productId: 1, quantity: 2 }],
    }
    const response = await proxyBackend(
      new Request('http://localhost:3000/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody),
      }),
      '/api/orders',
    )

    expect(fetchMock).toHaveBeenCalledWith(
      new URL('http://backend:8080/api/orders'),
      expect.objectContaining({
        method: 'POST',
        cache: 'no-store',
      }),
    )
    expect(new TextDecoder().decode(forwardedRequest?.body as ArrayBuffer)).toBe(
      JSON.stringify(requestBody),
    )
    expect(response.status).toBe(409)
    expect(response.headers.get('x-request-id')).toBe('request-1')
    await expect(response.json()).resolves.toEqual(backendPayload)
  })

  it('백엔드 연결 실패를 기존 envelope 형태의 502 응답으로 변환한다', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('offline'))))

    const response = await proxyBackend(
      new Request('http://localhost:3000/api/products'),
      '/api/products',
    )

    expect(response.status).toBe(502)
    await expect(response.json()).resolves.toEqual({
      data: null,
      error: {
        code: 'BACKEND_UNAVAILABLE',
        message: '백엔드에 연결하지 못했습니다.',
      },
    })
  })
})
