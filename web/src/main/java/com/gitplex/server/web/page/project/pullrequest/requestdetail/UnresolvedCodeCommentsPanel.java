package com.gitplex.server.web.page.project.pullrequest.requestdetail;

import java.util.Collection;

import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.UserManager;
import com.gitplex.server.model.CodeComment;
import com.gitplex.server.model.PullRequest;
import com.gitplex.server.web.component.link.ViewStateAwarePageLink;
import com.gitplex.server.web.page.project.pullrequest.requestdetail.codecomments.CodeCommentsPage;

@SuppressWarnings("serial")
class UnresolvedCodeCommentsPanel extends GenericPanel<PullRequest> {

	public UnresolvedCodeCommentsPanel(String id, IModel<PullRequest> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		PullRequest request = getModelObject();
		
		PageParameters params = CodeCommentsPage.paramsOf(request, getState());
		add(new ViewStateAwarePageLink<Void>("link", CodeCommentsPage.class, params));
	}
	
	private CodeCommentsPage.State getState() {
		CodeCommentsPage.State state = new CodeCommentsPage.State();
		state.setUnresolved(true);
		state.setUserName(GitPlex.getInstance(UserManager.class).getCurrent().getName());
		return state;
	}
	
	@Override
	protected void onConfigure() {
		super.onConfigure();
		
		Collection<CodeComment> comments = getModelObject().getCodeComments();
		getState().filter(comments);
		setVisible(!comments.isEmpty());
	}

}
