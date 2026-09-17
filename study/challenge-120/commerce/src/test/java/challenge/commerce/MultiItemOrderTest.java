package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 다품목 주문의 전부 성공/전부 취소와 구매 당시 금액을 HTTP·DB·PG에서 확인한다. */
class MultiItemOrderTest extends PriceExperimentSupport {
    @org.junit.jupiter.api.BeforeEach
    void salePrice() {
        price(29000);
    }

    private static final String TWO_ITEMS = """
            {"items":[
              {"optionId":101,"quantity":2,"displayedUnitPrice":129000},
              {"optionId":201,"quantity":3,"displayedUnitPrice":29000}
            ]}
            """;

    @Test
    void oneOrderContainsBothItemsAndOnePaymentKeepsTheirSnapshot() throws Exception {
        var created = request("POST", "/api/orders", TWO_ITEMS);
        assertEquals(201, created.code());
        var order = created.body();
        long id = order.path("id").asLong();
        assertEquals(1, orders.count());
        assertEquals(2, order.path("items").size());
        assertEquals(345000, order.path("totalAmount").asLong());
        assertEquals(258000, order.path("items").get(0).path("totalAmount").asLong());
        assertEquals(87000, order.path("items").get(1).path("totalAmount").asLong());
        assertEquals(10, stockFor(101));
        assertEquals(17, stockFor(201));
        database.update("update store_product set price = price + 10000, name = 'Changed'");
        assertEquals(order, details(id).body().path("order"));
        assertEquals(200, pay(id).code());
        assertEquals(200, pay(id).code());
        assertEquals(345000, gateway.lookup(String.valueOf(id)).orElseThrow().amount());
        assertEquals(1, gateway.approvals());
        assertEquals(1, payments.count());
        assertEquals(
                2,
                request("GET", "/api/orders", "")
                        .body()
                        .get(0)
                        .path("order")
                        .path("items")
                        .size());
    }

    @Test
    void laterStockFailureRollsBackEarlierStockAllocation() throws Exception {
        database.update("update store_option set stock = 1 where id = 201");
        assertEquals(409, request("POST", "/api/orders", TWO_ITEMS).code());
        assertNoOrder();
        assertEquals(12, stockFor(101), "먼저 확보한 재고도 되돌린다");
        assertEquals(1, stockFor(201));
    }

    @Test
    void secondItemPriceMismatchRejectsWholeOrder() throws Exception {
        price(39000);
        assertEquals(409, request("POST", "/api/orders", TWO_ITEMS).code());
        assertNoOrder();
        assertEquals(12, stockFor(101));
        assertEquals(20, stockFor(201));
    }

    @Test
    void equalGrandTotalCannotHideIndividualPriceChanges() throws Exception {
        database.update("update store_product set price = 130000 where id = 1");
        price(28000);
        var body = """
                {"items":[{"optionId":101,"quantity":1,"displayedUnitPrice":129000},
                          {"optionId":201,"quantity":1,"displayedUnitPrice":29000}]}
                """;
        assertEquals(409, request("POST", "/api/orders", body).code());
        assertNoOrder();
        assertEquals(12, stockFor(101));
        assertEquals(20, stockFor(201));
    }

    @Test
    void duplicateOptionsCannotBypassPerOptionQuantityLimit() throws Exception {
        var body = """
                {"items":[{"optionId":101,"quantity":3,"displayedUnitPrice":129000},
                          {"optionId":101,"quantity":3,"displayedUnitPrice":129000}]}
                """;
        assertEquals(400, request("POST", "/api/orders", body).code());
        assertNoOrder();
        assertEquals(12, stockFor(101));
    }

    @Test
    void invalidOrMissingItemsRejectWithoutWrites() throws Exception {
        for (var body : new String[] {
            "{}",
            "{\"items\":null}",
            "{\"items\":[]}",
            "{\"items\":[null]}",
            "{\"items\":[{\"optionId\":101,\"quantity\":1}]}",
            "{\"items\":[{\"optionId\":101,\"quantity\":0,\"displayedUnitPrice\":129000}]}"
        }) {
            assertEquals(400, request("POST", "/api/orders", body).code(), body);
        }
        assertNoOrder();
        assertEquals(12, stockFor(101));
    }

    @Test
    void missingSecondOptionLeavesFirstOptionUntouched() throws Exception {
        assertEquals(
                404,
                request("POST", "/api/orders", TWO_ITEMS.replace("201", "999")).code());
        assertNoOrder();
        assertEquals(12, stockFor(101));
    }

    @Test
    void requestOrderIsPreservedEvenWhenStockIsAcquiredInIdOrder() throws Exception {
        var reply = request("POST", "/api/orders", """
                {"items":[{"optionId":201,"quantity":1,"displayedUnitPrice":29000},
                          {"optionId":101,"quantity":1,"displayedUnitPrice":129000}]}
                """);
        assertEquals(201, reply.code());
        var saved = details(reply.body().path("id").asLong()).body().path("order");
        assertEquals(reply.body(), saved);
        assertEquals(201, saved.path("items").get(0).path("optionId").asLong());
        assertEquals(101, saved.path("items").get(1).path("optionId").asLong());
    }

    @Test
    void differentOptionsOfSameProductRemainSeparateItems() throws Exception {
        var reply = request("POST", "/api/orders", """
                {"items":[{"optionId":101,"quantity":1,"displayedUnitPrice":129000},
                          {"optionId":102,"quantity":2,"displayedUnitPrice":129000}]}
                """);
        assertEquals(201, reply.code());
        assertEquals(387000, reply.body().path("totalAmount").asLong());
        assertEquals(2, reply.body().path("items").size());
        assertEquals(11, stockFor(101));
        assertEquals(6, stockFor(102));
    }

    @Test
    void moreThanTwentyItemsIsRejectedBeforeLookingUpOptions() throws Exception {
        var lines = java.util.stream.IntStream.rangeClosed(1, 21)
                .mapToObj(id -> "{\"optionId\":%d,\"quantity\":1,\"displayedUnitPrice\":1000}".formatted(id))
                .collect(java.util.stream.Collectors.joining(","));
        assertEquals(
                400,
                request("POST", "/api/orders", "{\"items\":[" + lines + "]}").code());
        assertNoOrder();
    }

    private int stockFor(long optionId) {
        return database.queryForObject("select stock from store_option where id = ?", Integer.class, optionId);
    }

    private void assertNoOrder() {
        assertEquals(0, orders.count());
        assertEquals(0, database.queryForObject("select count(*) from store_order_item", Integer.class));
        assertEquals(0, payments.count());
        assertEquals(0, gateway.approvals());
    }
}
