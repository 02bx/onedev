package com.pmease.gitplex.web.page.depot.pullrequest.requestdetail;

import static com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy.MERGE_ALWAYS;
import static com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy.MERGE_WITH_SQUASH;
import static com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy.REBASE_SOURCE_ONTO_TARGET;
import static com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy.REBASE_TARGET_ONTO_SOURCE;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.eclipse.jgit.lib.ObjectId;

import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy;
import com.pmease.gitplex.core.entity.support.IntegrationPreview;
import com.pmease.gitplex.core.manager.AccountManager;
import com.pmease.gitplex.core.security.SecurityUtils;

@SuppressWarnings("serial")
class ResolveConflictInstructionPanel extends Panel {

	public ResolveConflictInstructionPanel(String id, IModel<PullRequest> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		PullRequest request = (PullRequest) getDefaultModelObject();
		Fragment fragment;
		Depot targetDepot = request.getTarget().getDepot();
		Account user = GitPlex.getInstance(AccountManager.class).getCurrent();
		IntegrationPreview preview = request.getIntegrationPreview();
		IntegrationStrategy strategy = request.getIntegrationStrategy();
		boolean sameRepo = request.getTarget().getDepot().equals(request.getSource().getDepot());					
		if (strategy == MERGE_WITH_SQUASH || strategy == REBASE_SOURCE_ONTO_TARGET) {
			fragment = new Fragment("content", "rebaseInSourceFrag", this);
			fragment.add(new Label("srcRepoName", request.getSource().getDepot()));
			fragment.add(new Label("srcBranchNameForCheckout", request.getSourceBranch()));
			fragment.add(new Label("srcBranchNameForPush", request.getSourceBranch()));
			WebMarkupContainer sameRepoContainer = new WebMarkupContainer("sameRepo");
			sameRepoContainer.add(new Label("destBranchName", request.getTargetBranch()));
			sameRepoContainer.setVisible(sameRepo);
			fragment.add(sameRepoContainer);
			WebMarkupContainer differentRepoContainer = new WebMarkupContainer("differentRepo");
			differentRepoContainer.add(new Label("destRepoUrl", request.getTarget().getDepot().getUrl()));
			differentRepoContainer.add(new Label("destBranchName", request.getTargetBranch()));
			differentRepoContainer.setVisible(!sameRepo);
			fragment.add(differentRepoContainer);
		} else if (strategy == REBASE_TARGET_ONTO_SOURCE) {
			fragment = new Fragment("content", "rebaseInTargetFrag", this);
			fragment.add(new Label("destRepoName", request.getTarget().getDepot()));
			fragment.add(new Label("destBranchNameForCheckout", request.getTargetBranch()));
			fragment.add(new Label("destBranchNameForPush", request.getTargetBranch()));
			WebMarkupContainer sameRepoContainer = new WebMarkupContainer("sameRepo");
			sameRepoContainer.add(new Label("srcBranchName", request.getSourceBranch()));
			sameRepoContainer.setVisible(sameRepo);
			fragment.add(sameRepoContainer);
			WebMarkupContainer differentRepoContainer = new WebMarkupContainer("differentRepo");
			differentRepoContainer.add(new Label("srcRepoUrl", request.getSource().getDepot().getUrl()));
			differentRepoContainer.add(new Label("srcBranchName", request.getSourceBranch()));
			differentRepoContainer.setVisible(!sameRepo);
			fragment.add(differentRepoContainer);
		} else if (user != null 
						&& SecurityUtils.canWrite(targetDepot)
						&& targetDepot.getGateKeeper().checkPush(user, request.getTargetDepot(), request.getTargetRef(), request.getTarget().getObjectId(), ObjectId.fromString(preview.getRequestHead())).allowIntegration()) {
			fragment = new Fragment("content", "mergeInTargetFrag", this);
			fragment.add(new Label("destRepoName", request.getTarget().getDepot()));
			fragment.add(new Label("destBranchNameForCheckout", request.getTargetBranch()));
			fragment.add(new Label("destBranchNameForPush", request.getTargetBranch()));
			WebMarkupContainer sameRepoContainer = new WebMarkupContainer("sameRepo");
			sameRepoContainer.add(new Label("ffOption", strategy==MERGE_ALWAYS?"--no-ff":""));
			sameRepoContainer.add(new Label("srcBranchName", request.getSourceBranch()));
			sameRepoContainer.setVisible(sameRepo);
			fragment.add(sameRepoContainer);
			WebMarkupContainer differentRepoContainer = new WebMarkupContainer("differentRepo");
			differentRepoContainer.add(new Label("ffOption", strategy==MERGE_ALWAYS?"--no-ff":""));
			differentRepoContainer.add(new Label("srcRepoUrl", request.getSource().getDepot().getUrl()));
			differentRepoContainer.add(new Label("srcBranchName", request.getSourceBranch()));
			differentRepoContainer.setVisible(!sameRepo);
			fragment.add(differentRepoContainer);
		} else {
			fragment = new Fragment("content", "mergeInSourceFrag", this);
			fragment.add(new Label("srcRepoName", request.getSource().getDepot()));
			fragment.add(new Label("srcBranchNameForCheckout", request.getSourceBranch()));
			fragment.add(new Label("srcBranchNameForPush", request.getSourceBranch()));
			WebMarkupContainer sameRepoContainer = new WebMarkupContainer("sameRepo");
			sameRepoContainer.add(new Label("ffOption", strategy==MERGE_ALWAYS?"--no-ff":""));
			sameRepoContainer.add(new Label("destBranchName", request.getTargetBranch()));
			sameRepoContainer.setVisible(sameRepo);
			fragment.add(sameRepoContainer);
			WebMarkupContainer differentRepoContainer = new WebMarkupContainer("differentRepo");
			differentRepoContainer.add(new Label("ffOption", strategy==MERGE_ALWAYS?"--no-ff":""));
			differentRepoContainer.add(new Label("destRepoUrl", request.getTarget().getDepot().getUrl()));
			differentRepoContainer.add(new Label("destBranchName", request.getTargetBranch()));
			differentRepoContainer.setVisible(!sameRepo);
			fragment.add(differentRepoContainer);
		}
		
		add(fragment);
	}

}
