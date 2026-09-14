package challenge.lab.infra;

import challenge.payment.Reservation;
import challenge.payment.ReservationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LabFixture implements CommandLineRunner {
    private final ReservationRepository reservations;

    public LabFixture(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public void run(String... args) {
        for (long id : new long[] {1, 2, 3}) {
            if (!reservations.existsById(id)) reservations.save(new Reservation(id));
        }
    }
}
