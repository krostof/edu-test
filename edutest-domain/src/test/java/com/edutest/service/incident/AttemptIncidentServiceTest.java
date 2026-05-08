package com.edutest.service.incident;

import com.edutest.dto.AttemptIncidentDto;
import com.edutest.dto.RecordIncidentRequestDto;
import com.edutest.persistance.entity.test.AttemptIncidentEntity;
import com.edutest.persistance.entity.test.IncidentTypeEnum;
import com.edutest.persistance.entity.test.TestAttemptEntity;
import com.edutest.persistance.entity.test.TestEntity;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.AttemptIncidentJpaRepository;
import com.edutest.persistance.repository.TestAttemptJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttemptIncidentServiceTest {

    @Mock
    private AttemptIncidentJpaRepository incidentRepository;

    @Mock
    private TestAttemptJpaRepository attemptRepository;

    @InjectMocks
    private AttemptIncidentService service;

    private TestEntity testEntity;
    private TestAttemptEntity attempt;
    private UserEntity student;

    @BeforeEach
    void setUp() {
        testEntity = new TestEntity();
        testEntity.setId(1L);

        student = new UserEntity();
        student.setId(100L);

        attempt = new TestAttemptEntity();
        attempt.setId(10L);
        attempt.setTestEntity(testEntity);
        attempt.setStudent(student);
        attempt.setIsCompleted(false);
    }

    @Nested
    @DisplayName("recordIncident validation")
    class RecordValidationTests {

        @Test
        @DisplayName("Throws when attempt not found")
        void attemptNotFound() {
            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.empty());

            RecordIncidentRequestDto req = RecordIncidentRequestDto.builder().type("TAB_HIDDEN").build();
            assertThatThrownBy(() -> service.recordIncident(1L, 10L, 100L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Throws AccessDenied when student doesn't own attempt")
        void notOwner() {
            UserEntity other = new UserEntity();
            other.setId(999L);
            attempt.setStudent(other);
            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));

            RecordIncidentRequestDto req = RecordIncidentRequestDto.builder().type("TAB_HIDDEN").build();
            assertThatThrownBy(() -> service.recordIncident(1L, 10L, 100L, req))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Throws on unknown type")
        void unknownType() {
            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));

            RecordIncidentRequestDto req = RecordIncidentRequestDto.builder().type("MAGIC").build();
            assertThatThrownBy(() -> service.recordIncident(1L, 10L, 100L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown incident type");
        }

        @Test
        @DisplayName("Throws on null type")
        void nullType() {
            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));

            RecordIncidentRequestDto req = RecordIncidentRequestDto.builder().type(null).build();
            assertThatThrownBy(() -> service.recordIncident(1L, 10L, 100L, req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Returns empty (no save) when attempt is already completed")
        void finishedAttemptIgnored() {
            attempt.setIsCompleted(true);
            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));

            RecordIncidentRequestDto req = RecordIncidentRequestDto.builder().type("TAB_HIDDEN").build();
            Optional<AttemptIncidentDto> result = service.recordIncident(1L, 10L, 100L, req);

            assertThat(result).isEmpty();
            verify(incidentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("recordIncident persistence and dedup")
    class PersistenceTests {

        @Test
        @DisplayName("Happy path: persists with type, occurredAt and metadata")
        void happyPath() {
            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(incidentRepository.findLatestByTypeSince(eq(10L), eq(IncidentTypeEnum.LARGE_PASTE), any()))
                    .thenReturn(Optional.empty());
            when(incidentRepository.save(any())).thenAnswer(inv -> {
                AttemptIncidentEntity e = inv.getArgument(0);
                // Simulate JPA assigning an ID
                setId(e, 1000L);
                return e;
            });

            RecordIncidentRequestDto req = RecordIncidentRequestDto.builder()
                    .type("LARGE_PASTE")
                    .metadata("length=512")
                    .build();
            Optional<AttemptIncidentDto> result = service.recordIncident(1L, 10L, 100L, req);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo("LARGE_PASTE");
            assertThat(result.get().getMetadata()).isEqualTo("length=512");
            assertThat(result.get().getAttemptId()).isEqualTo(10L);

            ArgumentCaptor<AttemptIncidentEntity> captor = ArgumentCaptor.forClass(AttemptIncidentEntity.class);
            verify(incidentRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(IncidentTypeEnum.LARGE_PASTE);
            assertThat(captor.getValue().getOccurredAt()).isNotNull();
            assertThat(captor.getValue().getTestAttempt()).isSameAs(attempt);
        }

        @Test
        @DisplayName("Dedup: returns existing incident when same type within window, no new save")
        void dedupWithinWindow() {
            AttemptIncidentEntity existing = AttemptIncidentEntity.builder()
                    .testAttempt(attempt)
                    .type(IncidentTypeEnum.TAB_HIDDEN)
                    .occurredAt(LocalDateTime.now().minusSeconds(2))
                    .metadata("first")
                    .build();
            setId(existing, 500L);

            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(incidentRepository.findLatestByTypeSince(eq(10L), eq(IncidentTypeEnum.TAB_HIDDEN), any()))
                    .thenReturn(Optional.of(existing));

            RecordIncidentRequestDto req = RecordIncidentRequestDto.builder().type("TAB_HIDDEN").build();
            Optional<AttemptIncidentDto> result = service.recordIncident(1L, 10L, 100L, req);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(500L);
            assertThat(result.get().getMetadata()).isEqualTo("first");
            verify(incidentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Truncates metadata > 1000 chars")
        void truncatesMetadata() {
            String huge = "x".repeat(2000);
            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(incidentRepository.findLatestByTypeSince(any(), any(), any())).thenReturn(Optional.empty());
            when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RecordIncidentRequestDto req = RecordIncidentRequestDto.builder()
                    .type("WINDOW_BLUR")
                    .metadata(huge)
                    .build();
            service.recordIncident(1L, 10L, 100L, req);

            ArgumentCaptor<AttemptIncidentEntity> captor = ArgumentCaptor.forClass(AttemptIncidentEntity.class);
            verify(incidentRepository).save(captor.capture());
            assertThat(captor.getValue().getMetadata()).hasSize(1000);
        }
    }

    @Nested
    @DisplayName("listIncidents")
    class ListTests {

        @Test
        @DisplayName("Returns ordered list mapped to DTOs")
        void returnsOrderedList() {
            AttemptIncidentEntity e1 = AttemptIncidentEntity.builder()
                    .testAttempt(attempt)
                    .type(IncidentTypeEnum.TAB_HIDDEN)
                    .occurredAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            setId(e1, 1L);
            AttemptIncidentEntity e2 = AttemptIncidentEntity.builder()
                    .testAttempt(attempt)
                    .type(IncidentTypeEnum.LARGE_PASTE)
                    .occurredAt(LocalDateTime.now().minusMinutes(2))
                    .metadata("length=300")
                    .build();
            setId(e2, 2L);

            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.of(attempt));
            when(incidentRepository.findByAttemptIdOrderByOccurredAt(10L)).thenReturn(List.of(e1, e2));

            List<AttemptIncidentDto> result = service.listIncidents(1L, 10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getType()).isEqualTo("TAB_HIDDEN");
            assertThat(result.get(1).getType()).isEqualTo("LARGE_PASTE");
            assertThat(result.get(1).getMetadata()).isEqualTo("length=300");
        }

        @Test
        @DisplayName("Throws when attempt not found")
        void notFound() {
            when(attemptRepository.findByIdAndTestId(10L, 1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.listIncidents(1L, 10L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
