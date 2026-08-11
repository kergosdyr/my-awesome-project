'use client'

import { ArrowRight, Plus } from '@phosphor-icons/react'
import Image from 'next/image'
import { useEffect, useRef, useState } from 'react'
import { getProductBadge, getProductImage } from '../productVisuals'
import type { Product } from '../types'

type CatalogStatus = 'error' | 'loading' | 'success'

interface ProductCatalogProps {
  cartQuantities: ReadonlyMap<number, number>
  error: string | null
  onAdd: (product: Product) => void
  onClearSearch: () => void
  onLoadMore: () => void
  onRetry: () => void
  products: Product[]
  status: CatalogStatus
  visibleCount: number
}

const currencyFormatter = new Intl.NumberFormat('ko-KR', {
  style: 'currency',
  currency: 'KRW',
  maximumFractionDigits: 0,
})

export function ProductCatalog({
  cartQuantities,
  error,
  onAdd,
  onClearSearch,
  onLoadMore,
  onRetry,
  products,
  status,
  visibleCount,
}: ProductCatalogProps) {
  const sentinelRef = useRef<HTMLDivElement>(null)
  const [loadingMore, setLoadingMore] = useState(false)
  const hasMore = visibleCount < products.length
  const renderedProducts = products.slice(0, visibleCount)

  useEffect(() => {
    const sentinel = sentinelRef.current
    if (!sentinel || !hasMore || typeof IntersectionObserver === 'undefined') {
      return
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting || loadingMore) return
        setLoadingMore(true)
        window.setTimeout(() => {
          onLoadMore()
          setLoadingMore(false)
        }, 350)
      },
      { rootMargin: '280px 0px' },
    )
    observer.observe(sentinel)
    return () => observer.disconnect()
  }, [hasMore, loadingMore, onLoadMore])

  if (status === 'loading') {
    return (
      <div className="demo-product-grid" aria-label="상품을 불러오는 중입니다">
        {Array.from({ length: 8 }, (_, index) => (
          <div className="demo-product-skeleton" key={index} aria-hidden="true" />
        ))}
      </div>
    )
  }

  if (status === 'error') {
    return (
      <div className="demo-catalog-state" role="alert">
        <span>CATALOG ERROR</span>
        <h3>상품을 불러오지 못했습니다.</h3>
        <p>{error ?? '잠시 후 다시 시도해 주세요.'}</p>
        <button type="button" onClick={onRetry}>
          다시 불러오기 <ArrowRight aria-hidden="true" />
        </button>
      </div>
    )
  }

  if (products.length === 0) {
    return (
      <div className="demo-catalog-state" role="status">
        <span>NO RESULTS</span>
        <h3>찾는 물건이 없습니다.</h3>
        <p>다른 이름이나 카테고리로 다시 찾아보세요.</p>
        <button type="button" onClick={onClearSearch}>
          검색 초기화 <ArrowRight aria-hidden="true" />
        </button>
      </div>
    )
  }

  return (
    <>
      <ul className="demo-product-grid" aria-label="상품 목록">
        {renderedProducts.map((product, index) => {
          const badge = getProductBadge(product)
          const cartQuantity = cartQuantities.get(product.id) ?? 0
          const canAdd =
            product.status === 'ACTIVE' &&
            product.stockQuantity > 0 &&
            cartQuantity < product.stockQuantity

          return (
            <li className="demo-product-card" key={product.id}>
              <div className="demo-product-card__image">
                <Image
                  src={getProductImage(product)}
                  alt={`${product.name} 제품 이미지`}
                  fill
                  preload={index < 2}
                  sizes="(max-width: 640px) 100vw, (max-width: 980px) 50vw, 33vw"
                />
                {badge && <span className="demo-product-badge">{badge}</span>}
                <button
                  className="demo-product-add"
                  type="button"
                  onClick={() => onAdd(product)}
                  disabled={!canAdd}
                  aria-label={`${product.name} ${cartQuantity > 0 ? '한 개 더 담기' : '장바구니에 담기'}`}
                >
                  {canAdd ? <Plus aria-hidden="true" /> : '—'}
                </button>
              </div>
              <div className="demo-product-card__body">
                <div>
                  <span>{product.sku.split('-')[0]}</span>
                  <h3>{product.name}</h3>
                </div>
                <strong>{currencyFormatter.format(product.price)}</strong>
              </div>
              <p>{product.description}</p>
            </li>
          )
        })}
      </ul>
      <div ref={sentinelRef} className="demo-catalog-sentinel" aria-live="polite">
        {hasMore ? (
          <>
            <span>{loadingMore ? '다음 상품을 불러오는 중' : '더 많은 상품'}</span>
            <button type="button" onClick={onLoadMore}>상품 더 보기</button>
          </>
        ) : (
          <span>모든 상품을 둘러봤습니다 · {products.length} objects</span>
        )}
      </div>
    </>
  )
}
