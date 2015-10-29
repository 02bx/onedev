package com.pmease.gitplex.web.page.repository.commit.filters;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.web.component.branchchoice.BranchChoiceProvider;
import com.pmease.gitplex.web.component.branchchoice.BranchSingleChoice;
import com.pmease.gitplex.web.page.repository.RepositoryPage;

import jersey.repackaged.com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class PathEditor extends FilterEditor {

	private BranchSingleChoice input;
	
	public PathEditor(String id, CommitFilter filter, boolean focus) {
		super(id, filter, focus);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		String branch;
		
		List<String> values = getModelObject();
		if (!values.isEmpty())
			branch = values.get(0);
		else
			branch = null;
		
    	BranchChoiceProvider branchProvider = new BranchChoiceProvider(new LoadableDetachableModel<Repository>() {

			@Override
			protected Repository load() {
				RepositoryPage page = (RepositoryPage) getPage();
				return page.getRepository();
			}
    		
    	});

    	add(input = new BranchSingleChoice("input", Model.of(branch), branchProvider, true, "Filter by branch"));
    	input.add(new AjaxFormSubmitBehavior("change") {
    		
    	});
	}

	@Override
	protected List<String> convertInputToValue() throws ConversionException {
		String value = input.getConvertedInput();
		if (value != null) 
			return Lists.newArrayList(value);
		else
			return new ArrayList<>();
	}

	@Override
	protected String getFocusScript() {
		return String.format("$('#%s').focus();", input.getMarkupId());
	}
	
}
