import { StorefrontPage } from '@/features/storefront/StorefrontPage'
import type { CatalogState } from '@/features/storefront/types'

const loadingCatalog: CatalogState = {
  status: 'loading',
  products: [],
  error: null,
}

export default function Loading() {
  return (
    <StorefrontPage
      initialCatalog={loadingCatalog}
      loadInitialCatalog={false}
    />
  )
}
