import { describe, expect, it, vi } from 'vitest'
import { ApiRequestError, listProducts } from './storefrontApi'

describe('storefrontApi', () => {
  it('오류 envelope의 메시지를 transport 오류로 변환한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve(
          new Response(
            JSON.stringify({
              data: null,
              error: { code: 'PRODUCTS_UNAVAILABLE', message: '상품 조회 실패' },
            }),
            { status: 503, headers: { 'Content-Type': 'application/json' } },
          ),
        ),
      ),
    )

    await expect(listProducts()).rejects.toEqual(
      new ApiRequestError('상품 조회 실패'),
    )
  })
})
