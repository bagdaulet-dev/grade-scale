package dev.bagdaulet.gradescale;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiErrors {
  private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

  private ResponseEntity<?> error(int status, String code, String message) {
    return ResponseEntity.status(status)
        .body(Map.of("error", Map.of("code", code, "message", message)));
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<?> business(ApiException e) {
    return error(e.status(), e.code(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    return error(
        422,
        "VALIDATION_ERROR",
        "Check the form: names are required; scores must be 0–100 and weights above 0–100, with at most two decimal places.");
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<?> malformed(Exception e) {
    return error(400, "INVALID_REQUEST", "Send valid JSON, identifiers and numeric values.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<?> missing(Exception e) {
    return error(404, "NOT_FOUND", "This page or resource was not found.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<?> method(Exception e) {
    return error(405, "METHOD_NOT_ALLOWED", "This request method is not supported.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<?> media(Exception e) {
    return error(415, "UNSUPPORTED_MEDIA_TYPE", "Send the request as application/json.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> unexpected(Exception e) {
    log.error("Request failed: {}", e.getClass().getSimpleName());
    return error(
        503, "SERVICE_UNAVAILABLE", "The service is temporarily unavailable. Please try again.");
  }
}
