package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** 기존 주문 가격으로 실험하되 원본 주문·결제·재고는 보존한다. */
class CouponRefundHttpTest extends PriceExperimentSupport {
    @Test
    @Tag("full")
    void planUsesOrderSnapshotWithoutChangingTheOrderOrPayment() throws Exception {
        price(29000);
        var created = buy(29000, 3);
        assertEquals(201, created.code());
        long id = created.body().path("id").asLong();
        price(39000);
        var plan = request("POST", "/dev/orders/" + id + "/coupon-refund-plan", "{\"discountAmount\":100}");
        assertEquals(200, plan.code());
        var amounts = plan.body().path("lines").get(0).path("unitRefundAmounts");
        assertEquals(3, amounts.size());
        long total = 0;
        for (var amount : amounts) total += amount.asLong();
        assertEquals(86900, total);
        assertEquals(created.body(), details(id).body().path("order"));
        assertEquals(17, stock());
        assertEquals(0, payments.count());
        assertEquals(0, gateway.approvals());
    }

    @Test
    void invalidCouponIsRejectedByProvidedInputBoundary() throws Exception {
        long id = order();
        String path = "/dev/orders/" + id + "/coupon-refund-plan";
        for (var body : new String[] {"{}", "{\"discountAmount\":-1}", "{\"discountAmount\":129001}"}) {
            assertEquals(400, request("POST", path, body).code());
        }
    }

    @Test
    void missingOrderReturnsNotFoundWithoutCallingPlanner() throws Exception {
        assertEquals(
                404,
                request("POST", "/dev/orders/99999/coupon-refund-plan", "{\"discountAmount\":100}")
                        .code());
    }
}
