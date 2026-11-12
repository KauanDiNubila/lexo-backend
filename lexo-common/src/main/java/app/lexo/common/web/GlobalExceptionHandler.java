package app.lexo.common.web;

import app.lexo.common.error.ConflictException;
import app.lexo.common.error.InvalidDataException;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.common.error.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                              HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.paraStatus(HttpStatus.NOT_FOUND), ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidData(InvalidDataException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.paraStatus(HttpStatus.BAD_REQUEST), ex.getMessage(), req);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex,
                                                                   HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.paraStatus(HttpStatus.SERVICE_UNAVAILABLE),
                ex.getMessage(), req);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLITO, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.REQUISICAO_INVALIDA,
                "Par\u00e2metro inv\u00e1lido: " + ex.getName(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Dados inválidos";
        }
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDACAO, message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.REQUISICAO_INVALIDA,
                "Corpo da requisição inválido", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.ACESSO_NEGADO, "Acesso negado", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro nao tratado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.ERRO_INTERNO,
                "Erro interno. Tente novamente.", req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.of(status.value(), code, message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
