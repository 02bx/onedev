package com.pmease.commons.wicket.editor;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;

import com.pmease.commons.editable.BeanDescriptor;
import com.pmease.commons.loader.AppLoader;

@SuppressWarnings("serial")
public abstract class BeanEditor<T> extends ValueEditor<T> {

	private final BeanDescriptor<T> beanDescriptor;
	
	public BeanEditor(String id, BeanDescriptor<T> beanDescriptor, IModel<T> model) {
		super(id, model);
		
		this.beanDescriptor = beanDescriptor;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new IValidator<T>() {

			@Override
			public void validate(IValidatable<T> validatable) {
				Validator validator = AppLoader.getInstance(Validator.class);
				for (ConstraintViolation<T> violation: validator.validate(validatable.getValue())) {
					ValuePath valuePath = new ValuePath(violation.getPropertyPath());
					ErrorContext errorContext = getErrorContext(valuePath);
					errorContext.addError(violation.getMessage());
				}
			}
			
		});
	}
	
	public BeanDescriptor<T> getBeanDescriptor() {
		return beanDescriptor;
	}

}
