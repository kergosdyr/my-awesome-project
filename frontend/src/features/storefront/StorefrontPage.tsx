'use client'

import { useMemo, useRef, useState, type FormEvent } from 'react'
import { Badge } from '@/components/ui/badge'
import { createOrder } from './data/storefrontApi'
import { CartPanel } from './components/CartPanel'
import { OrderSuccessDialog } from './components/OrderSuccessDialog'
import { ProductCatalog } from './components/ProductCatalog'
import { useProductCatalog } from './hooks/useProductCatalog'
import type { CartLine, CatalogState, OrderResult, Product } from './types'

interface StorefrontPageProps {
  initialCatalog?: CatalogState
  interactionDisabled?: boolean
  loadInitialCatalog?: boolean
}

export function StorefrontPage({
  initialCatalog,
  interactionDisabled = false,
  loadInitialCatalog,
}: StorefrontPageProps) {
  const { error: catalogError, products, retry, status } =
    useProductCatalog(initialCatalog, loadInitialCatalog)
  const [query, setQuery] = useState('')
  const [cartLines, setCartLines] = useState<CartLine[]>([])
  const [customerName, setCustomerName] = useState('')
  const [validationAttempted, setValidationAttempted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [orderError, setOrderError] = useState<string | null>(null)
  const [completedOrder, setCompletedOrder] = useState<OrderResult | null>(null)
  const searchInputRef = useRef<HTMLInputElement>(null)
  const submitButtonRef = useRef<HTMLButtonElement>(null)
  const cartShortcutRef = useRef<HTMLAnchorElement>(null)

  const visibleProducts = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase('ko-KR')
    if (!normalizedQuery) return products

    return products.filter((product) =>
      [product.name, product.description, product.sku].some((field) =>
        field.toLocaleLowerCase('ko-KR').includes(normalizedQuery),
      ),
    )
  }, [products, query])

  const cartQuantities = useMemo(
    () => new Map(cartLines.map((line) => [line.product.id, line.quantity])),
    [cartLines],
  )
  const itemCount = cartLines.reduce((sum, line) => sum + line.quantity, 0)
  const totalAmount = cartLines.reduce(
    (sum, line) => sum + line.product.price * line.quantity,
    0,
  )
  const customerError =
    validationAttempted && !customerName.trim()
      ? '주문자 이름을 입력해 주세요.'
      : undefined

  const addProduct = (product: Product) => {
    setOrderError(null)
    setCartLines((current) => {
      const existing = current.find((line) => line.product.id === product.id)

      if (!existing) return [...current, { product, quantity: 1 }]
      if (existing.quantity >= product.stockQuantity) return current

      return current.map((line) =>
        line.product.id === product.id
          ? { ...line, quantity: line.quantity + 1 }
          : line,
      )
    })
  }

  const updateQuantity = (productId: number, quantity: number) => {
    setOrderError(null)
    setCartLines((current) =>
      current.map((line) =>
        line.product.id === productId
          ? {
              ...line,
              quantity: Math.max(1, Math.min(quantity, line.product.stockQuantity)),
            }
          : line,
      ),
    )
  }

  const removeProduct = (productId: number) => {
    setOrderError(null)
    setCartLines((current) =>
      current.filter((line) => line.product.id !== productId),
    )
  }

  const submitOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setValidationAttempted(true)
    setOrderError(null)

    const trimmedCustomerName = customerName.trim()
    if (!trimmedCustomerName || cartLines.length === 0 || submitting) return

    setSubmitting(true)
    try {
      const order = await createOrder({
        customerName: trimmedCustomerName,
        lines: cartLines.map((line) => ({
          productId: line.product.id,
          quantity: line.quantity,
        })),
      })
      setCompletedOrder(order)
      setCartLines([])
      setCustomerName('')
      setValidationAttempted(false)
    } catch (error) {
      setOrderError(
        error instanceof Error ? error.message : '주문을 처리하지 못했습니다.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="storefront-shell">
      <header className="app-header">
        <div className="app-header__inner">
          <a className="brand" href="#catalog" aria-label="커머스 실험실 상품 카탈로그로 이동">
            <span className="brand__mark" aria-hidden="true">K</span>
            <span className="brand__copy">
              <strong>커머스 실험실</strong>
              <small>작은 주문, 깊은 실험</small>
            </span>
          </a>
          <nav className="quick-nav" aria-label="빠른 이동">
            <a href="#catalog">상품</a>
            <a ref={cartShortcutRef} href="#cart">
              장바구니
              <Badge variant="secondary" aria-label={`${itemCount}개`}>
                {itemCount}
              </Badge>
            </a>
          </nav>
        </div>
      </header>

      <main className="storefront-layout">
        <ProductCatalog
          ref={searchInputRef}
          status={status}
          error={catalogError}
          products={visibleProducts}
          totalProductCount={products.length}
          query={query}
          cartQuantities={cartQuantities}
          onSearchChange={setQuery}
          onRetry={() => void retry()}
          onAdd={addProduct}
        />
        <CartPanel
          ref={submitButtonRef}
          lines={cartLines}
          itemCount={itemCount}
          customerName={customerName}
          customerError={customerError}
          totalAmount={totalAmount}
          error={orderError}
          interactionDisabled={interactionDisabled}
          submitting={submitting}
          onCustomerNameChange={(value) => {
            setCustomerName(value)
            setOrderError(null)
          }}
          onUpdateQuantity={updateQuantity}
          onRemove={removeProduct}
          onSubmit={submitOrder}
        />
      </main>

      {completedOrder && (
        <OrderSuccessDialog
          order={completedOrder}
          onClose={() => setCompletedOrder(null)}
          returnFocusRef={cartShortcutRef}
        />
      )}
    </div>
  )
}
