-- Add teacher_feedback column to code_submissions for manual grading feedback.
-- Used by OpenQuestionGradingService.gradeAnswer() (CODING branch) to persist
-- the teacher's comment alongside the manually-set score.
--
-- IF NOT EXISTS makes this safe even on dev DBs where Hibernate ddl-auto=update
-- already added the column.

ALTER TABLE code_submissions
    ADD COLUMN IF NOT EXISTS teacher_feedback VARCHAR(2000);
