package com.pmease.gitop.web.page.project.pullrequest.activity;

import org.apache.wicket.model.LoadableDetachableModel;

import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.PullRequestUpdateManager;
import com.pmease.gitop.model.PullRequestUpdate;

@SuppressWarnings("serial")
public class PullRequestUpdateModel extends LoadableDetachableModel<PullRequestUpdate> {

	private final Long updateId;
	
	public PullRequestUpdateModel(Long updateId) {
		this.updateId = updateId;
	}
	
	@Override
	protected PullRequestUpdate load() {
		return Gitop.getInstance(PullRequestUpdateManager.class).load(updateId);
	}

}
