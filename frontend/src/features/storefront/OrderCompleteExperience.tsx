'use client'

import { ArrowRight, Check } from '@phosphor-icons/react'
import Link from 'next/link'
import { useShopCart } from './ShopCartContext'

const currencyFormatter = new Intl.NumberFormat('ko-KR', {
  style: 'currency',
  currency: 'KRW',
  maximumFractionDigits: 0,
})

export function OrderCompleteExperience() {
  const { hydrated, lastOrder, setLastOrder } = useShopCart()

  if (!hydrated) {
    return <main className="demo-order-complete" aria-label="주문 결과 준비 중" />
  }

  if (!lastOrder) {
    return (
      <main className="demo-checkout-empty">
        <span>NO ORDER FOUND</span>
        <h1>완료된 데모 주문이 없습니다.</h1>
        <p>카탈로그에서 상품을 고르고 주문 흐름을 처음부터 경험해 보세요.</p>
        <Link href="/shop">
          Demo Shop 시작하기 <ArrowRight aria-hidden="true" />
        </Link>
      </main>
    )
  }

  return (
    <main className="demo-order-complete">
      <div className="demo-order-complete__mark" aria-hidden="true"><Check /></div>
      <span className="demo-shop-kicker">Order simulation complete</span>
      <h1>
        주문하는 경험이
        <br />
        완성됐습니다.
      </h1>
      <p>
        {lastOrder.customerName}님의 데모 주문을 만들었습니다. 실제 결제와 배송은
        발생하지 않아요.
      </p>
      <dl>
        <div><dt>주문 번호</dt><dd>{lastOrder.orderNumber}</dd></div>
        <div><dt>상품</dt><dd>{lastOrder.lines.length}종</dd></div>
        <div><dt>합계</dt><dd>{currencyFormatter.format(lastOrder.totalAmount)}</dd></div>
        <div><dt>상태</dt><dd>DEMO CREATED</dd></div>
      </dl>
      <Link
        href="/shop"
        onClick={() => setLastOrder(null)}
      >
        카탈로그 다시 보기 <ArrowRight aria-hidden="true" />
      </Link>
    </main>
  )
}
