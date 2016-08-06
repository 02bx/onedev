package com.pmease.gitplex.web.page.depot.pullrequest.requestdetail;

import java.util.Collection;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.CodeComment;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.manager.AccountManager;
import com.pmease.gitplex.web.component.comment.CodeCommentFilter;
import com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.codecomments.CodeCommentsPage;

@SuppressWarnings("serial")
public class ApprovalHintPanel extends GenericPanel<PullRequest> {

	public ApprovalHintPanel(String id, IModel<PullRequest> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		PullRequest request = getModelObject();
		
		PageParameters params = CodeCommentsPage.paramsOf(request);
		getFilter().fillPageParams(params);
		add(new BookmarkablePageLink<Void>("link", CodeCommentsPage.class, params));
	}
	
	private CodeCommentFilter getFilter() {
		Account user = GitPlex.getInstance(AccountManager.class).getCurrent();
		CodeCommentFilter filter = new CodeCommentFilter();
		filter.setUnresolved(true);
		filter.setUserName(user.getName());
		return filter;
	}

	@Override
	protected void onConfigure() {
		super.onConfigure();
		
		Collection<CodeComment> comments = getModelObject().getCodeComments();
		getFilter().filter(comments);
		setVisible(!comments.isEmpty());
	}

}
