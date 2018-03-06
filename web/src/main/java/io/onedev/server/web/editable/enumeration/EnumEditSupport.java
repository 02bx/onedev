package io.onedev.server.web.editable.enumeration;

import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.editable.EditSupport;
import io.onedev.server.web.editable.NotDefinedLabel;
import io.onedev.server.web.editable.PropertyContext;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.editable.PropertyViewer;

@SuppressWarnings("serial")
public class EnumEditSupport implements EditSupport {

	@Override
	public BeanContext<?> getBeanEditContext(Class<?> beanClass, Set<String> excludeProperties) {
		return null;
	}

	@Override
	public PropertyContext<?> getPropertyEditContext(Class<?> beanClass, String propertyName) {
		PropertyDescriptor propertyDescriptor = new PropertyDescriptor(beanClass, propertyName);
		
        if (Enum.class.isAssignableFrom(propertyDescriptor.getPropertyClass())) {
            return new PropertyContext<Enum<?>>(propertyDescriptor) {

				@Override
				public PropertyViewer renderForView(String componentId, final IModel<Enum<?>> model) {
					return new PropertyViewer(componentId, this) {

						@Override
						protected Component newContent(String id, PropertyDescriptor propertyDescriptor) {
					        if (model.getObject() != null)
					            return new Label(id, model.getObject().toString());
					        else 
								return new NotDefinedLabel(id);
						}
						
					};
				}

				@Override
				public PropertyEditor<Enum<?>> renderForEdit(String componentId, IModel<Enum<?>> model) {
					return new EnumPropertyEditor(componentId, this, model);
				}
            	
            };
        } else {
            return null;
        }
	}

}
