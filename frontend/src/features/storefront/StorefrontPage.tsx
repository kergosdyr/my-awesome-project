'use client'

import { ArrowDown, ArrowRight } from '@phosphor-icons/react'
import Image from 'next/image'
import Link from 'next/link'
import { useMemo, useState } from 'react'
import { ProductCatalog } from './components/ProductCatalog'
import { useProductCatalog } from './hooks/useProductCatalog'
import { useShopCart } from './ShopCartContext'
import type { CatalogState, Product } from './types'

interface StorefrontPageProps {
  initialCatalog?: CatalogState
  interactionDisabled?: boolean
  loadInitialCatalog?: boolean
}

const categories = [
  { id: 'ALL', label: 'All objects' },
  { id: 'DRINK', label: 'Drink' },
  { id: 'DESK', label: 'Desk' },
  { id: 'CARRY', label: 'Carry' },
] as const

type CategoryId = (typeof categories)[number]['id']

function belongsToCategory(product: Product, category: CategoryId) {
  if (category === 'ALL') return true
  const family = product.sku.split('-')[0]
  if (category === 'DRINK') return ['BEAN', 'TEA', 'LIFE'].includes(family)
  if (category === 'DESK') return ['DESK', 'KEYBOARD'].includes(family)
  return family === 'BAG'
}

const currencyFormatter = new Intl.NumberFormat('ko-KR', {
  style: 'currency',
  currency: 'KRW',
  maximumFractionDigits: 0,
})

export function StorefrontPage({
  initialCatalog,
  interactionDisabled = false,
  loadInitialCatalog,
}: StorefrontPageProps) {
  const { error, products, retry, status } = useProductCatalog(
    initialCatalog,
    loadInitialCatalog,
  )
  const { addProduct, cartLines, itemCount, totalAmount } = useShopCart()
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState<CategoryId>('ALL')
  const [visibleCount, setVisibleCount] = useState(8)

  const visibleProducts = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase('ko-KR')
    return products.filter((product) => {
      if (!belongsToCategory(product, category)) return false
      if (!normalizedQuery) return true
      return [product.name, product.description, product.sku].some((field) =>
        field.toLocaleLowerCase('ko-KR').includes(normalizedQuery),
      )
    })
  }, [category, products, query])

  const cartQuantities = useMemo(
    () => new Map(cartLines.map((line) => [line.product.id, line.quantity])),
    [cartLines],
  )

  const changeQuery = (value: string) => {
    setQuery(value)
    setVisibleCount(8)
  }

  const changeCategory = (value: CategoryId) => {
    setCategory(value)
    setVisibleCount(8)
  }

  return (
    <main className="demo-shop-main">
      <section className="demo-shop-hero" aria-labelledby="demo-shop-title">
        <div className="demo-shop-hero__copy">
          <span className="demo-shop-kicker">A fake store for real flows</span>
          <h1 id="demo-shop-title">
            USEFUL THINGS
            <br />
            FOR MADE-UP DAYS.
          </h1>
          <p>
            커피부터 책상 위 도구까지. 결제되지 않지만 탐색하고 고르고 주문하는
            흐름은 실제처럼 경험할 수 있습니다.
          </p>
          <Link className="demo-shop-primary-link" href="#catalog">
            컬렉션 보기 <ArrowDown aria-hidden="true" />
          </Link>
        </div>
        <div className="demo-shop-hero__visual">
          <Image
            src="/images/shop-hero-keyboard.webp"
            alt="데모 샵의 로우 프로파일 키보드 컬렉션"
            fill
            fetchPriority="high"
            loading="eager"
            sizes="(max-width: 800px) 100vw, 46vw"
          />
          <span>OBJECT 005 / DESK</span>
        </div>
      </section>

      <div className="demo-shop-marquee" aria-hidden="true">
        <span>NO PAYMENT</span>
        <span>REAL INTERACTIONS</span>
        <span>DEMO DELIVERY</span>
        <span>SEOUL / 2026</span>
      </div>

      <section className="demo-catalog" id="catalog" aria-labelledby="catalog-title">
        <header className="demo-catalog__header">
          <div>
            <span className="demo-shop-kicker">Season 01 / Everyday systems</span>
            <h2 id="catalog-title">THE CATALOG</h2>
          </div>
          <p>{products.length.toString().padStart(2, '0')} curated objects</p>
        </header>

        <div className="demo-catalog__controls">
          <div className="demo-category-tabs" role="group" aria-label="상품 카테고리">
            {categories.map((item) => (
              <button
                key={item.id}
                type="button"
                aria-pressed={category === item.id}
                onClick={() => changeCategory(item.id)}
              >
                {item.label}
              </button>
            ))}
          </div>
          <label className="demo-catalog-search">
            <span>Search</span>
            <input
              type="search"
              value={query}
              onChange={(event) => changeQuery(event.target.value)}
              placeholder="상품 이름 또는 키워드"
              disabled={interactionDisabled || status !== 'success'}
            />
          </label>
        </div>

        <ProductCatalog
          status={status}
          error={error}
          products={visibleProducts}
          visibleCount={visibleCount}
          cartQuantities={cartQuantities}
          onAdd={addProduct}
          onLoadMore={() =>
            setVisibleCount((current) =>
              Math.min(current + 6, visibleProducts.length),
            )
          }
          onRetry={() => void retry()}
          onClearSearch={() => changeQuery('')}
        />
      </section>

      {itemCount > 0 && (
        <aside className="demo-floating-cart" aria-label="장바구니 요약">
          <div>
            <span>{itemCount} items</span>
            <strong>{currencyFormatter.format(totalAmount)}</strong>
          </div>
          <Link href="/shop/checkout">
            주문서로 이동 <ArrowRight aria-hidden="true" />
          </Link>
        </aside>
      )}
    </main>
  )
}
