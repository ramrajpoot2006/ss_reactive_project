package com.domainname.next.shippingapi.exception;

import com.domainname.next.shippingapi.constant.ErrorConstants;


import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import org.zalando.problem.Problem;
import org.zalando.problem.StatusType;
import org.zalando.problem.spring.webflux.advice.ProblemHandling;
import org.zalando.problem.violations.Violation;

import com.domainname.next.shippingapi.resources.response.ProblemResponse;
import com.domainname.next.shippingapi.util.MessageHelper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler implements ProblemHandling {
  private static final String REQUEST_BODY_MISSING = "body is missing";
  private static final String JSON_DECODING_ERROR = "JSON decoding error";
  private final MessageHelper messageHelper;

  public CustomExceptionHandler(MessageHelper messageHelper) {
    this.messageHelper = messageHelper;
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<Problem>> handleCustomException(Exception e, final ServerWebExchange request) {
    log.error("{} status code {} ", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, e);
    String errDetails = messageHelper.buildMessage(ErrorConstants.INTERNAL_ERROR_CODE);
    return Mono.just(ProblemResponse.internalServerError(request, errDetails));
  }

  @Override
  public Mono<ResponseEntity<Problem>> newConstraintViolationProblem(final Throwable throwable,
      final Collection<Violation> stream, final ServerWebExchange request) {
    log.error("{} error status code {}", throwable.getMessage(), HttpStatus.BAD_REQUEST, throwable);
    String errDetails;
    List<Violation> violations = stream.stream()
        .sorted(Comparator.comparing(Violation::getField).thenComparing(Violation::getMessage))
        .collect(Collectors.toList());
    var violation = violations.stream().findFirst().orElse(null);
    if (Objects.nonNull(violation)) {
      return getProblemResponse(violation, request);
    } else {
      errDetails = messageHelper.buildMessage(ErrorConstants.INTERNAL_ERROR_CODE);
      return Mono.just(ProblemResponse.internalServerError(request, errDetails));
    }
  }

  private Mono<ResponseEntity<Problem>> getProblemResponse(Violation violation, ServerWebExchange request) {
    String errDetails;
    String field = violation.getField();
    String errorCode = violation.getMessage();
    errDetails = messageHelper.buildMessage(errorCode, field);
    if (errorCode.equals(ErrorConstants.INVALID_FIELD_CODE)) {
      return Mono.just(ProblemResponse.unprocessableEntity(request, errDetails));
    } else if (errorCode.equals(ErrorConstants.DUPLICATE_CARRIER_STRING)) {
      return Mono.just(ProblemResponse.unprocessableEntity(request, errDetails));
    } else {
      return Mono.just(ProblemResponse.badRequest(request, errDetails));
    }
  }

  @ExceptionHandler(NotFoundException.class)
  public Mono<ResponseEntity<Problem>> handleNotFoundException(NotFoundException e, ServerWebExchange request) {
    String errDetails = messageHelper.buildMessage(e.getMessageCode(), e.getArgs());
    return Mono.just(ProblemResponse.notFound(request, errDetails));
  }

  @Override
  public Mono<ResponseEntity<Problem>> create(final StatusType status, final Throwable throwable,
      final ServerWebExchange request, final HttpHeaders headers) {
    log.error(throwable.getMessage());
    String[] message = throwable.getMessage().split(":");
    String errDetails = throwable.getMessage();
    if (message.length > 1) {
      if (throwable.getMessage().contains(REQUEST_BODY_MISSING)) {
        errDetails = messageHelper.buildMessage(ErrorConstants.REQUEST_BODY_MISSING_CODE);
      } else if (throwable.getMessage().contains(JSON_DECODING_ERROR)) {
        errDetails = messageHelper.buildMessage(ErrorConstants.INVALID_JSON_CODE);
      }
    }
    return Mono.just(ProblemResponse.problemResponseEntity(request, status, errDetails));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public Mono<ResponseEntity<Problem>> handleConflict(Exception e, final ServerWebExchange request) {
    log.error("{} message with status code {}", e.getMessage(), HttpStatus.CONFLICT, e);
    String errDetails = messageHelper.buildMessage(ErrorConstants.CONSTRAINT_VIOLATION_CODE);
    return Mono.just(ProblemResponse.conflict(request, errDetails));
  }
  
  @ExceptionHandler(ValidationException.class)
  public Mono<ResponseEntity<Problem>> handleValidationException(ValidationException e, ServerWebExchange request) {
    log.error("{} status error code {}", e.getMessage(), e.getMessageCode(), e);
    String errDetails = messageHelper.buildMessage(e.getMessageCode(), e.getArgs());
    return Mono.just(ProblemResponse.unprocessableEntity(request, errDetails));
  }
  
  @ExceptionHandler(CarrierStringsValidationException.class)
  public Mono<ResponseEntity<Problem>> handleCarrierStringValidationException(CarrierStringsValidationException e, ServerWebExchange request) {
    log.error("{} status error code {}", e.getMessage(), HttpStatus.CONFLICT, e);
    String errDetails = messageHelper.buildMessage(e.getMessageCode(), e.getArgs());
    return Mono.just(ProblemResponse.conflict(request, errDetails));
  }
}
