package challenge.commerce.domain.order;

import challenge.commerce.domain.catalog.ProductReader;
import challenge.commerce.domain.catalog.StockAllocator;
import challenge.commerce.support.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * B007 — 할인 종료 1초 뒤의 주문. Backend 45분 / Minimum 20분.
 * 화면에서 티셔츠 29,000원을 봤는데 주문 직전에 판매 가격이 39,000원으로 바뀌었다.
 * 입력 CreateOrder(optionId, quantity, displayedUnitPrice): 마지막 값은 화면에서 본 개당 가격이다.
 * 서버는 이 값을 판매 가격으로 믿어서는 안 된다. 수량 1~5, 양수 가격, 순차 요청만 다룬다.
 * 가격 인상: HTTP 409와 이해할 수 있는 안내, 주문·재고 변화 없음.
 * 가격 동일: 기존처럼 주문 생성. 가격 인하: 현재 가격으로 주문 또는 409 재확인 중 직접 선택한다.
 * 이미 만든 주문: 이후 상품 가격이 바뀌어도 주문 금액과 PG 승인 금액을 유지한다.
 *
 * <p>직접 판단: 가격 인하도 다시 확인받을까? 판단은 어느 객체가 맡아야 할까?
 * 거절된 주문에서 재고가 줄지 않았음을 어떻게 확인할까? 구현 위치와 처리 방법은 직접 정한다.
 * TODO B007: 기존 create의 동작을 이어서 가격 확인 규칙을 구현한다. 기존 코드는 초기화하지 않는다.
 * 입력 전달·검증, DB 가격 변경 재현, HTTP·PG·재고 관찰 테스트는 제공한다.
 * 관찰: ./gradlew priceExperiment / 완료 검사: ./gradlew test --tests '*PriceConsentTest'
 * 제출: 실행 전 예상, 인하 정책·이유, 관찰 결과, 실제 시간·도움 범위, 자작 변형 하나.
 * 검증 한계: H2 순차 HTTP 요청. 주문 처리 도중의 동시 가격 변경, 견적 유효기간, 쿠폰, 환불은 제외.
 * 실행 구성: 기존 Commerce - Server / 터미널의 위 Gradle 명령.
 */
@Service
public class OrderService {
    private final ProductReader products;
    private final StockAllocator stock;
    private final OrderSaver orders;

    public OrderService(ProductReader products, StockAllocator stock, OrderSaver orders) {
        this.products = products;
        this.stock = stock;
        this.orders = orders;
    }

    @Transactional
    public PurchaseOrder create(CreateOrder command) {
        // TODO B007: 기존 주문 흐름에 가격 확인 규칙을 연결한다. 구현 위치와 방식은 직접 결정한다.
        var product = products.readForOption(command.optionId());
        stock.allocate(command.optionId(), command.quantity());
        return orders.create(product, command.optionId(), command.quantity());
    }

    public record CreateOrder(long optionId, int quantity, long displayedUnitPrice) {
        public CreateOrder {
            if (optionId <= 0 || quantity < 1 || quantity > 5 || displayedUnitPrice <= 0) {
                throw BusinessException.invalid("옵션과 수량(1~5개)을 확인해 주세요.");
            }
        }
    }
}
