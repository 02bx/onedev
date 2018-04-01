package io.onedev.server.web.editable.beanlist;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import io.onedev.server.util.editable.EditableUtils;
import io.onedev.server.util.editable.annotation.Editable;
import io.onedev.server.web.editable.EditSupport;
import io.onedev.server.web.editable.EmptyValueLabel;
import io.onedev.server.web.editable.PropertyContext;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.editable.PropertyViewer;
import io.onedev.utils.ClassUtils;

@SuppressWarnings("serial")
public class BeanListEditSupport implements EditSupport {

	@Override
	public PropertyContext<?> getEditContext(Class<?> beanClass, String propertyName) {
		PropertyDescriptor propertyDescriptor = new PropertyDescriptor(beanClass, propertyName);
		
		if (List.class.isAssignableFrom(propertyDescriptor.getPropertyClass())) {
			final Class<?> elementClass = EditableUtils.getElementClass(propertyDescriptor.getPropertyGetter().getGenericReturnType());
			if (elementClass != null && ClassUtils.isConcrete(elementClass) 
					&& elementClass.getAnnotation(Editable.class) != null) {
				return new PropertyContext<List<Serializable>>(propertyDescriptor) {

					@Override
					public PropertyViewer renderForView(String componentId, final IModel<List<Serializable>> model) {
						return new PropertyViewer(componentId, this) {

							@Override
							protected Component newContent(String id, PropertyDescriptor propertyDescriptor) {
								if (model.getObject() != null) {
									return new BeanListPropertyViewer(id, elementClass, model.getObject());
								} else {
									return new EmptyValueLabel(id, propertyDescriptor.getPropertyGetter());
								}
							}
							
						};
					}

					@Override
					public PropertyEditor<List<Serializable>> renderForEdit(String componentId, IModel<List<Serializable>> model) {
						return new BeanListPropertyEditor(componentId, this, model);
					}
					
				};
			}
		}
		return null;
	}

}
