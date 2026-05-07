package com.edutest.persistance.entity.test;

import com.edutest.persistance.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attempt_incidents", indexes = {
        @Index(name = "idx_attempt_incidents_attempt", columnList = "test_attempt_id"),
        @Index(name = "idx_attempt_incidents_attempt_type", columnList = "test_attempt_id,type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptIncidentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_attempt_id", nullable = false)
    private TestAttemptEntity testAttempt;

    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private IncidentTypeEnum type;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** Optional context (e.g., paste length, current question index). Free-form text or JSON. */
    @Column(name = "metadata", length = 1000)
    private String metadata;
}
