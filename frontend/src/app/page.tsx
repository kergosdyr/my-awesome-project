import { connection } from 'next/server'
import { StorefrontPage } from '@/features/storefront/StorefrontPage'
import { loadInitialProductCatalog } from '@/features/storefront/data/serverStorefrontApi'

export default async function HomePage() {
  await connection()
  const initialCatalog = await loadInitialProductCatalog()

  return <StorefrontPage initialCatalog={initialCatalog} />
}
