package io.github.kergosdyr.commercelab.api.order;

import io.github.kergosdyr.commercelab.api.order.request.CreateOrderRequest;
import io.github.kergosdyr.commercelab.api.order.response.CreateOrderResponse;
import io.github.kergosdyr.commercelab.domain.order.OrderService;
import io.github.kergosdyr.commercelab.support.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponse>> placeOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        var order = orderService.placeOrder(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(CreateOrderResponse.fromResult(order)));
    }
}
