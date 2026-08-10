import { useRef, type RefObject } from 'react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
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

  const handleOpenChange = (open: boolean) => {
    if (open) return

    onClose()
    queueMicrotask(() => returnFocusRef.current?.focus())
  }

  return (
    <Dialog open onOpenChange={handleOpenChange}>
      <DialogContent
        className="sm:max-w-[420px]"
        onOpenAutoFocus={(event) => {
          event.preventDefault()
          confirmButtonRef.current?.focus()
        }}
        onCloseAutoFocus={(event) => event.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle>주문이 접수됐습니다</DialogTitle>
          <DialogDescription>주문 번호 {order.orderNumber}</DialogDescription>
        </DialogHeader>
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
        <DialogFooter>
          <DialogClose asChild>
            <Button ref={confirmButtonRef}>확인</Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
