package com.pmease.gitplex.web.component.branchchoice;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;

import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.model.RepoAndBranch;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.web.component.repochoice.AffinalRepositoryChoice;

@SuppressWarnings("serial")
public class AffinalBranchSingleChoice extends FormComponentPanel<String> {

	private final Long currentRepoId;
	
	private final IModel<Repository> selectedRepoModel;
	
	private final boolean allowEmpty;
	
	private BranchSingleChoice branchChoice;
	
	/**
	 * Construct with current repository model and selected branch model.
	 * 
	 * @param id
	 * 			id of the component
	 * @param currentRepoId
	 * 			id of current repository
	 * @param selectedBranchModel
	 * 			model of selected branch
	 */
	public AffinalBranchSingleChoice(String id, final Long currentRepoId, 
			IModel<String> selectedBranchModel, boolean allowEmpty) {
		super(id, selectedBranchModel);
		
		this.currentRepoId = currentRepoId;
		
		selectedRepoModel = new IModel<Repository>() {

			@Override
			public void detach() {
			}

			@Override
			public Repository getObject() {
				String branchId = getBranchId();
				if (branchId == null)
					return GitPlex.getInstance(Dao.class).load(Repository.class, currentRepoId);
				else 
					return new RepoAndBranch(branchId).getRepository();
			}

			@Override
			public void setObject(Repository object) {
				setBranchId(new RepoAndBranch(object, object.getDefaultBranch()).toString());
			}
			
		};
		
		this.allowEmpty = allowEmpty;
	}
	
	protected void onChange(AjaxRequestTarget target) {
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		setOutputMarkupId(true);
		
		add(new AffinalRepositoryChoice("repositoryChoice", currentRepoId, selectedRepoModel).add(new AjaxFormComponentUpdatingBehavior("change") {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(branchChoice);
				onChange(target);
			}

		}));
		
		BranchChoiceProvider choiceProvider = new BranchChoiceProvider(selectedRepoModel);
		add(branchChoice = new BranchSingleChoice("branchChoice", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return new RepoAndBranch(AffinalBranchSingleChoice.this.getModelObject()).getBranch();
			}

			@Override
			public void setObject(String object) {
				AffinalBranchSingleChoice.this.setModelObject(new RepoAndBranch(selectedRepoModel.getObject(), object).toString());
			}
			
		}, choiceProvider, allowEmpty));
		branchChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				onChange(target);
			}
			
		});
	}
	
	private String getBranchId() {
		return getModelObject();
	}

	private void setBranchId(String branchId) {
		getModel().setObject(branchId);
	}

	@Override
	protected void convertInput() {
		setConvertedInput(branchChoice.getConvertedInput());
	}

	@Override
	protected void onDetach() {
		selectedRepoModel.detach();
		
		super.onDetach();
	}
	
}
