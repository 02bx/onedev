package com.pmease.commons.editable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface EditContext extends Serializable {
	
	Serializable getBean();
	
	void updateBean();
	
	void validate();
	
	List<String> getValidationErrors();
	
	void clearValidationErrors();
	
	boolean hasValidationError();
	
	Map<Serializable, EditContext> getChildContexts();
	
	EditContext findChildContext(List<Serializable> contextPath, boolean exactMatch);
	
	void addValidationError(String errorMessage);
	
	Object renderForEdit(Object renderParam);
	
	Object renderForView(Object renderParam);

}
