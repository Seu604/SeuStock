package com.seu.seustock.controller;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;

final class ControllerLogSupport {

    private ControllerLogSupport() {
    }

    static List<String> invalidFields(BindingResult result) {
        return result.getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .toList();
    }
}
