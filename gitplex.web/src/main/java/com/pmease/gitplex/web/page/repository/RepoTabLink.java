package com.pmease.gitplex.web.page.repository;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;

@SuppressWarnings("serial")
public class RepoTabLink extends Panel {

	private final RepoTab tab;
	
	public RepoTabLink(String id, RepoTab tab) {
		super(id);
		
		this.tab = tab;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		RepositoryPage page = (RepositoryPage) getPage();
		Link<Void> link = new BookmarkablePageLink<Void>("link", 
				tab.getMainPageClass(), RepositoryPage.paramsOf(page.getRepository()));
		link.add(new Label("label", tab.getTitleModel()));
		add(link);
	}

}
