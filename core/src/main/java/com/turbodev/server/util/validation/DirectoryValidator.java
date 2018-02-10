package com.turbodev.server.util.validation;

import java.io.File;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.turbodev.utils.FileUtils;
import com.turbodev.server.util.validation.annotation.Directory;
import com.turbodev.launcher.bootstrap.Bootstrap;

public class DirectoryValidator implements ConstraintValidator<Directory, String> {

	private Directory annotation;
	
	public void initialize(Directory constaintAnnotation) {
		annotation = constaintAnnotation;
	}

	public boolean isValid(String value, ConstraintValidatorContext constraintContext) {
		try {
			if (value == null)
				return true;

			File dir = new File(value);
			if (annotation.absolute()) {
				if (!dir.isAbsolute()) {
					constraintContext.disableDefaultConstraintViolation();
					constraintContext.buildConstraintViolationWithTemplate("Please specify an absolute directory").addConstraintViolation();
					return false;
				}
			}
			if (annotation.writeable()) {
				if (!FileUtils.isWritable(dir)) {
					constraintContext.disableDefaultConstraintViolation();
					constraintContext.buildConstraintViolationWithTemplate("Directory is not writeable").addConstraintViolation();
					return false;
				}
			}
			if (annotation.outsideOfInstallDir()) {
				if (dir.getCanonicalFile().toPath().startsWith(Bootstrap.installDir.toPath())) {
					constraintContext.disableDefaultConstraintViolation();
					constraintContext.buildConstraintViolationWithTemplate("Please specify a directory outside of the installation directory").addConstraintViolation();
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			constraintContext.disableDefaultConstraintViolation();
			constraintContext.buildConstraintViolationWithTemplate("Invalid directory").addConstraintViolation();
			return false;
		}
	}
}
