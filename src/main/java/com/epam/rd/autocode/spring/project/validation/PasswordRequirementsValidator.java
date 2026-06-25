package com.epam.rd.autocode.spring.project.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordRequirementsValidator {

    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isBlank()) {
            errors.add("password.required");
            return errors;
        }

        if (password.length() < 8) {
            errors.add("password.min");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            errors.add("password.uppercase");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            errors.add("password.lowercase");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            errors.add("password.digit");
        }

        return errors;
    }
}
