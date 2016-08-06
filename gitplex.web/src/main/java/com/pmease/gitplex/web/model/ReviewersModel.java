package com.pmease.gitplex.web.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.entity.PullRequestReviewInvitation;

@SuppressWarnings("serial")
public class ReviewersModel extends LoadableDetachableModel<List<PullRequestReviewInvitation>>{

	private final IModel<PullRequest> requestModel;
	
	public ReviewersModel(IModel<PullRequest> requestModel) {
		this.requestModel = requestModel;
	}
	
	@Override
	protected List<PullRequestReviewInvitation> load() {
		List<PullRequestReviewInvitation> invitations = new ArrayList<>();
		for (PullRequestReviewInvitation invitation: requestModel.getObject().getReviewInvitations()) {
			if (invitation.isPreferred())
				invitations.add(invitation);
		}
		invitations.sort((invitation1, invitation2) -> invitation1.getDate().compareTo(invitation2.getDate()));
		return invitations;
	}

	@Override
	protected void onDetach() {
		requestModel.detach();
		
		super.onDetach();
	}

}
