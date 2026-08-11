import { StorefrontPage } from '@/features/storefront/StorefrontPage'
import { loadingCatalogState } from '@/features/storefront/catalogState'

export default function ShopLoading() {
  return (
    <StorefrontPage
      initialCatalog={loadingCatalogState}
      interactionDisabled
      loadInitialCatalog={false}
    />
  )
}
