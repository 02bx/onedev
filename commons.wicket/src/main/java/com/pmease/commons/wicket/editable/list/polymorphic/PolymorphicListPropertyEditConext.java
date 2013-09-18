package com.pmease.commons.wicket.editable.list.polymorphic;

import java.io.Serializable;

import org.apache.wicket.markup.html.basic.Label;

import com.pmease.commons.editable.AbstractPolymorphicListPropertyEditContext;

@SuppressWarnings("serial")
public class PolymorphicListPropertyEditConext extends AbstractPolymorphicListPropertyEditContext {

	public PolymorphicListPropertyEditConext(Serializable bean, String propertyName) {
		super(bean, propertyName);
	}

	@Override
	public Object renderForEdit(Object renderParam) {
		return new PolymorphicListPropertyEditor((String) renderParam, this);
	}

	@Override
	public Object renderForView(Object renderParam) {
		if (getElementContexts() != null) {
			return new PolymorphicListPropertyViewer((String) renderParam, this);
		} else {
			return new Label((String) renderParam, "<i>Not Defined</i>").setEscapeModelStrings(false);
		}
	}

}
