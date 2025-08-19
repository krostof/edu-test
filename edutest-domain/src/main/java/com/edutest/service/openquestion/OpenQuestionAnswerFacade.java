package com.edutest.service.openquestion;

import com.edutest.domain.assignment.openquestion.CompletionInfo;
import com.edutest.domain.assignment.openquestion.LengthStatus;
import com.edutest.domain.assignment.openquestion.OpenQuestionAnswer;

public class OpenQuestionAnswerFacade {

    private final OpenQuestionAnswerService answerService;
    private final OpenQuestionTextService textService;
    private final OpenQuestionStatusFactory statusFactory;

    public OpenQuestionAnswerFacade(OpenQuestionAnswerService answerService,
                                    OpenQuestionTextService textService,
                                    OpenQuestionStatusFactory statusFactory) {
        this.answerService = answerService;
        this.textService = textService;
        this.statusFactory = statusFactory;
    }

    public boolean isCorrect(OpenQuestionAnswer answer) {
        return answerService.isCorrect(answer);
    }

    public Float calculateScore(OpenQuestionAnswer answer, Integer maxPoints) {
        return answer.getScore();
    }

    public String getAnswerText(OpenQuestionAnswer answer) {
        return answer.getAnswerText();
    }

    public boolean hasAnswer(OpenQuestionAnswer answer) {
        return answer.hasAnswer();
    }

    public boolean isValidAnswer(OpenQuestionAnswer answer, Integer minLength, Integer maxLength) {
        return answerService.isWithinLengthLimits(answer, minLength, maxLength);
    }

    public String getValidationError(OpenQuestionAnswer answer, Integer minLength, Integer maxLength) {
        LengthStatus status = statusFactory.createLengthStatus(answer, minLength, maxLength);
        return status.isValid() ? null : status.getMessage();
    }

    public LengthStatus getLengthStatus(OpenQuestionAnswer answer, Integer minLength, Integer maxLength) {
        return statusFactory.createLengthStatus(answer, minLength, maxLength);
    }

    public CompletionInfo getCompletionInfo(OpenQuestionAnswer answer, Integer minLength) {
        return answerService.calculateCompletion(answer, minLength);
    }

    public String getAnswerPreview(OpenQuestionAnswer answer) {
        return textService.createPreview(answer);
    }

    public String getAnswerPreview(OpenQuestionAnswer answer, int maxLength) {
        return textService.createPreview(answer, maxLength);
    }

    public OpenQuestionAnswer updateTextCounts(OpenQuestionAnswer answer) {
        return textService.updateCounts(answer);
    }
}
