package app.lexo.adapter.in.web;

import app.lexo.common.web.ErrorCode;
import app.lexo.common.web.ErrorResponse;
import app.lexo.domain.EmailAlreadyInUseException;
import app.lexo.domain.ExpiredRefreshTokenException;
import app.lexo.domain.InvalidCredentialsException;
import app.lexo.domain.InvalidRefreshTokenException;
import app.lexo.domain.InvalidTwoFactorCodeException;
import app.lexo.domain.PasswordMismatchException;
import app.lexo.domain.TwoFactorRequiredException;
import app.lexo.domain.UnknownSessionUserException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> invalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ErrorCode.CREDENCIAIS_INVALIDAS, ex, request);
    }

    @ExceptionHandler(TwoFactorRequiredException.class)
    ResponseEntity<ErrorResponse> twoFactorRequired(TwoFactorRequiredException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ErrorCode.TOTP_REQUIRED, ex, request);
    }

    @ExceptionHandler(InvalidTwoFactorCodeException.class)
    ResponseEntity<ErrorResponse> invalidTwoFactorCode(InvalidTwoFactorCodeException ex,
                                                       HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ErrorCode.TOTP_INVALID, ex, request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ErrorResponse> invalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ErrorCode.REFRESH_INVALIDO, ex, request);
    }

    @ExceptionHandler(ExpiredRefreshTokenException.class)
    ResponseEntity<ErrorResponse> expiredRefreshToken(ExpiredRefreshTokenException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ErrorCode.REFRESH_EXPIRADO, ex, request);
    }

    @ExceptionHandler(UnknownSessionUserException.class)
    ResponseEntity<ErrorResponse> unknownSessionUser(UnknownSessionUserException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ErrorCode.NAO_AUTORIZADO, ex, request);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    ResponseEntity<ErrorResponse> emailAlreadyInUse(EmailAlreadyInUseException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, ErrorCode.EMAIL_EM_USO, ex, request);
    }

    @ExceptionHandler(PasswordMismatchException.class)
    ResponseEntity<ErrorResponse> passwordMismatch(PasswordMismatchException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.SENHAS_NAO_COINCIDEM, ex, request);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, RuntimeException ex,
                                                  HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), code, ex.getMessage(), request.getRequestURI()));
    }
}
