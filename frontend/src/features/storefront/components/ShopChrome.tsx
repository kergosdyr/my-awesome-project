'use client'

import { ArrowLeft, ShoppingBag } from '@phosphor-icons/react'
import Link from 'next/link'
import { ThemeToggle } from '@/components/site/SiteHeader'
import { useShopCart } from '../ShopCartContext'

export function ShopHeader() {
  const { itemCount } = useShopCart()

  return (
    <header className="demo-shop-header">
      <Link className="demo-shop-brand" href="/shop">
        DEMO<span>SHOP</span>
      </Link>
      <nav aria-label="Demo Shop 메뉴">
        <Link href="/shop#catalog">Catalog</Link>
        <Link href="/shop#about">About</Link>
      </nav>
      <div className="demo-shop-actions">
        <Link className="demo-shop-back" href="/">
          <ArrowLeft aria-hidden="true" /> Portfolio
        </Link>
        <Link
          className="demo-shop-cart"
          href="/shop/checkout"
          aria-label={`장바구니 ${itemCount}개`}
        >
          <ShoppingBag aria-hidden="true" />
          Cart <span>{itemCount}</span>
        </Link>
        <ThemeToggle />
      </div>
    </header>
  )
}

export function ShopFooter() {
  return (
    <footer className="demo-shop-footer" id="about">
      <div>
        <span>DEMO SHOP / SEOUL</span>
        <p>물건을 고르고 주문하는 경험을 직접 설계해 보는 가상의 상점입니다.</p>
      </div>
      <p>실제 결제와 배송은 이루어지지 않습니다.</p>
    </footer>
  )
}
