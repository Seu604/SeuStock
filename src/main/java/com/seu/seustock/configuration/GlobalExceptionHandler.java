package com.seu.seustock.configuration;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException ex,
                                 @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                                 Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        if (isHtmxRequest(hxRequest)) {
            model.addAttribute("statusCode", 404);
            model.addAttribute("errorTitle", "항목을 찾을 수 없습니다");
            return "fragments/error-modal :: modal";
        }
        return "error/404";
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleForbidden(SecurityException ex,
                                  @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                                  Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        if (isHtmxRequest(hxRequest)) {
            model.addAttribute("statusCode", 403);
            model.addAttribute("errorTitle", "접근할 수 없습니다");
            return "fragments/error-modal :: modal";
        }
        return "error/403";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(RuntimeException ex,
                                   @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                                   Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        if (isHtmxRequest(hxRequest)) {
            model.addAttribute("statusCode", 400);
            model.addAttribute("errorTitle", "요청을 처리할 수 없습니다");
            return "fragments/error-modal :: modal";
        }
        return "error/400";
    }

    private boolean isHtmxRequest(String hxRequest) {
        return "true".equalsIgnoreCase(hxRequest);
    }
}
