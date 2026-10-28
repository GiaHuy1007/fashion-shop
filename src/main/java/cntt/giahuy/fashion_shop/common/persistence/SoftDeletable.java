package cntt.giahuy.fashion_shop.common.persistence;

import java.time.Instant;

public interface SoftDeletable {

    Instant getDeletedAt();

    void setDeletedAt(Instant deletedAt);

    default boolean isDeleted() {
        return getDeletedAt() != null;
    }

    default void markDeleted() {
        setDeletedAt(Instant.now());
    }

    default void restore() {
        setDeletedAt(null);
    }
}
