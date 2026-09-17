package challenge.commerce.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CouponRefundRequest(@NotNull @PositiveOrZero Long discountAmount) {}
