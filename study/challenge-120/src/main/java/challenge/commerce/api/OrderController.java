package challenge.commerce.api;

import challenge.commerce.domain.order.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
    public PurchaseOrder create(@Valid @RequestBody CreateRequest request) {
        return orders.create(new OrderService.CreateOrder(request.optionId(), request.quantity()));
    }

    @GetMapping
    public List<OrderQueryService.OrderDetails> list() {
        return queries.list();
    }

    @GetMapping("/{id}")
    public OrderQueryService.OrderDetails find(@PathVariable("id") long id) {
        return queries.find(id);
    }

    public record CreateRequest(@Positive long optionId, @Min(1) @Max(5) int quantity) {}
}
