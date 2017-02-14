package com.gitplex.server.web.page.depot.pullrequest.requestdetail.overview.activity;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.gitplex.server.core.entity.PullRequest;
import com.gitplex.server.core.entity.PullRequestReference;
import com.gitplex.server.web.component.AccountLink;
import com.gitplex.server.web.component.avatar.AvatarLink;
import com.gitplex.server.web.component.pullrequest.requeststatus.RequestStatusPanel;
import com.gitplex.server.web.page.depot.pullrequest.requestdetail.overview.RequestOverviewPage;
import com.gitplex.server.web.util.DateUtils;

@SuppressWarnings("serial")
class ReferencedPanel extends GenericPanel<PullRequestReference> {

	public ReferencedPanel(String id, IModel<PullRequestReference> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		PullRequestReference reference = getModelObject();
		add(new AvatarLink("avatar", reference.getUser(), null));
		add(new AccountLink("name", reference.getUser()));
		add(new Label("age", DateUtils.formatAge(reference.getDate())));
		
		BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>("link", 
				RequestOverviewPage.class, RequestOverviewPage.paramsOf(reference.getReferencedBy()));
		link.add(new Label("number", "#" + reference.getReferencedBy().getNumber()));
		link.add(new Label("title", reference.getReferencedBy().getTitle()));
		add(link);
		
		add(new RequestStatusPanel("status", new AbstractReadOnlyModel<PullRequest>() {

			@Override
			public PullRequest getObject() {
				return ReferencedPanel.this.getModelObject().getReferencedBy();
			}
			
		}, false));
	}

}
