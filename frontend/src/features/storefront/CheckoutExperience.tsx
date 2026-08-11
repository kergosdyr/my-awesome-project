'use client'

import {
  ArrowLeft,
  ArrowRight,
  Check,
  Minus,
  Plus,
  Trash,
} from '@phosphor-icons/react'
import Image from 'next/image'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState, type FormEvent } from 'react'
import { createOrder } from './data/storefrontApi'
import { getProductImage } from './productVisuals'
import { useShopCart } from './ShopCartContext'

const currencyFormatter = new Intl.NumberFormat('ko-KR', {
  style: 'currency',
  currency: 'KRW',
  maximumFractionDigits: 0,
})

export function CheckoutExperience() {
  const router = useRouter()
  const {
    cartLines,
    clearCart,
    hydrated,
    removeProduct,
    setLastOrder,
    totalAmount,
    updateQuantity,
  } = useShopCart()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submitOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!cartLines.length || submitting) return

    const form = new FormData(event.currentTarget)
    const customerName = String(form.get('customerName') ?? '').trim()
    if (!customerName) return

    setSubmitting(true)
    setError(null)
    try {
      const order = await createOrder({
        customerName,
        lines: cartLines.map((line) => ({
          productId: line.product.id,
          quantity: line.quantity,
        })),
      })
      setLastOrder(order)
      clearCart()
      router.push('/shop/order-complete')
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : '주문을 처리하지 못했습니다.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  if (!hydrated) {
    return <main className="demo-checkout demo-checkout--loading" aria-label="주문서 준비 중" />
  }

  if (!cartLines.length) {
    return (
      <main className="demo-checkout-empty">
        <span>YOUR BAG / 00</span>
        <h1>아직 고른 물건이 없습니다.</h1>
        <p>카탈로그에서 마음에 드는 물건을 담으면 주문서를 작성할 수 있어요.</p>
        <Link href="/shop#catalog">
          카탈로그로 돌아가기 <ArrowRight aria-hidden="true" />
        </Link>
      </main>
    )
  }

  return (
    <main className="demo-checkout">
      <header className="demo-checkout__header">
        <Link href="/shop#catalog">
          <ArrowLeft aria-hidden="true" /> 계속 둘러보기
        </Link>
        <div className="demo-checkout-steps" aria-label="주문 단계">
          <span className="is-complete"><Check aria-hidden="true" /> Bag</span>
          <span className="is-current">02 Details</span>
          <span>03 Done</span>
        </div>
      </header>

      <form className="demo-checkout-grid" onSubmit={submitOrder}>
        <div className="demo-checkout-form">
          <span className="demo-shop-kicker">Step 02 / Delivery details</span>
          <h1>주문서를 작성해 주세요.</h1>
          <p className="demo-checkout-lede">
            실제 배송이나 결제는 진행되지 않습니다. 폼과 주문 상태를 체험하기 위한
            데모 정보만 입력해 주세요.
          </p>

          <fieldset>
            <legend>연락처</legend>
            <label className="demo-field demo-field--wide">
              <span>이름</span>
              <input name="customerName" required autoComplete="name" placeholder="홍길동" />
            </label>
            <label className="demo-field">
              <span>이메일</span>
              <input name="email" type="email" required autoComplete="email" placeholder="demo@example.com" />
            </label>
            <label className="demo-field">
              <span>휴대전화</span>
              <input name="phone" type="tel" autoComplete="tel" placeholder="010-0000-0000" />
            </label>
          </fieldset>

          <fieldset>
            <legend>배송지</legend>
            <label className="demo-field demo-field--wide">
              <span>주소</span>
              <input name="address" required autoComplete="street-address" placeholder="서울시 어딘가 101" />
            </label>
            <label className="demo-field">
              <span>도시</span>
              <input name="city" required autoComplete="address-level2" placeholder="서울" />
            </label>
            <label className="demo-field">
              <span>우편번호</span>
              <input name="postalCode" required inputMode="numeric" autoComplete="postal-code" placeholder="00000" />
            </label>
            <label className="demo-field demo-field--wide">
              <span>배송 메모 · 선택</span>
              <textarea name="note" rows={3} placeholder="문 앞에 놓아 주세요." />
            </label>
          </fieldset>

          <fieldset>
            <legend>데모 결제 방식</legend>
            <label className="demo-payment-option">
              <input type="radio" name="payment" value="card" defaultChecked />
              <span>Demo card</span>
              <small>카드 정보 입력 없이 주문 상태만 생성합니다.</small>
            </label>
            <label className="demo-payment-option">
              <input type="radio" name="payment" value="transfer" />
              <span>Imaginary transfer</span>
              <small>가상의 계좌 이체 흐름을 선택합니다.</small>
            </label>
          </fieldset>

          {error && <p className="demo-checkout-error" role="alert">{error}</p>}
          <button className="demo-place-order" type="submit" disabled={submitting}>
            {submitting ? '주문을 만드는 중…' : '데모 주문 완료하기'}
            {!submitting && <ArrowRight aria-hidden="true" />}
          </button>
        </div>

        <aside className="demo-order-summary" aria-labelledby="summary-title">
          <div className="demo-order-summary__heading">
            <h2 id="summary-title">YOUR BAG</h2>
            <span>{cartLines.length.toString().padStart(2, '0')}</span>
          </div>
          <ul>
            {cartLines.map(({ product, quantity }) => (
              <li key={product.id}>
                <div className="demo-order-line__image">
                  <Image src={getProductImage(product)} alt="" fill sizes="96px" />
                </div>
                <div className="demo-order-line__copy">
                  <strong>{product.name}</strong>
                  <span>{currencyFormatter.format(product.price)}</span>
                  <div className="demo-order-line__actions">
                    <button
                      type="button"
                      onClick={() => updateQuantity(product.id, quantity - 1)}
                      aria-label={`${product.name} 수량 줄이기`}
                    >
                      <Minus aria-hidden="true" />
                    </button>
                    <span>{quantity}</span>
                    <button
                      type="button"
                      onClick={() => updateQuantity(product.id, quantity + 1)}
                      aria-label={`${product.name} 수량 늘리기`}
                    >
                      <Plus aria-hidden="true" />
                    </button>
                    <button
                      type="button"
                      onClick={() => removeProduct(product.id)}
                      aria-label={`${product.name} 삭제`}
                    >
                      <Trash aria-hidden="true" />
                    </button>
                  </div>
                </div>
                <strong>{currencyFormatter.format(product.price * quantity)}</strong>
              </li>
            ))}
          </ul>
          <dl>
            <div><dt>상품 금액</dt><dd>{currencyFormatter.format(totalAmount)}</dd></div>
            <div><dt>데모 배송</dt><dd>무료</dd></div>
            <div><dt>결제 예정</dt><dd>{currencyFormatter.format(totalAmount)}</dd></div>
          </dl>
          <p>이 주문은 저장되지 않으며 실제 금액이 청구되지 않습니다.</p>
        </aside>
      </form>
    </main>
  )
}
