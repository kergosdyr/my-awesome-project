package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import challenge.commerce.domain.order.OrderCursor;
import challenge.commerce.domain.order.OrderQueryService;
import challenge.commerce.domain.order.OrderWindowResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 제공하는 HTTP 입출력만 검사한다. B009 조회 풀이에는 의존하지 않는다. */
class OrderWindowContractTest extends CommerceHttpSupport {
    @MockitoBean
    OrderQueryService orderQueryService;

    @Test
    void firstRequestUsesDefaultSizeAndNoBoundary() throws Exception {
        when(orderQueryService.window(20, null)).thenReturn(new OrderWindowResult(List.of(), null));
        var response = request("GET", "/api/orders/window", "");
        assertEquals(200, response.code());
        assertTrue(response.body().path("entries").isArray());
        assertTrue(response.body().path("next").isNull());
        verify(orderQueryService).window(20, null);
    }

    @Test
    void timestampAndIdAreTypedAndNextIsAnObject() throws Exception {
        var boundary = new OrderCursor(Instant.parse("2026-09-18T10:30:00.123456Z"), 104);
        var next = new OrderCursor(Instant.parse("2026-09-18T10:20:00Z"), 101);
        when(orderQueryService.window(3, boundary)).thenReturn(new OrderWindowResult(List.of(), next));
        var response =
                request("GET", "/api/orders/window?size=3&afterCreatedAt=2026-09-18T10:30:00.123456Z&afterId=104", "");
        assertEquals(200, response.code());
        assertEquals(
                next.createdAt(),
                Instant.parse(response.body().path("next").path("createdAt").asText()));
        assertEquals(next.id(), response.body().path("next").path("id").asLong());
        verify(orderQueryService).window(3, boundary);
    }

    @Test
    void invalidOrIncompleteBoundaryIsRejectedBeforeService() throws Exception {
        for (var query : List.of(
                "afterId=104",
                "afterCreatedAt=2026-09-18T10:30:00Z",
                "afterCreatedAt=not-a-date&afterId=104",
                "afterCreatedAt=2026-09-18T10:30:00Z&afterId=0",
                "afterCreatedAt=2026-09-18T10:30:00Z&afterId=-1",
                "afterCreatedAt=2026-09-18T10:30:00Z&afterId=abc",
                "size=0",
                "size=101")) {
            assertEquals(400, raw("GET", "/api/orders/window?" + query, "").statusCode(), query);
        }
        verifyNoInteractions(orderQueryService);
    }
}
