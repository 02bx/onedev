package com.pmease.gitplex.web.page.repository.pullrequest.requestdetail.overview;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.pmease.gitplex.core.model.Comment;
import com.pmease.gitplex.web.component.comment.CommentPanel;
import com.pmease.gitplex.web.component.comment.InlineCommentLink;

@SuppressWarnings("serial")
class InlineCommentActivityPanel extends Panel {

	private final IModel<Comment> commentModel;
	
	public InlineCommentActivityPanel(String id, IModel<Comment> commentModel) {
		super(id);
		
		this.commentModel = commentModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new CommentPanel("content", commentModel) {

			@Override
			protected Component newActionComponent(String id) {
				Fragment fragment = new Fragment(id, "actionFrag", InlineCommentActivityPanel.this);
				String filePath = commentModel.getObject().getInlineInfo().getBlobIdent().path;
				fragment.add(new InlineCommentLink("fileLink", commentModel, Model.of(filePath)));
				
				return fragment;
			}
			
		});
	}

	@Override
	protected void onDetach() {
		commentModel.detach();
		super.onDetach();
	}
}
