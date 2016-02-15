package com.pmease.gitplex.web.component.branchpicker;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.unbescape.html.HtmlEscape;

import com.pmease.commons.wicket.component.DropdownLink;
import com.pmease.gitplex.core.model.Depot;

@SuppressWarnings("serial")
public abstract class BranchPicker extends DropdownLink {

	private final IModel<Depot> repoModel;
	
	private String branch;
	
	public BranchPicker(String id, IModel<Depot> repoModel, String branch) {
		super(id);
		
		this.repoModel = repoModel;
		this.branch = branch;
	}

	@Override
	protected Component newContent(String id) {
		return new BranchSelector(id, repoModel, branch) {

			@Override
			protected void onSelect(AjaxRequestTarget target, String branch) {
				close(target);
				BranchPicker.this.branch = branch;
				target.add(BranchPicker.this);
				
				BranchPicker.this.onSelect(target, branch);
			}
			
		};
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		setEscapeModelStrings(false);
	}

	@Override
	public IModel<?> getBody() {
		return Model.of(String.format("<i class='fa fa-ext fa-branch'></i> <span>%s</span> <i class='fa fa-caret-down'></i>", 
				HtmlEscape.escapeHtml5(branch)));
	}

	@Override
	protected void onDetach() {
		repoModel.detach();
		super.onDetach();
	}

	protected abstract void onSelect(AjaxRequestTarget target, String branch);
}
