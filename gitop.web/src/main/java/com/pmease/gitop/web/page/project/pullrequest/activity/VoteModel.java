package com.pmease.gitop.web.page.project.pullrequest.activity;

import org.apache.wicket.model.LoadableDetachableModel;

import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.VoteManager;
import com.pmease.gitop.model.Vote;

@SuppressWarnings("serial")
public class VoteModel extends LoadableDetachableModel<Vote> {

	private final Long voteId;
	
	public VoteModel(Long voteId) {
		this.voteId = voteId;
	}
	
	@Override
	protected Vote load() {
		return Gitop.getInstance(VoteManager.class).load(voteId);
	}

}
