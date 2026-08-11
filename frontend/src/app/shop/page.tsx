import type { Metadata } from 'next'
import { connection } from 'next/server'
import { StorefrontPage } from '@/features/storefront/StorefrontPage'
import { loadInitialProductCatalog } from '@/features/storefront/data/serverStorefrontApi'

export const metadata: Metadata = {
  title: { absolute: 'Demo Shop' },
  description: '탐색부터 주문 완료까지 직접 체험하는 Justin의 가상 커머스 상점',
}

export default async function ShopPage() {
  await connection()
  const initialCatalog = await loadInitialProductCatalog()
  return <StorefrontPage initialCatalog={initialCatalog} />
}
