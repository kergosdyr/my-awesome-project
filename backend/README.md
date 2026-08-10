# Commerce Lab Backend

Java 21, Spring Boot, MySQL로 만든 커머스 실험의 기준 구현입니다. 작은 상품 조회와 주문 생성 흐름을 유지한 채 캐시 스탬피드, Redis, Kafka, 대기열, 락 전략 같은 주제를 브랜치별로 비교할 수 있도록 설계했습니다.

## 실행

MySQL에 `commerce_lab` 데이터베이스와 `commerce` 사용자를 만든 뒤 실행합니다.

```bash
./gradlew bootRun
```

연결 정보는 환경 변수로 바꿀 수 있습니다.

```text
DB_URL=jdbc:mysql://localhost:3306/commerce_lab?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
DB_USERNAME=commerce
DB_PASSWORD=commerce
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

테스트는 별도 H2 인메모리 데이터베이스에서 같은 Flyway 마이그레이션을 사용합니다.

```bash
./gradlew build
```

## API

모든 JSON 응답은 같은 형태입니다.

```json
{
  "data": {},
  "error": null
}
```

실패 시 `data`는 `null`, `error`는 `{ "code": "...", "message": "..." }`입니다.

### 상품 목록

```http
GET /api/products
```

Flyway가 실제 사용을 상정한 한국어 상품 여섯 개를 넣습니다. 반환 필드는 `id`, `sku`, `name`, `description`, `price`, `stockQuantity`, `status`입니다.

### 상품 상세

```http
GET /api/products/5
```

한 상품을 한 번의 쿼리로 읽습니다. 캐시 스탬피드 실험에서는 이 경로와 고정 ID를 핫 키 기준점으로 사용할 수 있습니다.

### 주문 생성

```http
POST /api/orders
Content-Type: application/json

{
  "customerName": "김코덱스",
  "lines": [
    { "productId": 1, "quantity": 2 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

한 주문에는 서로 다른 상품을 최대 20개까지 담을 수 있고, 같은 상품의 중복 라인은 허용하지 않습니다. 성공 응답에는 `orderNumber`, `customerName`, `totalAmount`, `status`, `createdAt`, 상품 스냅샷을 포함한 `lines`가 들어갑니다.

## 운영 기준점

노출하는 Actuator 엔드포인트는 다음 두 개뿐입니다.

- `GET /actuator/health`: 컨테이너와 애플리케이션 상태 확인
- `GET /actuator/prometheus`: Micrometer의 JVM, 프로세스, HTTP 서버 메트릭 수집

`/actuator/metrics` 등 나머지 웹 엔드포인트는 노출하지 않습니다.

## 경계와 트랜잭션

```text
api -> domain <- infra
         ^
       support
```

- `api`는 HTTP 검증, 요청 정규화, 응답 DTO 변환을 소유하며 컨트롤러는 서비스 파사드만 호출합니다.
- `domain`은 JPA 도메인 엔티티, 명령/쿼리, 서비스, Reader/Saver, 저장소 인터페이스를 소유합니다.
- `infra`는 Spring Data JPA 구현, MySQL 접근, 시간 및 CORS 설정을 소유합니다.
- `support`는 공통 응답 봉투와 단일 `ApiException`/`ErrorType` 체계를 소유합니다.

`ProductService`의 조회는 읽기 전용 트랜잭션입니다. `OrderService`가 주문 유스케이스의 유일한 바깥 쓰기 트랜잭션을 소유하고, 내부 `ProductReader`와 `OrderSaver`는 같은 기본 트랜잭션에 `REQUIRED`로 참여합니다.

주문은 요청 상품 ID를 오름차순으로 정렬한 뒤 `SELECT ... FOR UPDATE` 한 번으로 모두 잠급니다. 존재 여부를 전부 확인한 다음 `ProductEntity.reserve(...)`가 판매 상태와 재고를 검사하고 재고를 차감합니다. 정렬된 잠금 순서는 여러 상품을 동시에 주문할 때 교착 가능성을 줄입니다.

## Fetch 및 쿼리 계획

- `GET /api/products`: 연관관계 없는 상품 루트 1회 조회
- `GET /api/products/{id}`: 연관관계 없는 상품 루트 1회 조회
- `POST /api/orders`: 상품 잠금 조회 1회, dirty checking 상품 갱신 최대 20회, 주문 INSERT 1회, 주문 라인 INSERT 최대 20회

`OrderEntity.lines`는 최대 20개라는 집합 불변식이 있는 `LAZY @OneToMany`입니다. `OrderLineEntity.order`도 명시적으로 `LAZY`입니다. 주문 응답은 쓰기 트랜잭션 안에서 새 주문의 라인을 `PlacedOrderResult`로 고정한 뒤 API DTO로 바꾸므로, `spring.jpa.open-in-view=false`에서도 응답 매핑이 지연 로딩을 일으키지 않습니다.
