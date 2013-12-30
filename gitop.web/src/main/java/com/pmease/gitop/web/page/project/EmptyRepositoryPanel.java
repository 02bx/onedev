package com.pmease.gitop.web.page.project;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.model.Project;
import com.pmease.gitop.web.page.project.source.component.ProjectPanel;
import com.pmease.gitop.web.util.UrlUtils;

@SuppressWarnings("serial")
public class EmptyRepositoryPanel extends ProjectPanel {

	public EmptyRepositoryPanel(String id, IModel<Project> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new Label("new", Model.of(getRepoUrl())));
		add(new Label("existing", Model.of(getRepoUrl())));
	}
	
	@Override
	protected void onConfigure() {
		super.onConfigure();
		this.setVisibilityAllowed(!getProject().code().hasCommits());
	}
	
	private String getRepoUrl() {
		Project project = getProject();
		return UrlUtils.concatSegments(Gitop.getInstance().guessServerUrl(), project.getPathName() + ".git");
	}
}
