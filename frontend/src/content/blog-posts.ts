export interface BlogPost {
  category: string
  date: string
  readingTime: string
  slug: string
  summary: string
  title: string
  paragraphs: string[]
}

export const blogPosts: BlogPost[] = [
  {
    slug: 'boundaries-before-packages',
    category: 'Architecture',
    date: '2026.08.10',
    readingTime: '6 min',
    title: '패키지를 나누기 전에 경계를 먼저 찾는 법',
    summary: 'Spring 프로젝트의 폴더 구조보다 먼저 결정해야 할 책임과 언어에 관하여.',
    paragraphs: [
      '좋은 패키지 구조는 예쁜 트리에서 시작하지 않습니다. 변화가 어디에서 시작되고 어디까지 번지는지 관찰하는 일에서 시작합니다.',
      '저는 기능을 구현하기 전에 먼저 규칙을 말로 적습니다. 같은 이유로 함께 바뀌는 규칙은 가까이 두고, 서로 다른 속도로 변하는 책임은 경계를 사이에 둡니다.',
      '이렇게 만든 경계는 프레임워크가 바뀌어도 오래 남습니다. 패키지는 그 경계를 보여 주는 결과이지, 경계 자체가 아닙니다.',
    ],
  },
  {
    slug: 'measure-before-tuning',
    category: 'Performance',
    date: '2026.08.03',
    readingTime: '5 min',
    title: '빠르게 만들기보다 먼저 제대로 재는 이유',
    summary: '성능 개선을 감이 아니라 재현 가능한 실험으로 바꾸는 작은 습관들.',
    paragraphs: [
      '성능 문제는 대개 숫자 하나로 설명되지 않습니다. 지연 시간의 분포, 데이터 크기, 동시성, 캐시 상태가 함께 움직입니다.',
      '그래서 최적화 전에 기준선을 저장합니다. 동일한 입력과 환경에서 다시 실행할 수 있어야 개선이 실제인지 우연인지 구분할 수 있습니다.',
      '측정은 속도를 늦추는 절차가 아니라 잘못된 방향으로 빠르게 달리는 일을 막아 주는 가장 짧은 길입니다.',
    ],
  },
  {
    slug: 'frontend-as-observability',
    category: 'Product',
    date: '2026.07.28',
    readingTime: '4 min',
    title: '프런트엔드를 운영 가능성의 일부로 보기',
    summary: '좋은 오류 상태와 로딩 경험이 백엔드의 상태를 어떻게 더 잘 설명하는지.',
    paragraphs: [
      '사용자가 만나는 시스템은 API 응답이 아니라 화면입니다. 백엔드가 정교해도 화면이 상태를 숨기면 사용자는 시스템을 신뢰하기 어렵습니다.',
      '로딩, 빈 상태, 재시도, 성공 피드백을 제품의 언어로 설계하면 장애와 지연도 설명 가능한 경험이 됩니다.',
      '저에게 프런트엔드는 장식이 아니라 운영 상태를 사람에게 번역하는 관측 가능성의 마지막 계층입니다.',
    ],
  },
  {
    slug: 'small-commerce-lab',
    category: 'Build log',
    date: '2026.07.19',
    readingTime: '7 min',
    title: '작은 커머스로 동시성과 실패를 공부하기',
    summary: '재고 차감과 주문 생성이라는 익숙한 문제를 실험실로 만든 기록.',
    paragraphs: [
      '커머스는 익숙하지만 결코 단순하지 않습니다. 두 사람이 마지막 재고를 동시에 주문하는 순간, 코드의 낙관적인 가정이 드러납니다.',
      '작은 도메인 안에 잠금, 트랜잭션, 오류 모델, 재시도 정책을 담고 각 선택이 만드는 결과를 측정했습니다.',
      '완성품보다 중요한 것은 선택의 근거를 남기는 일입니다. 이 Shop은 그 기록을 직접 만져 볼 수 있게 만든 인터페이스입니다.',
    ],
  },
]
