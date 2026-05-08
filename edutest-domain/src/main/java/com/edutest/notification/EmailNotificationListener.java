package com.edutest.notification;

import com.edutest.domain.test.Test;
import com.edutest.domain.user.User;
import com.edutest.event.TestAssignedToGroupEvent;
import com.edutest.event.TestAttemptGradedEvent;
import com.edutest.persistance.entity.user.UserEntity;
import com.edutest.persistance.repository.UserRepository;
import com.edutest.service.email.EmailService;
import com.edutest.service.groupservice.StudentGroupService;
import com.edutest.service.testservice.TestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens for domain events and dispatches email notifications.
 *
 * Why these annotations together:
 *  - {@code @TransactionalEventListener(phase = AFTER_COMMIT)}: only fires after the
 *    publishing transaction commits. If the publishing operation rolls back, no email leaks.
 *  - {@code @Async("notificationExecutor")}: runs on a separate thread so the HTTP
 *    request returns immediately. SMTP is slow and unreliable — never block on it.
 *  - {@code @Transactional(readOnly = true, REQUIRES_NEW)}: async runs after commit,
 *    so the original transaction is gone. We need a fresh one to load lazy data.
 *
 * Email bodies live in {@code email-templates/templates.json} — see {@link EmailTemplateRepository}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DASH = "—";

    private final EmailService emailService;
    private final EmailTemplateRepository templates;
    private final TestService testService;
    private final StudentGroupService studentGroupService;
    private final UserRepository userRepository;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onTestAssignedToGroup(TestAssignedToGroupEvent event) {
        try {
            Test test = testService.findById(event.testId());
            List<User> students = studentGroupService.getGroupStudents(event.groupId());
            EmailTemplate template = templates.get("test-assigned-to-group");

            log.info("Notifying {} students about test '{}' assignment to group {}",
                    students.size(), test.getTitle(), event.groupId());

            for (User student : students) {
                Map<String, String> vars = testAssignedVars(student, test);
                emailService.send(student.getEmail(),
                        template.renderSubject(vars),
                        template.renderBody(vars));
            }
        } catch (Exception e) {
            // Listener failures must not bubble up — the event publisher already committed
            log.error("Failed to notify students about test assignment (testId={}, groupId={}): {}",
                    event.testId(), event.groupId(), e.getMessage(), e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onTestAttemptGraded(TestAttemptGradedEvent event) {
        try {
            UserEntity student = userRepository.findById(event.studentId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Student not found: " + event.studentId()));
            Test test = testService.findById(event.testId());
            EmailTemplate template = templates.get("test-graded");

            Map<String, String> vars = testGradedVars(student, test.getTitle(),
                    event.totalScore(), event.maxScore());
            emailService.send(student.getEmail(),
                    template.renderSubject(vars),
                    template.renderBody(vars));
            log.info("Notified student {} about graded attempt {} (score {}/{})",
                    student.getId(), event.attemptId(), event.totalScore(), event.maxScore());
        } catch (Exception e) {
            log.error("Failed to notify student about graded attempt (attemptId={}, studentId={}): {}",
                    event.attemptId(), event.studentId(), e.getMessage(), e);
        }
    }

    private static Map<String, String> testAssignedVars(User student, Test test) {
        Map<String, String> v = new HashMap<>();
        v.put("firstName", student.getFirstName());
        v.put("testTitle", test.getTitle());
        v.put("testDescription", nullableText(test.getDescription()));
        v.put("startDate", formatDate(test.getStartDate()));
        v.put("endDate", formatDate(test.getEndDate()));
        v.put("timeLimit", test.getTimeLimit() != null ? test.getTimeLimit() + " min" : DASH);
        return v;
    }

    private static Map<String, String> testGradedVars(UserEntity student, String testTitle,
                                                       Float totalScore, Float maxScore) {
        Map<String, String> v = new HashMap<>();
        v.put("firstName", student.getFirstName());
        v.put("testTitle", testTitle);
        v.put("totalScore", formatScore(totalScore));
        v.put("maxScore", formatScore(maxScore));
        return v;
    }

    private static String nullableText(String s) {
        return (s == null || s.isBlank()) ? DASH : s;
    }

    private static String formatDate(LocalDateTime ts) {
        return ts != null ? ts.format(DATE_FMT) : DASH;
    }

    private static String formatScore(Float score) {
        if (score == null) return DASH;
        if (score == score.intValue()) return String.valueOf(score.intValue());
        return String.format("%.1f", score);
    }
}
