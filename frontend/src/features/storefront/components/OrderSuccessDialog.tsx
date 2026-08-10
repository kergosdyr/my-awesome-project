import { useRef, type RefObject } from 'react'
import { Button } from '../../../components/ui/Button'
import { Dialog } from '../../../components/ui/Dialog'
import type { OrderResult } from '../types'

interface OrderSuccessDialogProps {
  onClose: () => void
  order: OrderResult
  returnFocusRef: RefObject<HTMLElement | null>
}

const currencyFormatter = new Intl.NumberFormat('ko-KR', {
  style: 'currency',
  currency: 'KRW',
  maximumFractionDigits: 0,
})

const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatCreatedAt(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : dateFormatter.format(date)
}

export function OrderSuccessDialog({
  onClose,
  order,
  returnFocusRef,
}: OrderSuccessDialogProps) {
  const confirmButtonRef = useRef<HTMLButtonElement>(null)

  return (
    <Dialog
      title="주문이 접수됐습니다"
      description={`주문 번호 ${order.orderNumber}`}
      onClose={onClose}
      initialFocusRef={confirmButtonRef}
      returnFocusRef={returnFocusRef}
      footer={
        <Button ref={confirmButtonRef} variant="primary" onClick={onClose}>
          확인
        </Button>
      }
    >
      <dl className="order-result">
        <div>
          <dt>주문자</dt>
          <dd>{order.customerName}</dd>
        </div>
        <div>
          <dt>상품 수량</dt>
          <dd>{order.lines.reduce((sum, line) => sum + line.quantity, 0)}개</dd>
        </div>
        <div>
          <dt>결제 금액</dt>
          <dd>{currencyFormatter.format(order.totalAmount)}</dd>
        </div>
        <div>
          <dt>접수 시각</dt>
          <dd>{formatCreatedAt(order.createdAt)}</dd>
        </div>
      </dl>
    </Dialog>
  )
}
