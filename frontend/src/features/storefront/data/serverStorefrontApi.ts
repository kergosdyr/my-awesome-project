import 'server-only'
import { backendUrl } from '@/lib/backend-url'
import { readEnvelope } from './storefrontApi'
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
      status: 'error',
      products: [],
      error: '백엔드에 연결하지 못했습니다.',
    }
  }

  try {
    const products = await readEnvelope<Product[]>(response)
    return { status: 'success', products, error: null }
  } catch (error) {
    return {
      status: 'error',
      products: [],
      error:
        error instanceof Error ? error.message : '상품을 불러오지 못했습니다.',
    }
  }
}
