package com.javabanker.adapter.in.web;

import com.javabanker.application.port.out.BalanceRepository.BalanceUnavailableException;
import com.javabanker.application.port.out.BankAssistant.AssistantUnavailableException;
import com.javabanker.application.port.out.TransactionPublisher.PublishFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps failures to RFC 9457 problem+json responses. Validation errors are handled by the base class (400). */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidDomainValue(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({PublishFailedException.class, BalanceUnavailableException.class, AssistantUnavailableException.class})
    ProblemDetail temporarilyUnavailable(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
