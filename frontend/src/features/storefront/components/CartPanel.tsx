import {
  forwardRef,
  type FormEvent,
  type Ref,
} from 'react'
import { Button } from '../../../components/ui/Button'
import { NumberStepper } from '../../../components/ui/NumberStepper'
import { StatePanel } from '../../../components/ui/StatePanel'
import { TextField } from '../../../components/ui/TextField'
import type { CartLine } from '../types'

interface CartPanelProps {
  customerError?: string
  customerName: string
  error: string | null
  itemCount: number
  lines: CartLine[]
  onCustomerNameChange: (value: string) => void
  onRemove: (productId: number) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onUpdateQuantity: (productId: number, quantity: number) => void
  submitting: boolean
  totalAmount: number
}

const currencyFormatter = new Intl.NumberFormat('ko-KR', {
  style: 'currency',
  currency: 'KRW',
  maximumFractionDigits: 0,
})

export const CartPanel = forwardRef<HTMLButtonElement, CartPanelProps>(
  function CartPanel(
    {
      customerError,
      customerName,
      error,
      itemCount,
      lines,
      onCustomerNameChange,
      onRemove,
      onSubmit,
      onUpdateQuantity,
      submitting,
      totalAmount,
    },
    submitButtonRef,
  ) {
    return (
      <aside id="cart" className="work-panel cart-panel" aria-labelledby="cart-title">
        <form onSubmit={onSubmit} noValidate>
          <div className="panel-heading cart-heading">
            <div>
              <div className="panel-title-row">
                <h2 id="cart-title">주문서</h2>
                <span className="count-badge">{itemCount}개</span>
              </div>
              <p>수량과 주문자 이름을 확인하세요.</p>
            </div>
          </div>

          <div className="cart-content">
            {lines.length === 0 ? (
              <StatePanel
                kind="empty"
                title="장바구니가 비어 있습니다"
                description="카탈로그에서 상품을 담아 주세요."
              />
            ) : (
              <ul className="cart-lines" aria-label="장바구니 상품">
                {lines.map(({ product, quantity }) => (
                  <li className="cart-line" key={product.id}>
                    <div className="cart-line__top">
                      <div className="cart-line__name">
                        <strong title={product.name}>{product.name}</strong>
                        <span>{currencyFormatter.format(product.price)}</span>
                      </div>
                      <button
                        type="button"
                        className="cart-line__remove"
                        onClick={() => onRemove(product.id)}
                        aria-label={`${product.name} 장바구니에서 삭제`}
                      >
                        삭제
                      </button>
                    </div>
                    <div className="cart-line__bottom">
                      <NumberStepper
                        label={`${product.name} 수량`}
                        value={quantity}
                        max={product.stockQuantity}
                        onChange={(nextQuantity) =>
                          onUpdateQuantity(product.id, nextQuantity)
                        }
                      />
                      <strong>
                        {currencyFormatter.format(product.price * quantity)}
                      </strong>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            <div className="cart-customer">
              <TextField
                id="customer-name"
                name="customerName"
                label="주문자 이름"
                value={customerName}
                onChange={(event) => onCustomerNameChange(event.target.value)}
                placeholder="이름을 입력하세요"
                autoComplete="name"
                maxLength={40}
                error={customerError}
                disabled={submitting}
              />
            </div>

            {error && (
              <div className="cart-error" role="alert">
                <strong>주문하지 못했습니다</strong>
                <span>{error}</span>
              </div>
            )}

            <div className="cart-summary">
              <span>결제 예정 금액</span>
              <strong>{currencyFormatter.format(totalAmount)}</strong>
            </div>
          </div>

          <div className="cart-footer">
            <Button
              ref={submitButtonRef as Ref<HTMLButtonElement>}
              className="cart-submit"
              type="submit"
              variant="primary"
              disabled={lines.length === 0}
              loading={submitting}
              loadingLabel="주문하는 중"
            >
              주문하기
            </Button>
          </div>
        </form>
      </aside>
    )
  },
)
