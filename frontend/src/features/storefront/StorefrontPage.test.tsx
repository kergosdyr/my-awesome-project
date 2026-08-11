import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { StorefrontPage } from './StorefrontPage'
import { ShopCartProvider } from './ShopCartContext'
import type { ApiEnvelope, Product } from './types'

const products: Product[] = [
  {
    id: 1,
    sku: 'KEYBOARD-LAB-01',
    name: '실험용 키보드',
    description: '주문 흐름을 확인하는 조용한 무선 키보드',
    price: 89000,
    stockQuantity: 8,
    status: 'ACTIVE',
  },
  {
    id: 2,
    sku: 'DESK-MAT-LAB-01',
    name: '정밀 데스크 매트',
    description: '작업 공간의 움직임을 안정적으로 받치는 매트',
    price: 49000,
    stockQuantity: 0,
    status: 'SOLD_OUT',
  },
]

function responseOf<T>(data: T) {
  const envelope: ApiEnvelope<T> = { data, error: null }
  return Promise.resolve(
    new Response(JSON.stringify(envelope), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

function renderStorefront(props?: Parameters<typeof StorefrontPage>[0]) {
  return render(
    <ShopCartProvider>
      <StorefrontPage {...props} />
    </ShopCartProvider>,
  )
}

describe('StorefrontPage', () => {
  beforeEach(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
    vi.stubGlobal('fetch', vi.fn(() => responseOf(products)))
  })

  it('서버에서 받은 초기 상품을 추가 브라우저 요청 없이 사용한다', () => {
    renderStorefront({
      initialCatalog: { status: 'success', products, error: null },
    })

    expect(screen.getByText('실험용 키보드')).toBeVisible()
    expect(fetch).not.toHaveBeenCalled()
  })

  it('route loading fallback에서는 카탈로그 입력을 비활성화한다', () => {
    renderStorefront({
      initialCatalog: { status: 'loading', products: [], error: null },
      interactionDisabled: true,
      loadInitialCatalog: false,
    })

    expect(screen.getByRole('searchbox')).toBeDisabled()
    expect(screen.getByLabelText('상품을 불러오는 중입니다')).toBeVisible()
    expect(fetch).not.toHaveBeenCalled()
  })

  it('서버 초기 조회 오류에서 브라우저 재시도로 복구한다', async () => {
    const user = userEvent.setup()
    renderStorefront({
      initialCatalog: {
        status: 'error',
        products: [],
        error: '백엔드에 연결하지 못했습니다.',
      },
    })

    expect(screen.getByRole('alert')).toHaveTextContent(
      '백엔드에 연결하지 못했습니다.',
    )
    await user.click(screen.getByRole('button', { name: /다시 불러오기/ }))

    expect(await screen.findByText('실험용 키보드')).toBeVisible()
    expect(fetch).toHaveBeenCalledWith(
      '/api/products',
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )
  })

  it('검색 결과 없음 상태에서 카탈로그로 복귀할 수 있다', async () => {
    const user = userEvent.setup()
    renderStorefront({
      initialCatalog: { status: 'success', products, error: null },
    })

    await user.type(screen.getByRole('searchbox'), '존재하지 않음')
    expect(screen.getByText('찾는 물건이 없습니다.')).toBeVisible()

    await user.click(screen.getByRole('button', { name: /검색 초기화/ }))
    expect(screen.getByText('실험용 키보드')).toBeVisible()
  })

  it('고객에게 재고 수량을 노출하지 않고 상태만 안내한다', () => {
    renderStorefront({
      initialCatalog: { status: 'success', products, error: null },
    })

    expect(screen.queryByText(/재고\s*8개/)).not.toBeInTheDocument()
    expect(screen.getByText('품절')).toBeVisible()
  })

  it('상품을 담으면 별도 주문서로 가는 장바구니 요약을 보여준다', async () => {
    const user = userEvent.setup()
    renderStorefront({
      initialCatalog: { status: 'success', products, error: null },
    })

    const addButton = screen.getByRole('button', {
      name: '실험용 키보드 장바구니에 담기',
    })
    await user.click(addButton)
    await user.click(
      screen.getByRole('button', { name: '실험용 키보드 한 개 더 담기' }),
    )

    expect(screen.getByLabelText('장바구니 요약')).toHaveTextContent('2 items')
    expect(screen.getByRole('link', { name: /주문서로 이동/ })).toHaveAttribute(
      'href',
      '/shop/checkout',
    )
  })
})
