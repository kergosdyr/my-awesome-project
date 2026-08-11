'use client'

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { CartLine, OrderResult, Product } from './types'

interface ShopCartValue {
  addProduct: (product: Product) => void
  cartLines: CartLine[]
  clearCart: () => void
  hydrated: boolean
  itemCount: number
  lastOrder: OrderResult | null
  removeProduct: (productId: number) => void
  setLastOrder: (order: OrderResult | null) => void
  totalAmount: number
  updateQuantity: (productId: number, quantity: number) => void
}

const CART_KEY = 'demo-shop-cart'
const ORDER_KEY = 'demo-shop-last-order'
const ShopCartContext = createContext<ShopCartValue | null>(null)

export function ShopCartProvider({ children }: { children: ReactNode }) {
  const [cartLines, setCartLines] = useState<CartLine[]>([])
  const [lastOrder, setLastOrderState] = useState<OrderResult | null>(null)
  const [hydrated, setHydrated] = useState(false)

  useEffect(() => {
    const hydrationTask = window.setTimeout(() => {
      try {
        const storedCart = window.localStorage.getItem(CART_KEY)
        const storedOrder = window.sessionStorage.getItem(ORDER_KEY)
        if (storedCart) setCartLines(JSON.parse(storedCart) as CartLine[])
        if (storedOrder) setLastOrderState(JSON.parse(storedOrder) as OrderResult)
      } catch {
        window.localStorage.removeItem(CART_KEY)
        window.sessionStorage.removeItem(ORDER_KEY)
      } finally {
        setHydrated(true)
      }
    }, 0)

    return () => window.clearTimeout(hydrationTask)
  }, [])

  useEffect(() => {
    if (!hydrated) return
    window.localStorage.setItem(CART_KEY, JSON.stringify(cartLines))
  }, [cartLines, hydrated])

  const value = useMemo<ShopCartValue>(() => {
    const addProduct = (product: Product) => {
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
      setCartLines((current) =>
        current.map((line) =>
          line.product.id === productId
            ? {
                ...line,
                quantity: Math.max(
                  1,
                  Math.min(quantity, line.product.stockQuantity),
                ),
              }
            : line,
        ),
      )
    }

    const setLastOrder = (order: OrderResult | null) => {
      setLastOrderState(order)
      if (order) window.sessionStorage.setItem(ORDER_KEY, JSON.stringify(order))
      else window.sessionStorage.removeItem(ORDER_KEY)
    }

    return {
      addProduct,
      cartLines,
      clearCart: () => setCartLines([]),
      hydrated,
      itemCount: cartLines.reduce((sum, line) => sum + line.quantity, 0),
      lastOrder,
      removeProduct: (productId) =>
        setCartLines((current) =>
          current.filter((line) => line.product.id !== productId),
        ),
      setLastOrder,
      totalAmount: cartLines.reduce(
        (sum, line) => sum + line.product.price * line.quantity,
        0,
      ),
      updateQuantity,
    }
  }, [cartLines, hydrated, lastOrder])

  return (
    <ShopCartContext.Provider value={value}>
      {children}
    </ShopCartContext.Provider>
  )
}

export function useShopCart() {
  const value = useContext(ShopCartContext)
  if (!value) throw new Error('useShopCart must be used within ShopCartProvider')
  return value
}
