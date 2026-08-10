import type {
  ApiEnvelope,
  CreateOrderRequest,
  OrderResult,
  Product,
} from '../types'

export class ApiRequestError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiRequestError'
  }
}

function getErrorMessage(error: ApiEnvelope<unknown>['error']) {
  if (typeof error === 'string') return error
  return error?.message
}

async function readEnvelope<T>(response: Response): Promise<T> {
  let payload: ApiEnvelope<T>

  try {
    payload = (await response.json()) as ApiEnvelope<T>
  } catch {
    throw new ApiRequestError('서버 응답을 읽지 못했습니다.')
  }

  const errorMessage = getErrorMessage(payload.error)
  if (!response.ok || errorMessage) {
    throw new ApiRequestError(errorMessage ?? '요청을 처리하지 못했습니다.')
  }

  if (payload.data == null) {
    throw new ApiRequestError('응답에 필요한 데이터가 없습니다.')
  }

  return payload.data
}

export async function listProducts(signal?: AbortSignal) {
  const response = await fetch('/api/products', {
    headers: { Accept: 'application/json' },
    signal,
  })

  return readEnvelope<Product[]>(response)
}

export async function createOrder(request: CreateOrderRequest) {
  const response = await fetch('/api/orders', {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  return readEnvelope<OrderResult>(response)
}
