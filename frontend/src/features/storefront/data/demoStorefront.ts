import type { CreateOrderRequest, OrderResult, Product } from '../types'

export const demoProducts: Product[] = [
  {
    id: 1,
    sku: 'BEAN-HAEUNDAE-1KG',
    name: '해운대 블렌드 원두 1kg',
    description: '초콜릿과 견과의 단맛이 오래 남는 데일리 미디엄 로스트',
    price: 28900,
    stockQuantity: 120,
    status: 'ACTIVE',
  },
  {
    id: 2,
    sku: 'TEA-JEJU-MATCHA',
    name: '유기농 제주 말차 틴',
    description: '제주 차밭에서 키운 찻잎을 곱게 갈아 담은 40g 말차',
    price: 18900,
    stockQuantity: 80,
    status: 'ACTIVE',
  },
  {
    id: 3,
    sku: 'LIFE-TUMBLER-450',
    name: '미니멀 스테인리스 텀블러',
    description: '보온과 보냉이 가능한 무광 스테인리스 450ml 텀블러',
    price: 27900,
    stockQuantity: 55,
    status: 'ACTIVE',
  },
  {
    id: 4,
    sku: 'DESK-MAT-GRAPHITE',
    name: '그래파이트 데스크 매트',
    description: '키보드와 마우스를 안정적으로 받쳐 주는 생활 방수 데스크 매트',
    price: 39000,
    stockQuantity: 42,
    status: 'ACTIVE',
  },
  {
    id: 5,
    sku: 'KEYBOARD-LOWPROFILE',
    name: '로우 프로파일 무선 키보드',
    description: '세 대의 기기를 오가며 연결하는 저소음 블루투스 키보드',
    price: 129000,
    stockQuantity: 18,
    status: 'ACTIVE',
  },
  {
    id: 6,
    sku: 'BAG-LINEN-NATURAL',
    name: '내추럴 리넨 에코백',
    description: '노트북과 장보기를 모두 담을 수 있는 안감 보강 리넨 백',
    price: 24000,
    stockQuantity: 0,
    status: 'SOLD_OUT',
  },
  {
    id: 101,
    sku: 'BEAN-NIGHT-SWIM-500G',
    name: '나이트 스윔 블렌드 500g',
    description: '다크 초콜릿과 검은 체리의 여운을 담은 늦은 오후의 블렌드',
    price: 21000,
    stockQuantity: 36,
    status: 'ACTIVE',
  },
  {
    id: 102,
    sku: 'TEA-MATCHA-WHISK-SET',
    name: '말차 스타터 세트',
    description: '말차 한 틴과 대나무 차선, 작은 계량 스푼을 함께 담은 세트',
    price: 46000,
    stockQuantity: 24,
    status: 'ACTIVE',
  },
  {
    id: 103,
    sku: 'LIFE-BOTTLE-SMOKE-600',
    name: '스모크 워터 보틀 600ml',
    description: '하루의 물을 가볍게 챙기는 반투명 스모크 컬러 보틀',
    price: 32000,
    stockQuantity: 48,
    status: 'ACTIVE',
  },
  {
    id: 104,
    sku: 'DESK-MAT-SAND-MINI',
    name: '샌드 데스크 매트 미니',
    description: '작은 책상과 노트북 작업에 맞춘 컴팩트 생활 방수 매트',
    price: 29000,
    stockQuantity: 4,
    status: 'ACTIVE',
  },
  {
    id: 105,
    sku: 'KEYBOARD-SAND-LOWPROFILE',
    name: '샌드 로우 프로파일 키보드',
    description: '따뜻한 샌드 컬러와 조용한 타건감을 조합한 무선 키보드',
    price: 139000,
    stockQuantity: 12,
    status: 'ACTIVE',
  },
  {
    id: 106,
    sku: 'BAG-LINEN-SHOULDER',
    name: '리넨 숄더 토트',
    description: '넓은 스트랩과 안쪽 포켓을 더한 내추럴 리넨 데일리 백',
    price: 36000,
    stockQuantity: 30,
    status: 'ACTIVE',
  },
  {
    id: 107,
    sku: 'BEAN-DECAF-DUSK-500G',
    name: '디카페인 더스크 500g',
    description: '카라멜의 단맛과 부드러운 질감을 남긴 저녁용 디카페인',
    price: 23000,
    stockQuantity: 28,
    status: 'ACTIVE',
  },
  {
    id: 108,
    sku: 'TEA-JEJU-HOJICHA',
    name: '제주 호지차 틴',
    description: '고소하게 볶은 찻잎의 향과 낮은 카페인이 편안한 호지차',
    price: 17000,
    stockQuantity: 40,
    status: 'ACTIVE',
  },
  {
    id: 109,
    sku: 'LIFE-TUMBLER-VIOLET-350',
    name: '바이올렛 텀블러 350ml',
    description: '작은 가방에도 들어가는 선명한 바이올렛 포인트 텀블러',
    price: 26000,
    stockQuantity: 3,
    status: 'ACTIVE',
  },
  {
    id: 110,
    sku: 'DESK-MAT-GRAPHITE-WIDE',
    name: '그래파이트 데스크 매트 와이드',
    description: '키보드와 노트, 마우스까지 한 번에 올리는 와이드 사이즈',
    price: 52000,
    stockQuantity: 16,
    status: 'ACTIVE',
  },
  {
    id: 111,
    sku: 'KEYBOARD-INK-NUMERIC',
    name: '잉크 숫자 키패드',
    description: '필요할 때만 꺼내 쓰는 저소음 블루투스 숫자 키패드',
    price: 59000,
    stockQuantity: 20,
    status: 'ACTIVE',
  },
  {
    id: 112,
    sku: 'BAG-LINEN-MARKET',
    name: '리넨 마켓 백',
    description: '접어서 휴대하기 좋고 바닥을 넓게 보강한 가벼운 장바구니',
    price: 22000,
    stockQuantity: 0,
    status: 'SOLD_OUT',
  },
]

export function withExtendedDemoCatalog(products: Product[]) {
  const existingIds = new Set(products.map((product) => product.id))
  return [
    ...products,
    ...demoProducts.filter(
      (product) => product.id >= 100 && !existingIds.has(product.id),
    ),
  ]
}

export function createDemoOrder(request: CreateOrderRequest): OrderResult {
  const lines = request.lines.map(({ productId, quantity }) => {
    const product = demoProducts.find((candidate) => candidate.id === productId)
    if (!product) throw new Error('데모 상품을 찾을 수 없습니다.')

    return {
      productId,
      productName: product.name,
      quantity,
      sku: product.sku,
      unitPrice: product.price,
      lineAmount: product.price * quantity,
    }
  })

  return {
    orderNumber: `DEMO-${Date.now().toString().slice(-8)}`,
    customerName: request.customerName,
    totalAmount: lines.reduce((sum, line) => sum + line.lineAmount, 0),
    status: 'DEMO_CREATED',
    createdAt: new Date().toISOString(),
    lines,
  }
}
