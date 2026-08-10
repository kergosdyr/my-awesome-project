package io.github.kergosdyr.commercelab.domain.order;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class OrderNumberIssuer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    public String issue(Instant placedAt) {
        var entropy = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "ORD-" + DATE_FORMATTER.format(placedAt) + "-" + entropy;
    }
}
