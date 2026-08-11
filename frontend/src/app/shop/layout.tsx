import type { ReactNode } from 'react'
import { ShopCartProvider } from '@/features/storefront/ShopCartContext'
import {
  ShopFooter,
  ShopHeader,
} from '@/features/storefront/components/ShopChrome'

export default function ShopLayout({ children }: { children: ReactNode }) {
  return (
    <ShopCartProvider>
      <div className="demo-shop-stage">
        <ShopHeader />
        {children}
        <ShopFooter />
      </div>
    </ShopCartProvider>
  )
}
