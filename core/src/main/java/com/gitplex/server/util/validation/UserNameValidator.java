package com.gitplex.server.util.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.gitplex.server.util.validation.annotation.UserName;

public class UserNameValidator implements ConstraintValidator<UserName, String> {
	
	private static Pattern pattern = Pattern.compile("^[\\w-\\.]+$");
	
	public void initialize(UserName constaintAnnotation) {
	}

	public boolean isValid(String value, ConstraintValidatorContext constraintContext) {
		if (value == null) {
			return true;
		} else if (!pattern.matcher(value).find()) {
			constraintContext.disableDefaultConstraintViolation();
			String message = "Only alphanumeric, underscore, dash, and dot are accepted";
			constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
			return false;
		} else {
			return true;
		}
	}
	
}
