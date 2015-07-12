package com.pmease.gitplex.web.component.branch;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;

import com.pmease.gitplex.core.model.RepoAndBranch;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.web.component.repochoice.AffinalRepositoryChoice;

@SuppressWarnings("serial")
public class AffinalBranchSingleChoice extends FormComponentPanel<RepoAndBranch> {

	private IModel<Repository> currentRepositoryModel;
	
	private IModel<Repository> selectedRepositoryModel;
	
	private BranchSingleChoice branchChoice;
	
	/**
	 * Construct with current repository model and selected branch model.
	 * 
	 * @param id
	 * 			id of the component
	 * @param currentRepoModel
	 * 			model of current repository. Note that the model object should never be null
	 * @param selectedBranchModel
	 * 			model of selected branch
	 */
	public AffinalBranchSingleChoice(String id, IModel<Repository> currentRepoModel, IModel<RepoAndBranch> selectedBranchModel) {
		super(id, selectedBranchModel);
		
		this.currentRepositoryModel = currentRepoModel;
		
		selectedRepositoryModel = new IModel<Repository>() {

			@Override
			public void detach() {
			}

			@Override
			public Repository getObject() {
				RepoAndBranch repoAndBranch = getRepoAndBranch();
				if (repoAndBranch == null)
					return AffinalBranchSingleChoice.this.currentRepositoryModel.getObject();
				else 
					return repoAndBranch.getRepository();
			}

			@Override
			public void setObject(Repository object) {
				setRepoAndBranch(new RepoAndBranch(object, object.getDefaultBranch()));
			}
			
		};
		
	}
	
	protected void onChange(AjaxRequestTarget target) {
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		setOutputMarkupId(true);
		
		add(new AffinalRepositoryChoice("repositoryChoice", currentRepositoryModel, selectedRepositoryModel).add(new AjaxFormComponentUpdatingBehavior("change") {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(branchChoice);
				onChange(target);
			}

		}));
		
		BranchChoiceProvider choiceProvider = new BranchChoiceProvider(selectedRepositoryModel);
		add(branchChoice = new BranchSingleChoice("branchChoice", getModel(), choiceProvider));
		branchChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				onChange(target);
			}
			
		});
	}
	
	private RepoAndBranch getRepoAndBranch() {
		return getModelObject();
	}

	private void setRepoAndBranch(RepoAndBranch repoAndBranch) {
		getModel().setObject(repoAndBranch);
	}

	@Override
	protected void convertInput() {
		setConvertedInput(branchChoice.getConvertedInput());
	}

	@Override
	protected void onDetach() {
		currentRepositoryModel.detach();
		selectedRepositoryModel.detach();
		
		super.onDetach();
	}
	
}
