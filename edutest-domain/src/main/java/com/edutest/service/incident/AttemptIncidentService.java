package com.edutest.service.incident;

import com.edutest.dto.AttemptIncidentDto;
import com.edutest.dto.RecordIncidentRequestDto;
import com.edutest.persistance.entity.test.AttemptIncidentEntity;
import com.edutest.persistance.entity.test.IncidentTypeEnum;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.repository.AttemptIncidentJpaRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttemptIncidentService {

    /** How long we suppress duplicate incidents of the same type. Stops log spam from rapid events. */
    private static final long DEDUP_WINDOW_SECONDS = 5;

    private final AttemptIncidentJpaRepository incidentRepository;
    private final TestAttemptJpaRepository attemptRepository;

    @Transactional
    public Optional<AttemptIncidentDto> recordIncident(
            Long testId, Long attemptId, Long studentId, RecordIncidentRequestDto request) {

        TestAttemptEntity attempt = attemptRepository.findByIdAndTestId(attemptId, testId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("You do not have access to this test attempt");
        }

        // Don't record incidents for finished attempts (replay attacks, late frontend events).
        if (Boolean.TRUE.equals(attempt.getIsCompleted())) {
            log.debug("Ignoring incident for completed attempt {}", attemptId);
            return Optional.empty();
        }

        IncidentTypeEnum type;
        try {
            type = IncidentTypeEnum.valueOf(request.getType());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown incident type: " + request.getType());
        }

        // Backend-side dedup: if the same type happened within the last DEDUP_WINDOW_SECONDS,
        // skip persisting. Frontend already throttles, this is a second line of defense.
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(DEDUP_WINDOW_SECONDS);
        Optional<AttemptIncidentEntity> recent = incidentRepository
                .findLatestByTypeSince(attemptId, type, cutoff);
        if (recent.isPresent()) {
            log.debug("Deduped incident {} for attempt {} (within {}s window)",
                    type, attemptId, DEDUP_WINDOW_SECONDS);
            return Optional.of(toDto(recent.get()));
        }

        AttemptIncidentEntity entity = AttemptIncidentEntity.builder()
                .testAttempt(attempt)
                .type(type)
                .occurredAt(LocalDateTime.now())
                .metadata(truncate(request.getMetadata(), 1000))
                .build();
        AttemptIncidentEntity saved = incidentRepository.save(entity);

        log.info("Recorded incident {} for attempt {} (student {})", type, attemptId, studentId);
        return Optional.of(toDto(saved));
    }

    @Transactional(readOnly = true)
    public List<AttemptIncidentDto> listIncidents(Long testId, Long attemptId) {
        TestAttemptEntity attempt = attemptRepository.findByIdAndTestId(attemptId, testId)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found"));

        return incidentRepository.findByAttemptIdOrderByOccurredAt(attempt.getId()).stream()
                .map(AttemptIncidentService::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countIncidents(Long attemptId) {
        return incidentRepository.countByAttemptId(attemptId);
    }

    private static AttemptIncidentDto toDto(AttemptIncidentEntity entity) {
        return AttemptIncidentDto.builder()
                .id(entity.getId())
                .attemptId(entity.getTestAttempt().getId())
                .type(entity.getType().name())
                .occurredAt(entity.getOccurredAt())
                .metadata(entity.getMetadata())
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
