package com.fizzycoyotestudio.eventforge.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = {DashboardController.class, EventBuilderController.class, GamePlayController.class})
public class MvcExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MvcExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(IllegalArgumentException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDataIntegrity(DataIntegrityViolationException ex, Model model) {
        model.addAttribute("message", "Database conflict: " + ex.getMostSpecificCause().getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleGeneric(Exception ex, Model model) {
        log.error("Unhandled exception in MVC", ex);
        model.addAttribute("message", "Something went wrong: " + ex.getMessage());
        return "error";
    }
}
