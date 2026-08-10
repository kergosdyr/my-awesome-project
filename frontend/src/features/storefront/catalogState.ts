import type { CatalogState } from './types'

export const loadingCatalogState: CatalogState = {
  status: 'loading',
  products: [],
  error: null,
}
