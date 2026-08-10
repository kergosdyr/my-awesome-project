import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { StorefrontPage } from './StorefrontPage'
import type { ApiEnvelope, OrderResult, Product } from './types'

const products: Product[] = [
  {
    id: 1,
    sku: 'LAB-KEYBOARD-01',
    name: '실험용 키보드',
    description: '주문과 재고 흐름을 확인하는 조용한 기계식 키보드',
    price: 89000,
    stockQuantity: 8,
    status: 'ACTIVE',
  },
  {
    id: 2,
    sku: 'LAB-MOUSE-01',
    name: '정밀 마우스',
    description: '대기열과 캐시 실험에 함께 쓰는 무선 마우스',
    price: 49000,
    stockQuantity: 0,
    status: 'SOLD_OUT',
  },
]

const completedOrder: OrderResult = {
  orderNumber: 'ORD-20260810-0001',
  customerName: '김코덱스',
  totalAmount: 178000,
  status: 'CREATED',
  createdAt: '2026-08-10T10:30:00+09:00',
  lines: [
    {
      productId: 1,
      productName: '실험용 키보드',
      quantity: 2,
      sku: 'LAB-KEYBOARD-01',
      unitPrice: 89000,
      lineAmount: 178000,
    },
  ],
}

function responseOf<T>(data: T, init?: ResponseInit) {
  const envelope: ApiEnvelope<T> = { data, error: null }
  return Promise.resolve(
    new Response(JSON.stringify(envelope), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
      ...init,
    }),
  )
}

describe('StorefrontPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(() => responseOf(products)))
  })

  it('서버에서 받은 초기 상품을 추가 브라우저 요청 없이 사용한다', () => {
    render(
      <StorefrontPage
        initialCatalog={{ status: 'success', products, error: null }}
      />,
    )

    expect(screen.getByText('실험용 키보드')).toBeVisible()
    expect(fetch).not.toHaveBeenCalled()
  })

  it('서버 초기 조회 오류에서 브라우저 재시도로 복구한다', async () => {
    const user = userEvent.setup()
    render(
      <StorefrontPage
        initialCatalog={{
          status: 'error',
          products: [],
          error: '백엔드에 연결하지 못했습니다.',
        }}
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent(
      '백엔드에 연결하지 못했습니다.',
    )
    await user.click(screen.getByRole('button', { name: '다시 불러오기' }))

    expect(await screen.findByText('실험용 키보드')).toBeVisible()
    expect(fetch).toHaveBeenCalledWith(
      '/api/products',
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )
  })

  it('상품 로딩과 검색 결과 없음 상태를 안정적으로 보여준다', async () => {
    const user = userEvent.setup()
    render(<StorefrontPage />)

    expect(screen.getByText('상품을 불러오는 중입니다.')).toBeInTheDocument()
    expect(await screen.findByText('실험용 키보드')).toBeVisible()
    expect(screen.getByRole('button', { name: '정밀 마우스 담기' })).toBeDisabled()

    await user.type(screen.getByRole('searchbox', { name: '상품 검색' }), '존재하지 않음')

    expect(screen.getByText('검색 결과가 없습니다')).toBeVisible()
    expect(screen.getByText('0개 표시')).toBeVisible()
  })

  it('수량을 변경해 주문하고 성공 대화상자를 닫으면 빠른 이동으로 초점을 돌린다', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockImplementationOnce(() => responseOf(products))
      .mockImplementationOnce(() => responseOf(completedOrder))
    const user = userEvent.setup()
    render(<StorefrontPage />)

    await user.click(
      await screen.findByRole('button', { name: '실험용 키보드 담기' }),
    )
    await user.click(
      screen.getByRole('button', { name: '실험용 키보드 수량 늘리기' }),
    )
    await user.type(screen.getByLabelText('주문자 이름'), '김코덱스')
    await user.click(screen.getByRole('button', { name: '주문하기' }))

    const dialog = await screen.findByRole('dialog', {
      name: '주문이 접수됐습니다',
    })
    expect(within(dialog).getByText('주문 번호 ORD-20260810-0001')).toBeVisible()
    expect(within(dialog).getByText('₩178,000')).toBeVisible()
    expect(fetchMock).toHaveBeenLastCalledWith(
      '/api/orders',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          customerName: '김코덱스',
          lines: [{ productId: 1, quantity: 2 }],
        }),
      }),
    )

    await user.click(within(dialog).getByRole('button', { name: '확인' }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(screen.getByRole('link', { name: /장바구니/ })).toHaveFocus()
    })
    expect(screen.getByText('장바구니가 비어 있습니다')).toBeVisible()
  })

  it('주문 API 오류를 주문서 안에서 복구 가능한 상태로 보여준다', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockImplementationOnce(() => responseOf(products))
      .mockImplementationOnce(() =>
        Promise.resolve(
          new Response(
            JSON.stringify({
              data: null,
              error: { code: 'OUT_OF_STOCK', message: '재고가 부족합니다.' },
            }),
            { status: 409, headers: { 'Content-Type': 'application/json' } },
          ),
        ),
      )
    const user = userEvent.setup()
    render(<StorefrontPage />)

    await user.click(
      await screen.findByRole('button', { name: '실험용 키보드 담기' }),
    )
    await user.type(screen.getByLabelText('주문자 이름'), '김코덱스')
    await user.click(screen.getByRole('button', { name: '주문하기' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('재고가 부족합니다.')
    expect(screen.getByRole('button', { name: '주문하기' })).toBeEnabled()
  })
})
