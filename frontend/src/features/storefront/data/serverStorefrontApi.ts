import 'server-only'
import { backendUrl } from '@/lib/backend-url'
import { readEnvelope } from './storefrontApi'
import { demoProducts, withExtendedDemoCatalog } from './demoStorefront'
import type { CatalogState, Product } from '../types'

export async function loadInitialProductCatalog(): Promise<CatalogState> {
  let response: Response

  try {
    response = await fetch(backendUrl('/api/products'), {
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    })
  } catch {
    return {
      status: 'success',
      products: demoProducts,
      error: null,
    }
  }

  try {
    const products = await readEnvelope<Product[]>(response)
    return {
      status: 'success',
      products: withExtendedDemoCatalog(products),
      error: null,
    }
  } catch (error) {
    if (error instanceof TypeError) {
      return { status: 'success', products: demoProducts, error: null }
    }
    return {
      status: 'error',
      products: [],
      error:
        error instanceof Error ? error.message : '상품을 불러오지 못했습니다.',
    }
  }
}
