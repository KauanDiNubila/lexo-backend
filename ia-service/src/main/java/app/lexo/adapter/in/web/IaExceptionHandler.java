package app.lexo.adapter.in.web;

import app.lexo.common.web.ErrorCode;
import app.lexo.common.web.ErrorResponse;
import app.lexo.domain.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class IaExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ErrorResponse> rateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), ErrorCode.REQUISICOES_EM_EXCESSO, ex.getMessage(),
                        request.getRequestURI()));
    }
}
