import { forwardRef, type Ref } from 'react'
import { Button } from '../../../components/ui/Button'
import { StatePanel } from '../../../components/ui/StatePanel'
import type { Product } from '../types'

type CatalogStatus = 'error' | 'loading' | 'success'

interface ProductCatalogProps {
  cartQuantities: ReadonlyMap<number, number>
  error: string | null
  onAdd: (product: Product) => void
  onRetry: () => void
  onSearchChange: (value: string) => void
  products: Product[]
  query: string
  status: CatalogStatus
  totalProductCount: number
}

const currencyFormatter = new Intl.NumberFormat('ko-KR', {
  style: 'currency',
  currency: 'KRW',
  maximumFractionDigits: 0,
})

function ProductSkeletons() {
  return (
    <div className="catalog-skeletons" aria-hidden="true">
      {Array.from({ length: 4 }, (_, index) => (
        <div className="catalog-skeleton" key={index}>
          <div className="skeleton-block skeleton-block--name" />
          <div className="skeleton-block skeleton-block--price" />
          <div className="skeleton-block skeleton-block--stock" />
          <div className="skeleton-block skeleton-block--button" />
        </div>
      ))}
    </div>
  )
}

function getStockPresentation(product: Product) {
  if (product.status === 'INACTIVE') {
    return { label: '판매 중지', tone: 'neutral' }
  }

  if (product.status === 'SOLD_OUT' || product.stockQuantity <= 0) {
    return { label: '품절', tone: 'neutral' }
  }

  if (product.stockQuantity <= 5) {
    return { label: `재고 ${product.stockQuantity}개`, tone: 'warning' }
  }

  return { label: `재고 ${product.stockQuantity}개`, tone: 'success' }
}

export const ProductCatalog = forwardRef<HTMLInputElement, ProductCatalogProps>(
  function ProductCatalog(
    {
      cartQuantities,
      error,
      onAdd,
      onRetry,
      onSearchChange,
      products,
      query,
      status,
      totalProductCount,
    },
    searchInputRef,
  ) {
    const resultLabel =
      status === 'success' && query
        ? `${products.length}개 표시`
        : `총 ${totalProductCount}개`

    return (
      <section id="catalog" className="work-panel catalog-panel" aria-labelledby="catalog-title">
        <div className="panel-heading catalog-heading">
          <div>
            <div className="panel-title-row">
              <h1 id="catalog-title">상품 카탈로그</h1>
              <span className="count-badge" aria-live="polite">
                {resultLabel}
              </span>
            </div>
            <p>주문 흐름을 확인할 상품을 선택하세요.</p>
          </div>
          <label className="catalog-search">
            <span className="sr-only">상품 검색</span>
            <span className="catalog-search__label" aria-hidden="true">
              검색
            </span>
            <input
              ref={searchInputRef as Ref<HTMLInputElement>}
              type="search"
              value={query}
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder="상품명 또는 SKU"
              disabled={status !== 'success' || totalProductCount === 0}
            />
            {query && (
              <button
                type="button"
                onClick={() => onSearchChange('')}
                aria-label="검색어 지우기"
              >
                지우기
              </button>
            )}
          </label>
        </div>

        <div className="catalog-columns" aria-hidden="true">
          <span>상품</span>
          <span>가격</span>
          <span>재고</span>
          <span>담기</span>
        </div>

        <div className="catalog-body">
          {status === 'loading' && (
            <>
              <span className="sr-only" role="status">
                상품을 불러오는 중입니다.
              </span>
              <ProductSkeletons />
            </>
          )}

          {status === 'error' && (
            <StatePanel
              kind="error"
              title="상품을 불러오지 못했습니다"
              description={error ?? '잠시 후 다시 시도해 주세요.'}
              action={
                <Button variant="outline" onClick={onRetry}>
                  다시 불러오기
                </Button>
              }
            />
          )}

          {status === 'success' && totalProductCount === 0 && (
            <StatePanel
              kind="empty"
              title="등록된 상품이 없습니다"
              description="백엔드에 상품을 등록하면 이곳에 표시됩니다."
            />
          )}

          {status === 'success' && totalProductCount > 0 && products.length === 0 && (
            <StatePanel
              kind="empty"
              title="검색 결과가 없습니다"
              description="다른 상품명이나 SKU로 검색해 보세요."
              action={
                <Button variant="secondary" onClick={() => onSearchChange('')}>
                  검색 초기화
                </Button>
              }
            />
          )}

          {status === 'success' && products.length > 0 && (
            <ul className="product-list" aria-label="상품 목록">
              {products.map((product) => {
                const stock = getStockPresentation(product)
                const cartQuantity = cartQuantities.get(product.id) ?? 0
                const canAdd =
                  product.status === 'ACTIVE' &&
                  product.stockQuantity > cartQuantity
                const unavailableLabel =
                  product.status === 'INACTIVE'
                    ? '중지'
                    : product.status === 'SOLD_OUT' || product.stockQuantity <= 0
                      ? '품절'
                      : '최대'

                return (
                  <li className="product-row" key={product.id}>
                    <div className="product-row__info">
                      <div className="product-row__name-line">
                        <strong title={product.name}>{product.name}</strong>
                        <span className="sku">{product.sku}</span>
                      </div>
                      <p title={product.description}>{product.description}</p>
                    </div>
                    <strong className="product-row__price">
                      {currencyFormatter.format(product.price)}
                    </strong>
                    <span className={`stock-badge stock-badge--${stock.tone}`}>
                      {stock.label}
                    </span>
                    <Button
                      size="small"
                      variant="secondary"
                      onClick={() => onAdd(product)}
                      disabled={!canAdd}
                      aria-label={`${product.name} ${cartQuantity > 0 ? '한 개 더 담기' : '담기'}`}
                    >
                      {!canAdd
                        ? unavailableLabel
                        : cartQuantity > 0
                          ? '추가'
                          : '담기'}
                    </Button>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </section>
    )
  },
)
