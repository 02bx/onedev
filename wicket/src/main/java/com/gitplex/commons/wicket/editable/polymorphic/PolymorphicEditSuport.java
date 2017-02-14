package com.gitplex.commons.wicket.editable.polymorphic;

import java.io.Serializable;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.gitplex.calla.loader.LoaderUtils;
import com.gitplex.commons.wicket.editable.BeanContext;
import com.gitplex.commons.wicket.editable.EditSupport;
import com.gitplex.commons.wicket.editable.NotDefinedLabel;
import com.gitplex.commons.wicket.editable.PropertyContext;
import com.gitplex.commons.wicket.editable.PropertyDescriptor;
import com.gitplex.commons.wicket.editable.PropertyEditor;
import com.gitplex.commons.wicket.editable.PropertyViewer;
import com.gitplex.commons.wicket.editable.annotation.Editable;

@SuppressWarnings("serial")
public class PolymorphicEditSuport implements EditSupport {

	@Override
	public BeanContext<?> getBeanEditContext(Class<?> beanClass, Set<String> excludeProperties) {
		return null;
	}

	@Override
	public PropertyContext<?> getPropertyEditContext(Class<?> beanClass, String propertyName) {
		PropertyDescriptor propertyDescriptpr = new PropertyDescriptor(beanClass, propertyName);
		Class<?> propertyClass = propertyDescriptpr.getPropertyClass();
		if (propertyClass.getAnnotation(Editable.class) != null && !LoaderUtils.isConcrete(propertyClass)) {
			return new PropertyContext<Serializable>(propertyDescriptpr) {

				@Override
				public PropertyViewer renderForView(String componentId, final IModel<Serializable> model) {
					return new PropertyViewer(componentId, this) {

						@Override
						protected Component newContent(String id, PropertyDescriptor propertyDescriptor) {
							if (model.getObject() != null)
								return new PolymorphicPropertyViewer(id, propertyDescriptor, model.getObject());
							else
								return new NotDefinedLabel(id);
						}
						
					};
				}

				@Override
				public PropertyEditor<Serializable> renderForEdit(String componentId, IModel<Serializable> model) {
					return new PolymorphicPropertyEditor(componentId, this, model);
				}
				
			};
		} else {
			return null;
		}
	}

}
