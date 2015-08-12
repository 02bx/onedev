package com.pmease.gitplex.web.page.repository.pullrequest.requestdetail.overview;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.pmease.gitplex.core.comment.Comment;
import com.pmease.gitplex.core.comment.CommentReply;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.PullRequestComment;
import com.pmease.gitplex.web.component.comment.CommentCollapsing;
import com.pmease.gitplex.web.component.comment.CommentPanel;

@SuppressWarnings("serial")
class CommentActivityPanel extends Panel {

	private final IModel<PullRequestComment> commentModel;
	
	public CommentActivityPanel(String id, IModel<PullRequestComment> commentModel) {
		super(id);
		
		this.commentModel = commentModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		add(new CommentPanel("content", commentModel) {

			@Override
			protected Component newAdditionalCommentOperations(String id, final IModel<Comment> commentModel) {
				Fragment fragment = new Fragment(id, "operationsFrag", CommentActivityPanel.this) {

					@Override
					protected void onDetach() {
						commentModel.detach();
						super.onDetach();
					}
					
				};
				
				fragment.add(new AjaxLink<Void>("collapse") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						send(CommentActivityPanel.this, Broadcast.BUBBLE, 
								new CommentCollapsing(target, commentModel.getObject()));
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();
						
						setVisible(commentModel.getObject().isResolved());
					}
					
				});
				
				fragment.add(new SinceChangesLink("changes", new AbstractReadOnlyModel<PullRequest>() {

					@Override
					public PullRequest getObject() {
						return CommentActivityPanel.this.commentModel.getObject().getRequest();
					}
					
				}, CommentActivityPanel.this.commentModel.getObject().getDate(), "Changes since this comment"));
				
				fragment.setRenderBodyOnly(true);
				
				return fragment;
			}

			@Override
			protected Component newAdditionalReplyOperations(String id, CommentReply reply) {
				return new SinceChangesLink(id, new AbstractReadOnlyModel<PullRequest>() {

					@Override
					public PullRequest getObject() {
						return commentModel.getObject().getRequest();
					}
					
				}, reply.getDate(), "Changes since this reply");
			}
			
		});
	}

	@Override
	protected void onDetach() {
		commentModel.detach();
		super.onDetach();
	}

}
