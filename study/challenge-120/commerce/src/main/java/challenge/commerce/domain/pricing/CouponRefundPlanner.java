package challenge.commerce.domain.pricing;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/**
 * B008 해답: 품목별 비례 할인에서 버려진 나머지가 큰 순서로 남은1원을 배분한다.
 * 동률이면 optionId 오름차순. 품목 안에서는 상품 번호가 빠른 쪽에 환불 잔돈을 배분한다.
 * 실제 주문·PG·재고를 변경하지 않는 계산이다.
 */
@Component
public class CouponRefundPlanner {
    public CouponRefundPlanResult plan(CouponRefundCommand command) {
        long totalAmount = command.lines().stream()
                .mapToLong(line -> Math.multiplyExact(line.unitPrice(), line.quantity()))
                .sum();
        var total = BigInteger.valueOf(totalAmount);
        var coupon = BigInteger.valueOf(command.discountAmount());
        // 1. 비례 할인 몫을 구하고, 남은1원을 받을 우선순위로 정렬한다.
        var shares = command.lines().stream()
                .map(line -> {
                    long lineAmount = Math.multiplyExact(line.unitPrice(), line.quantity());
                    var quotientAndRemainder =
                            coupon.multiply(BigInteger.valueOf(lineAmount)).divideAndRemainder(total);
                    return new DiscountShare(
                            line, lineAmount, quotientAndRemainder[0].longValueExact(), quotientAndRemainder[1]);
                })
                .sorted(Comparator.comparing(DiscountShare::remainder)
                        .reversed()
                        .thenComparingLong(share -> share.line().optionId()))
                .toList();

        long allocatedDiscount =
                shares.stream().mapToLong(DiscountShare::baseDiscount).sum();
        long remainingDiscount = command.discountAmount() - allocatedDiscount;

        // 2. 정렬된 순위로 추가 할인을 확정하고, 개별 상품의 환불액으로 변환한다.
        var results = IntStream.range(0, shares.size())
                .mapToObj(rank -> {
                    var share = shares.get(rank);
                    boolean receivesExtraDiscount = rank < remainingDiscount;
                    long discount = share.baseDiscount() + (receivesExtraDiscount ? 1 : 0);
                    long lineRefund = share.lineAmount() - discount;
                    int quantity = share.line().quantity();
                    long baseRefund = lineRefund / quantity;
                    long remainingWon = lineRefund % quantity;
                    var unitRefunds = IntStream.range(0, quantity)
                            .mapToObj(unit -> {
                                boolean receivesExtraWon = unit < remainingWon;
                                return baseRefund + (receivesExtraWon ? 1 : 0);
                            })
                            .toList();
                    return new RefundLineResult(share.line().optionId(), unitRefunds);
                })
                .toList();
        return new CouponRefundPlanResult(results);
    }

    /** 정수 할인액과 아직 배분하지 못한 소수 부분을 함께 보관하는 중간 계산 결과. */
    private record DiscountShare(CouponLineCommand line, long lineAmount, long baseDiscount, BigInteger remainder) {}
}
