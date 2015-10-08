package com.pmease.gitplex.web.page.repository.pullrequest.requestdetail.overview;

import org.apache.wicket.markup.html.panel.Panel;

import com.pmease.gitplex.core.model.PullRequestActivity;

@SuppressWarnings("serial")
public class DeleteSourceBranch extends AbstractRenderableActivity {

	public DeleteSourceBranch(PullRequestActivity activity) {
		super(activity);
	}
	
	@Override
	public Panel render(String panelId) {
		return new DeleteSourceBranchActivityPanel(panelId, this);
	}

}
