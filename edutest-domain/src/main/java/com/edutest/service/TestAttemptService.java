package com.edutest.service;

import com.edutest.domain.test.Test;
import com.edutest.domain.test.TestAttempt;
import com.edutest.domain.user.User;

import java.time.LocalDateTime;

public class TestAttemptService {

    public TestAttempt startTestAttempt(Test test, User student) {
        validateTestCanBeStarted(test, student);
        
        return TestAttempt.builder()
                .test(test)
                .student(student)
                .startedAt(LocalDateTime.now())
                .isCompleted(false)
                .build();
    }

    public void finishTestAttempt(TestAttempt attempt, Float finalScore) {
        if (attempt.isFinished()) {
            throw new IllegalStateException("Test attempt is already finished");
        }
        
        attempt.finish(finalScore);
    }

    public void forceFinishDueToTimeLimit(TestAttempt attempt) {
        if (!attempt.isTimeExpired()) {
            throw new IllegalStateException("Cannot force finish - time limit not exceeded");
        }
        
        attempt.finish();
    }

    public boolean canStudentStartTest(Test test, User student) {
        try {
            validateTestCanBeStarted(test, student);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canStudentResumeAttempt(TestAttempt attempt, User student) {
        return attempt.getStudent().equals(student) && 
               attempt.canBeResumed();
    }

    private void validateTestCanBeStarted(Test test, User student) {
        if (!student.isStudent()) {
            throw new IllegalArgumentException("Only students can start test attempts");
        }
        
        if (!test.isActive()) {
            throw new IllegalStateException("Test is not currently active");
        }
        
        if (!test.isAvailableForStudent(student)) {
            throw new IllegalStateException("Student is not assigned to this test");
        }
        
        if (test.hasStudentStartedAttempt(student)) {
            throw new IllegalStateException("Student has already started this test");
        }
    }

    public TestAttempt.AttemptResult validateAndPrepareAttempt(Test test, User student) {
        if (!canStudentStartTest(test, student)) {
            return new TestAttempt.AttemptResult(false, "Cannot start test");
        }
        
        TestAttempt existingAttempt = test.getStudentAttempt(student);
        if (existingAttempt != null && existingAttempt.canBeResumed()) {
            return new TestAttempt.AttemptResult(true, "Resume existing attempt", existingAttempt);
        }
        
        TestAttempt newAttempt = startTestAttempt(test, student);
        return new TestAttempt.AttemptResult(true, "New attempt created", newAttempt);
    }
}