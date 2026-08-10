import { useCallback, useEffect, useRef, useState } from 'react'
import { listProducts } from '../data/storefrontApi'
import type { CatalogState } from '../types'

const loadingCatalog: CatalogState = {
  status: 'loading',
  products: [],
  error: null,
}

export function useProductCatalog(
  initialState: CatalogState = loadingCatalog,
  loadInitialCatalog = true,
) {
  const abortRef = useRef<AbortController | null>(null)
  const [state, setState] = useState<CatalogState>(initialState)

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
    if (!loadInitialCatalog || initialState.status !== 'loading') return

    const loadTask = window.setTimeout(() => void load(), 0)

    return () => {
      window.clearTimeout(loadTask)
      abortRef.current?.abort()
    }
  }, [initialState.status, load, loadInitialCatalog])

  return { ...state, retry: load }
}
