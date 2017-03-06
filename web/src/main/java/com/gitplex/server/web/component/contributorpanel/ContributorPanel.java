package com.gitplex.server.web.component.contributorpanel;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.jgit.lib.PersonIdent;

import com.gitplex.server.web.component.link.AccountLink;
import com.gitplex.server.web.util.DateUtils;

@SuppressWarnings("serial")
public class ContributorPanel extends Panel {

	private final PersonIdent author;
	
	private final PersonIdent committer;
	
	private final boolean withDate;
	
	public ContributorPanel(String id, PersonIdent author, PersonIdent committer, boolean withDate) {
		super(id);
		this.author = author;
		this.committer = committer;
		this.withDate = withDate;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new AccountLink("author", author));
		if (committer.getEmailAddress().equals(author.getEmailAddress())
				&& committer.getName().equals(author.getName())) {
			add(new WebMarkupContainer("committer").setVisible(false));
		} else {
			add(new AccountLink("committer", committer));
		}
		if (withDate)
			add(new Label("date", DateUtils.formatAge(committer.getWhen())));
		else
			add(new WebMarkupContainer("date").setVisible(false));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new ContributorResourceReference()));
	}

}
