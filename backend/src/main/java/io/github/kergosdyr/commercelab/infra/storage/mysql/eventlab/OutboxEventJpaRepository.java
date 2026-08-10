package io.github.kergosdyr.commercelab.infra.storage.mysql.eventlab;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from OutboxEventJpaEntity event "
            + "where event.publishedAt is null order by event.occurredAt, event.eventId")
    List<OutboxEventJpaEntity> findPendingForUpdate(Pageable pageable);

    long countByPublishedAtIsNull();

    Optional<OutboxEventJpaEntity> findFirstByPublishedAtIsNullOrderByOccurredAtAscEventIdAsc();

    @Query("select coalesce(sum(event.publishFailures), 0) from OutboxEventJpaEntity event")
    long sumPublishFailures();
}
