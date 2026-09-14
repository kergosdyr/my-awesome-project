package challenge.payment;

/** 가짜 결제사의 계약. 실제 네트워크·결제는 없으며 Day5 테스트 fake와 Day6 실행용 PG 시뮬레이터가 이 경계를 구현한다. */
public interface PaymentGateway {
    /**
     * 가짜 PG 내부 상태. PROCESSING=접수됐으나 승인 전, APPROVED=승인 완료. 우리 서비스의 PENDING/PAID와 구분한다. 실제 PG 공통 규격을
     * 뜻하지 않는다.
     */
    enum Status {
        PROCESSING,
        APPROVED
    }

    record Receipt(String key, long reservationId, long amount, Status status, String approvalId) {
        public boolean approved() {
            return status == PaymentGateway.Status.APPROVED;
        }
    }

    /**
     * 같은 key+예약+금액은 같은 결제. 같은 key에 다른 예약/금액은 IllegalArgumentException. 첫 호출이 승인 후 응답 유실이면
     * ResponseLostException. 재호출은 기존 결과를 반환한다.
     */
    Receipt approve(String key, long reservationId, long amount);

    /** 없으면 Optional.empty. 조회는 결제를 새로 만들지 않는다. */
    java.util.Optional<Receipt> lookup(String key);

    /** 응답을 받지 못한 통신 사건. 이 가짜 PG에서는 승인 완료 후 발생한다. */
    class ResponseLostException extends RuntimeException {}
}
