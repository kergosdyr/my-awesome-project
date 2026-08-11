import type { Product } from './types'

const productImageByFamily: Record<string, string> = {
  BAG: '/images/product-linen-bag.webp',
  BEAN: '/images/product-haeundae-coffee.webp',
  DESK: '/images/product-graphite-desk-mat.webp',
  KEYBOARD: '/images/product-low-profile-keyboard.webp',
  LIFE: '/images/product-steel-tumbler.webp',
  TEA: '/images/product-jeju-matcha.webp',
}

export function getProductImage(product: Product) {
  const family = product.sku.split('-')[0]
  return productImageByFamily[family] ?? productImageByFamily.BEAN
}

export function getProductBadge(product: Product) {
  if (product.status === 'INACTIVE') return '판매 종료'
  if (product.status === 'SOLD_OUT' || product.stockQuantity <= 0) return '품절'
  if (product.stockQuantity <= 5) return '품절 임박'
  if ([2, 5, 103, 109].includes(product.id)) return 'BEST'
  if (product.id >= 100) return 'NEW'
  return null
}
