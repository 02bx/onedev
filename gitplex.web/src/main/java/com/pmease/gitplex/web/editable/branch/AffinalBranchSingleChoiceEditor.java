package com.pmease.gitplex.web.editable.branch;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import com.pmease.commons.editable.PropertyDescriptor;
import com.pmease.commons.wicket.editable.ErrorContext;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.commons.wicket.editable.PropertyEditor;
import com.pmease.gitplex.core.model.RepoAndBranch;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.web.component.branch.AffinalBranchSingleChoice;
import com.pmease.gitplex.web.page.repository.RepositoryPage;

@SuppressWarnings("serial")
public class AffinalBranchSingleChoiceEditor extends PropertyEditor<RepoAndBranch> {
	
	private AffinalBranchSingleChoice input;
	
	public AffinalBranchSingleChoiceEditor(String id, PropertyDescriptor propertyDescriptor, IModel<RepoAndBranch> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
    	
    	RepoAndBranch repoAndBranch = getModelObject();

		input = new AffinalBranchSingleChoice("input", new AbstractReadOnlyModel<Repository>() {

			@Override
			public Repository getObject() {
				RepositoryPage page = (RepositoryPage) getPage();
				return page.getRepository();
			}
    		
    	}, Model.of(repoAndBranch));
        
        add(input);
	}

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		return null;
	}

	@Override
	protected RepoAndBranch convertInputToValue() throws ConversionException {
		return input.getConvertedInput();
	}

}
