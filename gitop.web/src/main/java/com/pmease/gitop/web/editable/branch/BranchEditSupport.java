package com.pmease.gitop.web.editable.branch;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.eclipse.jgit.util.StringUtils;

import com.pmease.commons.editable.EditableUtils;
import com.pmease.commons.editable.PropertyDescriptor;
import com.pmease.commons.editable.PropertyDescriptorImpl;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.commons.wicket.editable.EditSupport;
import com.pmease.commons.wicket.editable.NotDefinedLabel;
import com.pmease.commons.wicket.editable.PropertyContext;
import com.pmease.commons.wicket.editable.PropertyEditor;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.editable.BranchChoice;
import com.pmease.gitop.model.Branch;

@SuppressWarnings("serial")
public class BranchEditSupport implements EditSupport {

	@Override
	public BeanContext<?> getBeanEditContext(Class<?> beanClass) {
		return null;
	}

	@Override
	public PropertyContext<?> getPropertyEditContext(Class<?> beanClass, String propertyName) {
		PropertyDescriptor propertyDescriptor = new PropertyDescriptorImpl(beanClass, propertyName);
		Method propertyGetter = propertyDescriptor.getPropertyGetter();
        if (propertyGetter.getAnnotation(BranchChoice.class) != null) {
        	if (List.class.isAssignableFrom(propertyGetter.getReturnType()) 
        			&& EditableUtils.getElementClass(propertyGetter.getGenericReturnType()) == Long.class) {
        		return new PropertyContext<List<Long>>(propertyDescriptor) {

					@Override
					public Component renderForView(String componentId, IModel<List<Long>> model) {
				        List<Long> branchIds = model.getObject();
				        if (branchIds != null && !branchIds.isEmpty()) {
				        	Dao dao = Gitop.getInstance(Dao.class);
				        	List<String> branchNames = new ArrayList<>();
				        	for (Long branchId: branchIds) {
				        		branchNames.add(dao.load(Branch.class, branchId).getName());
				        	}
				            return new Label(componentId, StringUtils.join(branchNames, ", " ));
				        } else {
							return new NotDefinedLabel(componentId);
				        }
					}

					@Override
					public PropertyEditor<List<Long>> renderForEdit(String componentId, IModel<List<Long>> model) {
						return new BranchMultiChoiceEditor(componentId, this, model);
					}
        			
        		};
        	} else if (propertyGetter.getReturnType() == Long.class) {
        		return new PropertyContext<Long>(propertyDescriptor) {

					@Override
					public Component renderForView(String componentId, IModel<Long> model) {
				        Long branchId = model.getObject();
				        if (branchId != null) {
				        	Branch branch = Gitop.getInstance(Dao.class).load(Branch.class, branchId);
				            return new Label(componentId, branch.getName());
				        } else {
							return new NotDefinedLabel(componentId);
				        }
					}

					@Override
					public PropertyEditor<Long> renderForEdit(String componentId, IModel<Long> model) {
						return new BranchSingleChoiceEditor(componentId, this, model);
					}
        			
        		};
        	} else {
        		throw new RuntimeException("Annotation 'BranchChoice' should be applied to property with type 'Long' or 'List<Long>'.");
        	}
        } else {
            return null;
        }
	}

}
