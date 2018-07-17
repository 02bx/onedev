package io.onedev.server.web.page.project.issues.issuedetail.activities.activity;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import io.onedev.server.model.IssueAction;
import io.onedev.server.model.User;
import io.onedev.server.util.DateUtils;
import io.onedev.server.web.component.link.UserLink;

@SuppressWarnings("serial")
class ActionPanel extends GenericPanel<IssueAction> {

	public ActionPanel(String id, IModel<IssueAction> model) {
		super(id, model);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		IssueAction change = getModelObject();
		User userForDisplay = User.getForDisplay(change.getUser(), change.getUserName());
		add(new UserLink("user", userForDisplay));
		add(new Label("title", change.getData().getTitle(change, false)));
		add(new Label("age", DateUtils.formatAge(change.getDate())));
		
		add(change.getData().render("body", change));
	}

}
