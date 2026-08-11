import type { Metadata } from 'next'
import { CheckoutExperience } from '@/features/storefront/CheckoutExperience'

export const metadata: Metadata = {
  title: { absolute: 'Checkout — Demo Shop' },
  description: 'Demo Shop의 가상 주문서를 작성하는 체험 페이지',
}

export default function CheckoutPage() {
  return <CheckoutExperience />
}
