package com.edutest.domain.common;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
public abstract class DomainEntity {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    protected DomainEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 0L;
    }

    protected DomainEntity(Long id) {
        this();
        this.id = id;
    }

    public void markAsUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isNew() {
        return id == null;
    }

    public boolean hasId() {
        return id != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEntity that = (DomainEntity) o;
        // Two entities are equal if they have the same non-null id
        if (id == null || that.id == null) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Use a constant for entities without id to ensure consistency
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }
}