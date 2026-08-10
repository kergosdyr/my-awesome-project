import { forwardRef, type FormEvent, type Ref } from 'react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import { Spinner } from '@/components/ui/spinner'
import { QuantityStepper } from './QuantityStepper'
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
      <aside
        id="cart"
        className="work-panel cart-panel"
        aria-labelledby="cart-title"
      >
        <form onSubmit={onSubmit} noValidate>
          <div className="panel-heading cart-heading">
            <div>
              <div className="panel-title-row">
                <h2 id="cart-title">주문서</h2>
                <Badge variant="secondary" className="tabular-nums">
                  {itemCount}개
                </Badge>
              </div>
              <p>수량과 주문자 이름을 확인하세요.</p>
            </div>
          </div>

          <div className="cart-content">
            {lines.length === 0 ? (
              <Empty className="cart-empty-state" role="status">
                <EmptyHeader>
                  <EmptyTitle>장바구니가 비어 있습니다</EmptyTitle>
                  <EmptyDescription>
                    카탈로그에서 상품을 담아 주세요.
                  </EmptyDescription>
                </EmptyHeader>
              </Empty>
            ) : (
              <ul className="cart-lines" aria-label="장바구니 상품">
                {lines.map(({ product, quantity }) => (
                  <li className="cart-line" key={product.id}>
                    <div className="cart-line__top">
                      <div className="cart-line__name">
                        <strong title={product.name}>{product.name}</strong>
                        <span>{currencyFormatter.format(product.price)}</span>
                      </div>
                      <Button
                        type="button"
                        variant="ghost"
                        size="xs"
                        onClick={() => onRemove(product.id)}
                        aria-label={`${product.name} 장바구니에서 삭제`}
                      >
                        삭제
                      </Button>
                    </div>
                    <div className="cart-line__bottom">
                      <QuantityStepper
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
              <FieldGroup>
                <Field
                  data-disabled={submitting || undefined}
                  data-invalid={Boolean(customerError) || undefined}
                >
                  <FieldLabel htmlFor="customer-name">주문자 이름</FieldLabel>
                  <Input
                    id="customer-name"
                    name="customerName"
                    value={customerName}
                    onChange={(event) => onCustomerNameChange(event.target.value)}
                    placeholder="이름을 입력하세요"
                    autoComplete="name"
                    maxLength={40}
                    aria-invalid={Boolean(customerError)}
                    aria-describedby={
                      customerError ? 'customer-name-error' : undefined
                    }
                    disabled={submitting}
                  />
                  <FieldError id="customer-name-error">{customerError}</FieldError>
                </Field>
              </FieldGroup>
            </div>

            {error && (
              <Alert variant="destructive" className="mb-4">
                <AlertTitle>주문하지 못했습니다</AlertTitle>
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <Separator />
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
              variant="default"
              disabled={lines.length === 0 || submitting}
              aria-busy={submitting || undefined}
            >
              {submitting ? (
                <>
                  <Spinner data-icon="inline-start" aria-hidden="true" />
                  주문하는 중
                </>
              ) : (
                '주문하기'
              )}
            </Button>
          </div>
        </form>
      </aside>
    )
  },
)
