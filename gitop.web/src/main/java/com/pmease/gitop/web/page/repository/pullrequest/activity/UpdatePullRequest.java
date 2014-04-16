package com.pmease.gitop.web.page.repository.pullrequest.activity;

import java.util.Date;

import org.apache.wicket.markup.html.panel.Panel;

import com.pmease.gitop.model.PullRequestUpdate;
import com.pmease.gitop.model.User;

public class UpdatePullRequest implements PullRequestActivity {

	private final PullRequestUpdate update;
	
	public UpdatePullRequest(PullRequestUpdate update) {
		this.update = update;
	}
	
	@Override
	public Panel render(String panelId) {
		return new UpdateActivityPanel(panelId, new PullRequestUpdateModel(update.getId()));
	}

	@Override
	public Date getDate() {
		return update.getDate();
	}

	@Override
	public User getUser() {
		return update.getUser();
	}

	@Override
	public String getAction() {
		return "Updated";
	}

}
