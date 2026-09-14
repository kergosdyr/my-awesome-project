package challenge.payment;

import jakarta.persistence.*;

/** 결제 대상의 존재 확인을 위한 제공 데이터. 예약 생성·재고 로직은 오늘 과제가 아니다. */
@Entity
@Table(name = "reservation")
public class Reservation {
    @Id private Long id;

    protected Reservation() {}

    public Reservation(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
