package com.gitplex.server.web.page.depot.pullrequest.requestdetail;

import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import com.gitplex.server.model.PullRequestUpdate;
import com.gitplex.server.web.component.link.ViewStateAwarePageLink;
import com.gitplex.server.web.page.depot.pullrequest.requestdetail.changes.RequestChangesPage;

@SuppressWarnings("serial")
public class UnreviewedChangesPanel extends GenericPanel<PullRequestUpdate> {

	public UnreviewedChangesPanel(String id, IModel<PullRequestUpdate> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		PullRequestUpdate update = getModelObject();

		RequestChangesPage.State state = new RequestChangesPage.State();
		state.oldCommit = update.getHeadCommitHash();
		state.newCommit = update.getRequest().getHeadCommitHash();
		
		add(new ViewStateAwarePageLink<Void>("link", RequestChangesPage.class, 
				RequestChangesPage.paramsOf(update.getRequest(), state)));
	}

	@Override
	protected void onConfigure() {
		super.onConfigure();
		
		PullRequestUpdate update = getModelObject();
		setVisible(!update.equals(update.getRequest().getLatestUpdate()));
	}
	
}
