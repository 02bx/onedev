package io.onedev.server.web.page.project.blob.render.renderers.cispec.dependencies;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import io.onedev.server.ci.DependencySpec;
import io.onedev.server.ci.JobSpec;
import io.onedev.server.web.editable.EditSupport;
import io.onedev.server.web.editable.EditableUtils;
import io.onedev.server.web.editable.EmptyValueLabel;
import io.onedev.server.web.editable.PropertyContext;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.editable.PropertyViewer;

@SuppressWarnings("serial")
public class DependencyListEditSupport implements EditSupport {

	@Override
	public PropertyContext<?> getEditContext(PropertyDescriptor descriptor) {
		if (descriptor.getBeanClass() == JobSpec.class && List.class.isAssignableFrom(descriptor.getPropertyClass())) {
			final Class<?> elementClass = EditableUtils.getElementClass(descriptor.getPropertyGetter().getGenericReturnType());
			if (elementClass == DependencySpec.class) {
				return new PropertyContext<List<Serializable>>(descriptor) {

					@Override
					public PropertyViewer renderForView(String componentId, final IModel<List<Serializable>> model) {
						return new PropertyViewer(componentId, descriptor) {

							@Override
							protected Component newContent(String id, PropertyDescriptor propertyDescriptor) {
								if (model.getObject() != null) {
									return new DependencyListViewPanel(id, elementClass, model.getObject());
								} else {
									return new EmptyValueLabel(id, propertyDescriptor.getPropertyGetter());
								}
							}
							
						};
					}

					@Override
					public PropertyEditor<List<Serializable>> renderForEdit(String componentId, IModel<List<Serializable>> model) {
						return new DependencyListEditPanel(componentId, descriptor, model);
					}
					
				};
			}
		}
		return null;
	}

	@Override
	public int getPriority() {
		return 0;
	}
	
}
