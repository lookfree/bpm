package org.jeecg.modules.bpm.controller;

import org.jeecg.modules.bpm.definition.exception.IllegalStateTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class BpmExceptionHandler {

    @ExceptionHandler(IllegalStateTransitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalTransition(IllegalStateTransitionException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", 400);
        result.put("message", e.getMessage());
        return result;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", 400);
        result.put("message", e.getMessage());
        return result;
    }
}
