package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 현재 동작을 관찰하는 실험. 성공 표시는 B007 완료를 뜻하지 않는다. */
class PriceObservationTest extends PriceExperimentSupport {
    @Test
    void printBeforeAndAfterPriceChange() throws Exception {
        price(29000);
        long seen =
                request("GET", "/api/products", "").body().get(1).path("price").asLong();
        var earlier = buy(seen, 2);
        assertEquals(201, earlier.code());
        long id = earlier.body().path("id").asLong();
        price(39000);
        var late = buy(seen, 2);
        assertTrue(late.code() == 201 || late.code() == 409, "HTTP 실험 환경 또는 구현 오류");
        System.out.printf(
                "%n[할인 종료] 화면 가격=%d, 현재 가격=39000, 늦은 주문 HTTP=%d%n본문=%s%n주문 수=%d, 남은 재고=%d%n",
                seen, late.code(), late.body(), orderJpaRepository.count(), stock());
        assertEquals(200, pay(id).code());
        System.out.printf(
                "[기존 주문 결제] 주문 금액=%d, PG 승인 금액=%d%n",
                details(id).body().path("order").path("totalAmount").asLong(),
                fakePaymentGateway.lookup(String.valueOf(id)).orElseThrow().amount());
        price(19000);
        var cheaper = buy(seen, 1);
        assertTrue(cheaper.code() == 201 || cheaper.code() == 409);
        System.out.printf("[가격 인하] 화면 가격=%d, 현재 가격=19000, HTTP=%d, 본문=%s%n", seen, cheaper.code(), cheaper.body());
    }
}
