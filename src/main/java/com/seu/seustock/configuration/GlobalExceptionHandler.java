package com.seu.seustock.configuration;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleForbidden(SecurityException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(RuntimeException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/400";
    }
}
