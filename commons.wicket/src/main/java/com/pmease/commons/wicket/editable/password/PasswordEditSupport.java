package com.pmease.commons.wicket.editable.password;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.commons.wicket.editable.DefaultPropertyDescriptor;
import com.pmease.commons.wicket.editable.EditSupport;
import com.pmease.commons.wicket.editable.NotDefinedLabel;
import com.pmease.commons.wicket.editable.PropertyContext;
import com.pmease.commons.wicket.editable.PropertyDescriptor;
import com.pmease.commons.wicket.editable.PropertyEditor;
import com.pmease.commons.wicket.editable.PropertyViewer;
import com.pmease.commons.wicket.editable.annotation.Password;

@SuppressWarnings("serial")
public class PasswordEditSupport implements EditSupport {

	@Override
	public BeanContext<?> getBeanEditContext(Class<?> beanClass) {
		return null;
	}

	@Override
	public PropertyContext<?> getPropertyEditContext(Class<?> beanClass, String propertyName) {
		PropertyDescriptor propertyDescriptor = new DefaultPropertyDescriptor(beanClass, propertyName);
		if (propertyDescriptor.getPropertyClass() == String.class) {
			Password password = propertyDescriptor.getPropertyGetter().getAnnotation(Password.class);
			if (password != null) {
				if (password.confirmative()) {
					return new PropertyContext<String>(propertyDescriptor) {

						@Override
						public PropertyViewer renderForView(String componentId, final IModel<String> model) {
							return new PropertyViewer(componentId, this) {

								@Override
								protected Component newContent(String id, PropertyDescriptor propertyDescriptor) {
									if (model.getObject() != null) {
										return new Label(id, "******");
									} else {
										return new NotDefinedLabel(id);
									}
								}
								
							};
						}

						@Override
						public PropertyEditor<String> renderForEdit(String componentId, IModel<String> model) {
							return new ConfirmativePasswordPropertyEditor(componentId, this, model);
						}
						
					};
				} else {
					return new PropertyContext<String>(propertyDescriptor) {

						@Override
						public PropertyViewer renderForView(String componentId, final IModel<String> model) {
							return new PropertyViewer(componentId, this) {

								@Override
								protected Component newContent(String id, PropertyDescriptor propertyDescriptor) {
									if (model.getObject() != null) {
										return new Label(id, "******");
									} else {
										return new NotDefinedLabel(id);
									}
								}
								
							};
						}

						@Override
						public PropertyEditor<String> renderForEdit(String componentId, IModel<String> model) {
							return new PasswordPropertyEditor(componentId, this, model);
						}
						
					};
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

}
