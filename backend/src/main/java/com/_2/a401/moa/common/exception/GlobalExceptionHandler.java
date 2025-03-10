package com._2.a401.moa.common.exception;

import com._2.a401.moa.auth.exception.AuthException;
import com._2.a401.moa.member.controller.MemberController;
import com.amazonaws.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;

import static com._2.a401.moa.common.exception.ExceptionCode.INVALID_REQUEST;
import static com._2.a401.moa.common.exception.ExceptionCode.SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        final MethodArgumentNotValidException e,
        final HttpHeaders headers,
        final HttpStatusCode status,
        final WebRequest request
    ) {
        log.warn(e.getMessage(), e);
        String errorMessage = Objects.requireNonNull(e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        return ResponseEntity.badRequest()
            .body(new ExceptionResponse(INVALID_REQUEST, errorMessage));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ExceptionResponse> handleAuthException(final AuthException e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.status(UNAUTHORIZED)
                .body(new ExceptionResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MoaException.class)
    public ResponseEntity<ExceptionResponse> handleMoaException(final MoaException e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.badRequest()
            .body(new ExceptionResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(final Exception e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.internalServerError()
            .body(new ExceptionResponse(SERVER_ERROR));
    }
}
