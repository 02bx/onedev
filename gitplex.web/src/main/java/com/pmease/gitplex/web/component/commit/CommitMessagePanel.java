package com.pmease.gitplex.web.component.commit;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.google.common.base.Objects;
import com.pmease.commons.git.Commit;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.web.page.repository.info.code.commit.RepoCommitPage;

@SuppressWarnings("serial")
public class CommitMessagePanel extends Panel {

	private final IModel<Repository> repoModel;
	
	public CommitMessagePanel(String id, IModel<Repository> repoModel, IModel<Commit> commitModel) {
		super(id, commitModel);
		
		this.repoModel = repoModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		AbstractLink link = new BookmarkablePageLink<Void>("commitlink",
				RepoCommitPage.class,
				RepoCommitPage.paramsOf(repoModel.getObject(), getCommit().getHash(), null));
		
		add(link);
		link.add(new Label("shortmessage", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getCommit().getSubject();
			}
		}));

		add(new Label("detailedmessage", Model.of(getCommit().getMessage())) {
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				Commit commit = getCommit();
				setVisibilityAllowed(!Objects.equal(commit.getSubject(), commit.getMessage()));
			}
		});
		
		WebMarkupContainer detailedToggle = new WebMarkupContainer("detailedToggle") {
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				Commit commit = getCommit();
				boolean b = !Objects.equal(commit.getSubject(), commit.getMessage());
				setVisibilityAllowed(b);
			}
		};
		add(detailedToggle);
	}
	
	private Commit getCommit() {
		return (Commit) getDefaultModelObject();
	}

	@Override
	protected void onDetach() {
		repoModel.detach();
		
		super.onDetach();
	}
	
}
