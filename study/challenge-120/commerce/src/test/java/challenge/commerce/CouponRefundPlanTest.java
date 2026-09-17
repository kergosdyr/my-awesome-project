package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.domain.pricing.*;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** 금액의 합·범위·비례 배분을 검사한다. 잔돈을 어느 옵션에 먼저 줄지는 강요하지 않는다. */
class CouponRefundPlanTest {
    private final CouponRefundPlanner planner = new CouponRefundPlanner();

    @Test
    @Tag("minimum")
    void noCouponKeepsEveryUnitPrice() {
        var lines = List.of(new CouponLineCommand(101, 1000, 3), new CouponLineCommand(201, 2500, 2));
        var plan = checked(lines, 0);
        assertEquals(List.of(1000L, 1000L, 1000L), byOption(plan).get(101L));
        assertEquals(List.of(2500L, 2500L), byOption(plan).get(201L));
    }

    @Test
    @Tag("minimum")
    void couponRemainderIsNotLostWhenThreeUnitsAreCancelledSeparately() {
        var plan = checked(List.of(new CouponLineCommand(101, 1000, 3)), 100);
        assertEquals(
                2900,
                byOption(plan).get(101L).stream().mapToLong(Long::longValue).sum());
    }

    @Test
    @Tag("minimum")
    void unequalLinePricesReceiveProportionalDiscounts() {
        var plan = checked(List.of(new CouponLineCommand(101, 1000, 1), new CouponLineCommand(201, 3000, 1)), 1000);
        assertEquals(List.of(750L), byOption(plan).get(101L));
        assertEquals(List.of(2250L), byOption(plan).get(201L));
    }

    @Test
    @Tag("full")
    void fullDiscountLeavesNoRefund() {
        var plan = checked(List.of(new CouponLineCommand(101, 333, 3), new CouponLineCommand(201, 1, 1)), 1000);
        assertTrue(plan.lines().stream()
                .flatMap(line -> line.unitRefundAmounts().stream())
                .allMatch(amount -> amount == 0));
    }

    @Test
    @Tag("full")
    void tiedRemaindersHaveStableAssignmentRegardlessOfInputOrder() {
        var a = new CouponLineCommand(101, 1000, 3);
        var b = new CouponLineCommand(201, 1000, 3);
        var c = new CouponLineCommand(301, 1000, 3);
        var first = checked(List.of(a, b, c), 101);
        assertEquals(byOption(first), byOption(checked(List.of(c, a, b), 101)));
        assertEquals(byOption(first), byOption(checked(List.of(a, b, c), 101)));
    }

    @Test
    @Tag("full")
    void tinyPricesCannotReceiveNegativeRefunds() {
        checked(
                List.of(
                        new CouponLineCommand(101, 1, 1),
                        new CouponLineCommand(201, 1, 5),
                        new CouponLineCommand(301, 99, 1)),
                104);
    }

    @Test
    @Tag("full")
    void intermediateMultiplicationMustNotOverflowLong() {
        checked(
                List.of(
                        new CouponLineCommand(101, 1_000_000_000_000L, 5),
                        new CouponLineCommand(201, 999_999_999_999L, 5)),
                9_000_000_000_001L);
    }

    @Test
    @Tag("full")
    void groupingTheSameUnitCancellationsDoesNotChangeTheirTotal() {
        var values = byOption(checked(List.of(new CouponLineCommand(101, 1000, 5)), 103))
                .get(101L);
        long separately = values.stream().mapToLong(Long::longValue).sum();
        long firstTwo = values.get(0) + values.get(1);
        long lastThree = values.get(2) + values.get(3) + values.get(4);
        assertEquals(4897, separately);
        assertEquals(separately, firstTwo + lastThree);
    }

    @Test
    @Tag("full")
    void plannerDoesNotMutateInputOrder() {
        var lines = new ArrayList<>(List.of(new CouponLineCommand(201, 101, 2), new CouponLineCommand(101, 303, 1)));
        var original = List.copyOf(lines);
        checked(lines, 7);
        assertEquals(original, lines);
    }

    private CouponRefundPlanResult checked(List<CouponLineCommand> lines, long coupon) {
        var result = planner.plan(new CouponRefundCommand(lines, coupon));
        assertNotNull(result);
        var actual = byOption(result);
        assertEquals(lines.size(), result.lines().size());
        assertEquals(lines.stream().map(CouponLineCommand::optionId).collect(Collectors.toSet()), actual.keySet());
        long originalTotal = lines.stream()
                .mapToLong(line -> line.unitPrice() * line.quantity())
                .sum();
        long refundTotal = 0;
        for (var line : lines) {
            var units = actual.get(line.optionId());
            assertEquals(line.quantity(), units.size());
            assertTrue(units.stream().allMatch(amount -> amount != null && amount >= 0 && amount <= line.unitPrice()));
            assertTrue(Collections.max(units) - Collections.min(units) <= 1, "같은 품목 상품끼리 차이는 최대1원");
            long lineRefund = units.stream().mapToLong(Long::longValue).sum();
            long original = line.unitPrice() * line.quantity();
            var division = BigInteger.valueOf(coupon)
                    .multiply(BigInteger.valueOf(original))
                    .divideAndRemainder(BigInteger.valueOf(originalTotal));
            long floor = division[0].longValueExact();
            long ceil = floor + (division[1].signum() == 0 ? 0 : 1);
            assertTrue(original - lineRefund == floor || original - lineRefund == ceil, "품목별 할인은 정확한 비례 몫의 내림/올림 범위");
            refundTotal += lineRefund;
        }
        assertEquals(originalTotal - coupon, refundTotal, "1원도 잃거나 더 돌려주지 않는다");
        return result;
    }

    private Map<Long, List<Long>> byOption(CouponRefundPlanResult plan) {
        return plan.lines().stream()
                .collect(Collectors.toMap(RefundLineResult::optionId, RefundLineResult::unitRefundAmounts));
    }
}
