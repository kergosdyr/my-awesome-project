package io.github.kergosdyr.commercelab.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.LongStream;

import io.github.kergosdyr.commercelab.support.error.ApiException;
import io.github.kergosdyr.commercelab.support.error.ErrorType;
import org.junit.jupiter.api.Test;

class CreateOrderCommandTest {

    @Test
    void rejectsMoreThanTwentyDistinctProducts() {
        var lines = LongStream.rangeClosed(1, 21)
                .mapToObj(productId -> new CreateOrderCommand.Line(productId, 1))
                .toList();

        assertThatThrownBy(() -> new CreateOrderCommand("테스터", lines))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_ORDER_LINES)
                );
    }

    @Test
    void rejectsDuplicateProductLines() {
        var lines = List.of(
                new CreateOrderCommand.Line(1L, 1),
                new CreateOrderCommand.Line(1L, 2)
        );

        assertThatThrownBy(() -> new CreateOrderCommand("테스터", lines))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_ORDER_LINES)
                );
    }

    @Test
    void exposesProductIdsInStableLockOrder() {
        var command = new CreateOrderCommand(
                "테스터",
                List.of(
                        new CreateOrderCommand.Line(9L, 1),
                        new CreateOrderCommand.Line(2L, 1),
                        new CreateOrderCommand.Line(5L, 1)
                )
        );

        assertThat(command.productIdsInLockOrder()).containsExactly(2L, 5L, 9L);
    }
}
