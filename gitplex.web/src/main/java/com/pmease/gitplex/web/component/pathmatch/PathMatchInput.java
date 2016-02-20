package com.pmease.gitplex.web.component.pathmatch;

import java.io.Serializable;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IErrorMessageSource;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pmease.gitplex.core.model.Depot;
import com.pmease.gitplex.core.util.pathmatch.PathMatchUtils;

@SuppressWarnings("serial")
public class PathMatchInput extends TextField<String> {

	private static final Logger logger = LoggerFactory.getLogger(PathMatchInput.class);
	
	private final IModel<Depot> depotModel;
	
	public PathMatchInput(String id, IModel<Depot> depotModel, IModel<String> refMatchModel) {
		super(id, refMatchModel);
		
		this.depotModel = depotModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new PathMatchBehavior(depotModel));
		add(new IValidator<String>() {

			@Override
			public void validate(IValidatable<String> validatable) {
				try {
					PathMatchUtils.parse(validatable.getValue()); 
				} catch (final Exception e) {
					logger.error("Error parsing ref match string: " + validatable.getValue(), e);
					if (e.getCause().getMessage() != null) {
						validatable.error(new IValidationError() {

							@Override
							public Serializable getErrorMessage(IErrorMessageSource messageSource) {
								return "Syntax error: " + e.getCause().getMessage();
							}
							
						});
					} else {
						validatable.error(new IValidationError() {

							@Override
							public Serializable getErrorMessage(IErrorMessageSource messageSource) {
								return "Syntax error: " + e.getCause().getClass().getSimpleName();
							}
							
						});
					}
				}
			}
			
		});
	}

	@Override
	protected void onDetach() {
		depotModel.detach();
		super.onDetach();
	}

}
