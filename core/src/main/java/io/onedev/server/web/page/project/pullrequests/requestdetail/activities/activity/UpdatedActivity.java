package io.onedev.server.web.page.project.pullrequests.requestdetail.activities.activity;

import java.util.Date;

import org.apache.wicket.Component;
import org.apache.wicket.model.LoadableDetachableModel;

import io.onedev.server.OneDev;
import io.onedev.server.model.PullRequestUpdate;
import io.onedev.server.model.User;
import io.onedev.server.persistence.dao.Dao;
import io.onedev.server.web.page.project.pullrequests.requestdetail.activities.PullRequestActivity;
import io.onedev.server.web.util.DeleteCallback;

@SuppressWarnings("serial")
public class UpdatedActivity implements PullRequestActivity {

	private final Long updateId;
	
	public UpdatedActivity(PullRequestUpdate update) {
		updateId = update.getId();
	}
	
	@Override
	public Component render(String componentId, DeleteCallback deleteCallback) {
		return new UpdatedPanel(componentId, new LoadableDetachableModel<PullRequestUpdate>() {

			@Override
			protected PullRequestUpdate load() {
				return getUpdate();
			}
			
		});
	}

	public PullRequestUpdate getUpdate() {
		return OneDev.getInstance(Dao.class).load(PullRequestUpdate.class, updateId);
	}

	@Override
	public Date getDate() {
		return getUpdate().getDate();
	}

	@Override
	public String getAnchor() {
		return null;
	}

	@Override
	public User getUser() {
		return null;
	}

}
