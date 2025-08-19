package com.edutest.domain.common;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
}