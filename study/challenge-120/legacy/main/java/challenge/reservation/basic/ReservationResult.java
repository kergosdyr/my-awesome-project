package challenge.reservation.basic;

public record ReservationResult(Status status, Long reservationId) {
    public enum Status { CREATED, EXISTING, SOLD_OUT }
}
