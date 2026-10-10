package edu.sjsu.cmpe172.tutoringappointments.controller;

import edu.sjsu.cmpe172.tutoringappointments.service.BookingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class ExceptionHandlerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<?> booking(BookingException exception) {
        return error(exception.status(), exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> notFound(Exception exception) {
        return error(404, "Resource not found.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> method(Exception exception) {
        return error(405, "HTTP method not supported.");
    }

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<?> input(Exception exception) {
        return error(400, "Invalid or missing request input.");
    }

    @ExceptionHandler({
        DataIntegrityViolationException.class,
        PessimisticLockingFailureException.class
    })
    public ResponseEntity<?> conflict(Exception exception) {
        return error(409, "The slot changed or conflicts with an existing booking. Refresh and try again.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception exception) {
        logger.error("Request failed", exception);

        return error(500, "The request could not be completed.");
    }

    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("status", status, "message", message));
    }
}
