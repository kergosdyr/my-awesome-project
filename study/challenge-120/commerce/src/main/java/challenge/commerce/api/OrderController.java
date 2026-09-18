package challenge.commerce.api;

import challenge.commerce.api.request.CreateOrderRequest;
import challenge.commerce.api.response.OrderDetailsResponse;
import challenge.commerce.api.response.OrderPageResponse;
import challenge.commerce.api.response.OrderResponse;
import challenge.commerce.domain.order.*;
import challenge.commerce.domain.query.PageQuery;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    public OrderController(OrderService orderService, OrderQueryService orderQueryService) {
        this.orderService = orderService;
        this.orderQueryService = orderQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return OrderResponse.from(orderService.create(request.toCommand()));
    }

    @GetMapping
    public OrderPageResponse list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return OrderPageResponse.from(orderQueryService.list(new PageQuery(page, size)));
    }

    @GetMapping("/window")
    public challenge.commerce.api.response.OrderWindowResponse window(
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "afterCreatedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant afterCreatedAt,
            @RequestParam(name = "afterId", required = false) Long afterId) {
        if (size < 1 || size > 100) {
            throw challenge.commerce.support.BusinessException.invalid("한 번에 1~100개를 조회하세요.");
        }
        if ((afterCreatedAt == null) != (afterId == null)) {
            throw challenge.commerce.support.BusinessException.invalid("afterCreatedAt과 afterId를 함께 보내세요.");
        }
        var after = afterCreatedAt == null ? null : new OrderCursor(afterCreatedAt, afterId);
        return challenge.commerce.api.response.OrderWindowResponse.from(orderQueryService.window(size, after));
    }

    @GetMapping("/{id}")
    public OrderDetailsResponse find(@PathVariable("id") long id) {
        return OrderDetailsResponse.from(orderQueryService.find(id));
    }
}
