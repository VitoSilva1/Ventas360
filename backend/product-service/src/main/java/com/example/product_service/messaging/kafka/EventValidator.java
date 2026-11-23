package com.example.product_service.messaging.kafka;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates consumed payloads against their DTO constraints before they reach the domain.
 */
@Component
public class EventValidator {

    private final Validator validator;

    public EventValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T requireValid(T event) {
        if (event == null) {
            throw new InvalidEventException("Event payload is empty");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new InvalidEventException(event.getClass().getSimpleName() + " is invalid: " + detail);
        }
        return event;
    }
}
