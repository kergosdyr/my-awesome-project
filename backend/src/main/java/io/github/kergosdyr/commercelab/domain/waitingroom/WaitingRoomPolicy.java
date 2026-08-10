package io.github.kergosdyr.commercelab.domain.waitingroom;

import java.time.Duration;

public interface WaitingRoomPolicy {

    int maxConcurrency();

    Duration workDuration();

    Duration ticketTtl();

    Duration admissionTtl();

    Duration processingLeaseTtl();

    Duration promotionReadinessTtl();

    Duration pollInterval();
}
