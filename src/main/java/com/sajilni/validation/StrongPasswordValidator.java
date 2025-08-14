package com.sajilni.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigits;
    private boolean requireSpecialChars;

    @Override
    public void initialize(StrongPassword annotation) {
        this.minLength = annotation.minLength();
        this.requireUppercase = annotation.requireUppercase();
        this.requireLowercase = annotation.requireLowercase();
        this.requireDigits = annotation.requireDigits();
        this.requireSpecialChars = annotation.requireSpecialChars();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(password)) {
            return false;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        // Check minimum length
        if (password.length() < minLength) {
            context.buildConstraintViolationWithTemplate("{validation.password.too.short}")
                    .addConstraintViolation();
            valid = false;
        }

        // Check uppercase requirement
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            context.buildConstraintViolationWithTemplate("{validation.password.no.uppercase}")
                    .addConstraintViolation();
            valid = false;
        }

        // Check lowercase requirement
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            context.buildConstraintViolationWithTemplate("{validation.password.no.lowercase}")
                    .addConstraintViolation();
            valid = false;
        }

        // Check digits requirement
        if (requireDigits && !password.matches(".*\\d.*")) {
            context.buildConstraintViolationWithTemplate("{validation.password.no.digits}")
                    .addConstraintViolation();
            valid = false;
        }

        // Check special characters requirement
        if (requireSpecialChars && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            context.buildConstraintViolationWithTemplate("{validation.password.no.special}")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}