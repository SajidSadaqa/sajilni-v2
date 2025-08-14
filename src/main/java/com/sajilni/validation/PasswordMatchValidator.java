package com.sajilni.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    private String password;
    private String confirmPassword;

    @Override
    public void initialize(PasswordMatch annotation) {
        this.password = annotation.password();
        this.confirmPassword = annotation.confirmPassword();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        BeanWrapperImpl wrapper = new BeanWrapperImpl(obj);
        Object passwordValue = wrapper.getPropertyValue(password);
        Object confirmPasswordValue = wrapper.getPropertyValue(confirmPassword);

        boolean valid = passwordValue != null && passwordValue.equals(confirmPasswordValue);

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(confirmPassword)
                    .addConstraintViolation();
        }

        return valid;
    }
}