package com.pmease.gitplex.web.page.depot.comments;

import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.manager.CodeCommentInfoManager;
import com.pmease.gitplex.web.component.comment.CodeCommentAware;
import com.pmease.gitplex.web.component.comment.CodeCommentFilter;
import com.pmease.gitplex.web.component.comment.CodeCommentListPanel;
import com.pmease.gitplex.web.page.depot.DepotPage;

@SuppressWarnings("serial")
public class DepotCommentsPage extends DepotPage implements CodeCommentAware {

	private final CodeCommentFilter filterOption;
	
	private final List<String> commentedFiles;
	
	public DepotCommentsPage(PageParameters params) {
		super(params);
		
		filterOption = new CodeCommentFilter(params);
		
		commentedFiles = GitPlex.getInstance(CodeCommentInfoManager.class).getCommentedFiles(getDepot());
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new CodeCommentListPanel("codeComments", new IModel<CodeCommentFilter>() {

			@Override
			public void detach() {
			}

			@Override
			public CodeCommentFilter getObject() {
				return filterOption;
			}

			@Override
			public void setObject(CodeCommentFilter object) {
				PageParameters params = paramsOf(getDepot());
				object.fillPageParams(params);
				setResponsePage(DepotCommentsPage.class, params);
			}
			
		}) {
			
			@Override
			protected PullRequest getPullRequest() {
				return null;
			}

		});
	}

	@Override
	public List<String> getCommentedFiles() {
		return commentedFiles;
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new DepotCommentsResourceReference()));
	}

}
