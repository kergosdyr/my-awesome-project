import type { Metadata } from 'next'
import { OrderCompleteExperience } from '@/features/storefront/OrderCompleteExperience'

export const metadata: Metadata = {
  title: { absolute: 'Order complete — Demo Shop' },
  description: 'Demo Shop 주문 체험 완료 화면',
}

export default function OrderCompletePage() {
  return <OrderCompleteExperience />
}
