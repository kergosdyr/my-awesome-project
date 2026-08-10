package io.github.kergosdyr.commercelab.api.eventlab;

import static io.github.kergosdyr.commercelab.api.eventlab.request.PublicationStrategyParameter.toDomain;

import io.github.kergosdyr.commercelab.api.eventlab.request.CreateLabOrderRequest;
import io.github.kergosdyr.commercelab.api.eventlab.response.EventLabMetricsResponse;
import io.github.kergosdyr.commercelab.api.eventlab.response.LabOrderResponse;
import io.github.kergosdyr.commercelab.domain.eventlab.LabResetResult;
import io.github.kergosdyr.commercelab.domain.eventlab.OrderEventLabService;
import io.github.kergosdyr.commercelab.support.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labs/events")
public class EventLabController {

    private final OrderEventLabService eventLabService;

    public EventLabController(OrderEventLabService eventLabService) {
        this.eventLabService = eventLabService;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<LabOrderResponse>> placeOrder(
            @RequestParam String strategy,
            @Valid @RequestBody CreateLabOrderRequest request
    ) {
        var result = eventLabService.placeOrder(request.toCommand(), toDomain(strategy));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(LabOrderResponse.from(result)));
    }

    @GetMapping("/metrics")
    public ApiResponse<EventLabMetricsResponse> readMetrics() {
        return ApiResponse.success(EventLabMetricsResponse.from(eventLabService.readMetrics()));
    }

    @PostMapping("/reset")
    public ApiResponse<LabResetResult> resetFixtures() {
        return ApiResponse.success(eventLabService.resetFixtures());
    }
}
