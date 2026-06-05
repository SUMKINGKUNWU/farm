package com.farm.exchange.common;

import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        HttpStatus status = exception.getStatus();
        return ResponseEntity.status(status).body(error(status, exception.getCode(), exception.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiErrorResponse.FieldErrorItem> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError)
                .collect(Collectors.toList());
        String message = fieldErrors.isEmpty() ? "请求参数不合法" : fieldErrors.get(0).getMessage();
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request, fieldErrors));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", badRequestMessage(exception), request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "请求方法不支持", request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, "DATA_INTEGRITY_ERROR", "数据约束冲突，请检查提交内容", request));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccess(DataAccessException exception, HttpServletRequest request) {
        log.error("Database access error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "数据库访问异常，请稍后重试", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected API error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系统异常，请稍后重试", request));
    }

    private ApiErrorResponse.FieldErrorItem fieldError(FieldError fieldError) {
        return new ApiErrorResponse.FieldErrorItem(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private String badRequestMessage(Exception exception) {
        if (exception instanceof MissingServletRequestParameterException) {
            return "缺少请求参数：" + ((MissingServletRequestParameterException) exception).getParameterName();
        }
        if (exception instanceof MissingRequestHeaderException) {
            return "缺少请求头：" + ((MissingRequestHeaderException) exception).getHeaderName();
        }
        if (exception instanceof MethodArgumentTypeMismatchException) {
            return "请求参数类型不正确：" + ((MethodArgumentTypeMismatchException) exception).getName();
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return "请求体格式不正确";
        }
        return "请求参数不合法";
    }

    private ApiErrorResponse error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return error(status, code, message, request, null);
    }

    private ApiErrorResponse error(HttpStatus status, String code, String message, HttpServletRequest request, List<ApiErrorResponse.FieldErrorItem> fieldErrors) {
        return new ApiErrorResponse(code, status.value(), message, request.getRequestURI(), fieldErrors);
    }
}
