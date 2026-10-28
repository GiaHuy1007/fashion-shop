package cntt.giahuy.fashion_shop.common.event;

import java.time.Instant;

public interface DomainEvent {
    default Instant occurredAt() {
        return Instant.now();
    }
}
