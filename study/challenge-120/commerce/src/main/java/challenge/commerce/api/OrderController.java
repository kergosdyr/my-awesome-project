package challenge.commerce.api;

import challenge.commerce.api.request.CreateOrderRequest;
import challenge.commerce.api.response.OrderDetailsResponse;
import challenge.commerce.api.response.OrderResponse;
import challenge.commerce.domain.order.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;
    private final OrderQueryService queries;

    public OrderController(OrderService orders, OrderQueryService queries) {
        this.orders = orders;
        this.queries = queries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return OrderResponse.from(orders.create(request.toCommand()));
    }

    @GetMapping
    public List<OrderDetailsResponse> list() {
        return queries.list().stream().map(OrderDetailsResponse::from).toList();
    }

    @GetMapping("/window")
    public challenge.commerce.api.response.OrderWindowResponse window(
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "after", required = false) String after) {
        if (size < 1 || size > 100) {
            throw challenge.commerce.support.BusinessException.invalid("한 번에 1~100개를 조회하세요.");
        }
        return challenge.commerce.api.response.OrderWindowResponse.from(queries.window(size, after));
    }

    @GetMapping("/{id}")
    public OrderDetailsResponse find(@PathVariable("id") long id) {
        return OrderDetailsResponse.from(queries.find(id));
    }
}
