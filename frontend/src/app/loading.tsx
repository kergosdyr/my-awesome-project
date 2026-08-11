import { StorefrontPage } from '@/features/storefront/StorefrontPage'
import { loadingCatalogState } from '@/features/storefront/catalogState'

export default function Loading() {
  return (
    <StorefrontPage
      initialCatalog={loadingCatalogState}
      interactionDisabled
      loadInitialCatalog={false}
    />
  )
}
