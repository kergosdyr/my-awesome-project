import { useCallback, useEffect, useRef, useState } from 'react'
import { listProducts } from '../data/storefrontApi'
import type { Product } from '../types'

type CatalogState =
  | { status: 'loading'; products: Product[]; error: null }
  | { status: 'success'; products: Product[]; error: null }
  | { status: 'error'; products: Product[]; error: string }

export function useProductCatalog() {
  const abortRef = useRef<AbortController | null>(null)
  const [state, setState] = useState<CatalogState>({
    status: 'loading',
    products: [],
    error: null,
  })

  const load = useCallback(async () => {
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller
    setState({ status: 'loading', products: [], error: null })

    try {
      const products = await listProducts(controller.signal)
      if (!controller.signal.aborted) {
        setState({ status: 'success', products, error: null })
      }
    } catch (error) {
      if (!controller.signal.aborted) {
        setState({
          status: 'error',
          products: [],
          error:
            error instanceof Error
              ? error.message
              : '상품을 불러오지 못했습니다.',
        })
      }
    }
  }, [])

  useEffect(() => {
    void load()
    return () => abortRef.current?.abort()
  }, [load])

  return { ...state, retry: load }
}
