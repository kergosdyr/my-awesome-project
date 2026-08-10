export type ProductStatus = 'ACTIVE' | 'SOLD_OUT' | 'INACTIVE'

export interface Product {
  description: string
  id: number
  name: string
  price: number
  sku: string
  status: ProductStatus
  stockQuantity: number
}

export interface CartLine {
  product: Product
  quantity: number
}

export interface CreateOrderRequest {
  customerName: string
  lines: Array<{
    productId: number
    quantity: number
  }>
}

export interface OrderLine {
  lineAmount: number
  productId: number
  productName: string
  quantity: number
  sku: string
  unitPrice: number
}

export interface OrderResult {
  createdAt: string
  customerName: string
  lines: OrderLine[]
  orderNumber: string
  status: string
  totalAmount: number
}

export interface ApiErrorPayload {
  code?: string
  message: string
}

export interface ApiEnvelope<T> {
  data: T | null
  error: ApiErrorPayload | string | null
}
