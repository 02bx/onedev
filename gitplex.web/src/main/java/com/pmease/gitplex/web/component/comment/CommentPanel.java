package com.pmease.gitplex.web.component.comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.time.Duration;
import org.hibernate.StaleObjectStateException;

import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.loader.InheritableThreadLocalData;
import com.pmease.commons.wicket.behavior.ConfirmBehavior;
import com.pmease.commons.wicket.component.feedback.FeedbackPanel;
import com.pmease.commons.wicket.component.markdownviewer.MarkdownViewer;
import com.pmease.commons.wicket.websocket.WebSocketRenderBehavior.PageId;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.CommentManager;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.Comment;
import com.pmease.gitplex.core.model.CommentReply;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.component.avatar.AvatarMode;
import com.pmease.gitplex.web.component.comment.event.CommentRemoved;
import com.pmease.gitplex.web.component.comment.event.CommentResized;
import com.pmease.gitplex.web.component.userlink.UserLink;
import com.pmease.gitplex.web.model.UserModel;
import com.pmease.gitplex.web.utils.DateUtils;
import com.pmease.gitplex.web.websocket.PullRequestChanged;

@SuppressWarnings("serial")
public class CommentPanel extends GenericPanel<Comment> {

	private static final String HEAD_ID = "head";
	
	private static final String BODY_ID = "body";
	
	private static final String CONTENT_ID = "content";
	
	private static final String ADD_REPLY_ID = "addReply";
	
	private static final String FORM_ID = "form";
	
	private RepeatingView repliesView;
	
	private final IModel<List<CommentReply>> repliesModel = new LoadableDetachableModel<List<CommentReply>>() {

		@Override
		protected List<CommentReply> load() {
			List<CommentReply> replies = new ArrayList<>(getComment().getReplies());
			Collections.sort(replies, new Comparator<CommentReply>() {

				@Override
				public int compare(CommentReply reply1, CommentReply reply2) {
					return reply1.getDate().compareTo(reply2.getDate());
				}
				
			});
			return replies;
		}
		
	};
	
	public CommentPanel(String id, IModel<Comment> commentModel) {
		super(id, commentModel);
	}

	private Comment getComment() {
		return getModelObject();
	}
	
	private Fragment renderComment() {
		Fragment fragment = new Fragment(BODY_ID, "viewFrag", this);

		fragment.add(new MarkdownViewer("comment", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return getComment().getContent();
			}

			@Override
			public void setObject(String object) {
				Comment comment = getComment();
				comment.setContent(object);
				GitPlex.getInstance(CommentManager.class).save(comment, false);				
			}
			
		}, SecurityUtils.canModify(getComment())));
		
		fragment.setOutputMarkupId(true);
		return fragment;
	}

	private Fragment renderReply(CommentReply reply) {
		Fragment fragment = new Fragment(BODY_ID, "viewFrag", this);

		final Long replyId = reply.getId();
		fragment.add(new MarkdownViewer("comment", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return GitPlex.getInstance(Dao.class).load(CommentReply.class, replyId).getContent();
			}

			@Override
			public void setObject(String object) {
				GitPlex.getInstance(Dao.class).load(CommentReply.class, replyId).saveContent(object);
			}
			
		}, SecurityUtils.canModify(getComment())));
		
		fragment.setOutputMarkupId(true);
		return fragment;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		final WebMarkupContainer head = new WebMarkupContainer(HEAD_ID); 
		head.setOutputMarkupId(true);
		add(head);
		
		head.add(new UserLink("user", new UserModel(getComment().getUser()), AvatarMode.NAME));
		head.add(newActionComponent("action"));
		head.add(new Label("age", DateUtils.formatAge(getComment().getDate())));

		head.add(new AjaxLink<Void>("edit") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Fragment fragment = new Fragment(BODY_ID, "editFrag", CommentPanel.this);

				Form<?> form = new Form<Void>(FORM_ID);
				fragment.add(form);
				final CommentInput input = new CommentInput("input", new AbstractReadOnlyModel<PullRequest>() {

					@Override
					public PullRequest getObject() {
						return getComment().getRequest();
					}
					
				}, Model.of(getComment().getContent()));
				input.setRequired(true);
				form.add(input);
				form.add(new FeedbackPanel("feedback", input).hideAfter(Duration.seconds(5)));

				final long lastVersion = getComment().getVersion();
				form.add(new AjaxSubmitLink("save") {

					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						Comment comment = getComment();
						if (comment.getVersion() != lastVersion)
							throw new StaleObjectStateException(Comment.class.getName(), comment.getId());
						comment.setContent(input.getModelObject());
						GitPlex.getInstance(CommentManager.class).save(comment, false);

						Fragment fragment = renderComment();
						CommentPanel.this.replace(fragment);
						target.add(fragment);
						target.add(head);
						send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
					}
					
					@Override
					protected void onError(AjaxRequestTarget target, Form<?> form) {
						super.onError(target, form);
						target.add(form);
					}
					
				});
				
				form.add(new AjaxLink<Void>("cancel") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						Fragment fragment = renderComment();
						CommentPanel.this.replace(fragment);
						target.add(fragment);
						target.add(head);
						send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
					}
					
				});
				
				fragment.setOutputMarkupId(true);
				CommentPanel.this.replace(fragment);
				target.add(fragment);
				target.add(head);
				send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(SecurityUtils.canModify(getComment()) && CommentPanel.this.get(BODY_ID).get(FORM_ID) == null);
			}

		});
		
		head.add(new AjaxLink<Void>("delete") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				getComment().delete();
				send(CommentPanel.this, Broadcast.BUBBLE, new CommentRemoved(target, getComment()));
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(SecurityUtils.canModify(getComment()));
			}

		}.add(new ConfirmBehavior("Deleting this comment will also delete all its replies. Do you really want to continue?")));
		
		head.add(newAdditionalCommentOperations("additionalOperations", new AbstractReadOnlyModel<Comment>() {

			@Override
			public Comment getObject() {
				return getComment();
			}
			
		}));
		
		head.add(new WebMarkupContainer("anchor").add(AttributeModifier.replace("name", "comment" + getComment().getId())));
		
		add(renderComment());

		add(newAddReply());
		
		setOutputMarkupId(true);
	}
	
	@Override
	public void onEvent(IEvent<?> event) {
		super.onEvent(event);

		if (event.getPayload() instanceof PullRequestChanged) {
			PullRequestChanged pullRequestChanged = (PullRequestChanged) event.getPayload();
			if (pullRequestChanged.getEvent() == PullRequest.Event.COMMENT_REPLIED) {
				AjaxRequestTarget target = pullRequestChanged.getTarget();
				List<CommentReply> replies = repliesModel.getObject();
				Date lastReplyDate;
				if (repliesView.size() != 0) {
					Component lastReplyRow = repliesView.get(repliesView.size()-1);
					lastReplyDate = ((CommentReply)lastReplyRow.getDefaultModelObject()).getDate();
				} else {
					lastReplyDate = getComment().getDate();
				}
				for (CommentReply reply: replies) {
					if (reply.getDate().after(lastReplyDate)) {
						Component newReplyRow = newReplyRow(repliesView.newChildId(), reply); 
						repliesView.add(newReplyRow);
						String script = String.format("$('#%s>.comment>.replies>table>tbody').append(\"<tr id='%s' class='reply'></tr>\");", 
								getMarkupId(), newReplyRow.getMarkupId());
						target.prependJavaScript(script);
						target.add(newReplyRow);
						send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
					}
				}
			}
		}
	}

	private Component newAddReply() {
		WebMarkupContainer addReplyRow = new WebMarkupContainer(ADD_REPLY_ID);
		addReplyRow.setOutputMarkupId(true);
		addReplyRow.setVisible(GitPlex.getInstance(UserManager.class).getCurrent() != null);
		
		Fragment fragment = new Fragment(CONTENT_ID, "addFrag", this);
		fragment.add(new AjaxLink<Void>(ADD_REPLY_ID) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				final WebMarkupContainer row = new WebMarkupContainer(ADD_REPLY_ID);
				
				row.setOutputMarkupId(true);
					
				Fragment fragment = new Fragment(CONTENT_ID, "editFrag", CommentPanel.this);
				row.add(fragment);
				
				Form<?> form = new Form<Void>(FORM_ID);
				fragment.add(form);
				final CommentInput input = new CommentInput("input", new AbstractReadOnlyModel<PullRequest>() {

					@Override
					public PullRequest getObject() {
						return getComment().getRequest();
					}
					
				}, Model.of(""));
				input.setRequired(true);
				form.add(input);

				form.add(new FeedbackPanel("feedback", input).hideAfter(Duration.seconds(5)));
				
				final int pageId = getPage().getPageId();
				form.add(new AjaxSubmitLink("save") {

					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						Component addReply = newAddReply();
						CommentPanel.this.replace(addReply);
						target.add(addReply);
						
						CommentReply reply; 
						InheritableThreadLocalData.set(new PageId(pageId));
						try {
							reply = getComment().addReply(input.getModelObject());
						} finally {
							InheritableThreadLocalData.clear();
						}
						Component newReplyRow = newReplyRow(repliesView.newChildId(), reply); 
						repliesView.add(newReplyRow);
						String script = String.format("$('#%s>.comment>.replies>table>tbody').append(\"<tr id='%s' class='reply'></tr>\");", 
								CommentPanel.this.getMarkupId(), newReplyRow.getMarkupId());
						target.prependJavaScript(script);
						target.add(newReplyRow);
						send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
					}
					
					@Override
					protected void onError(AjaxRequestTarget target, Form<?> form) {
						super.onError(target, form);
						target.add(form);
					}
					
				});
				
				form.add(new AjaxLink<Void>("cancel") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						Component addReply = newAddReply();
						CommentPanel.this.replace(addReply);
						target.add(addReply);
						send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
					}
					
				});

				CommentPanel.this.replace(row);
				target.add(row);
				send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
			}
			
		});
		addReplyRow.add(fragment);
		
		return addReplyRow;
	}
	
	private RepeatingView newRepliesView() {
		RepeatingView repliesView = new RepeatingView("replies");

		for (CommentReply reply: repliesModel.getObject())
			repliesView.add(newReplyRow(repliesView.newChildId(), reply));
		
		return repliesView;
	}

	@Override
	protected void onBeforeRender() {
		addOrReplace(repliesView = newRepliesView());
		
		super.onBeforeRender();
	}

	private Component newReplyRow(String id, CommentReply reply) {
		final Long replyId = reply.getId();
		final WebMarkupContainer row = new WebMarkupContainer(id, new LoadableDetachableModel<CommentReply>() {

			@Override
			protected CommentReply load() {
				return GitPlex.getInstance(Dao.class).load(CommentReply.class, replyId);
			}
			
		});
		row.setOutputMarkupId(true);
		
		WebMarkupContainer avatarColumn = new WebMarkupContainer("avatar");
		avatarColumn.add(new UserLink("avatar", new UserModel(reply.getUser()), AvatarMode.AVATAR));
		row.add(avatarColumn);
		
		final WebMarkupContainer head = new WebMarkupContainer("head");
		head.setOutputMarkupId(true);
		row.add(head);
		
		head.add(new UserLink("user", new UserModel(reply.getUser()), AvatarMode.NAME));
		head.add(new Label("age", DateUtils.formatAge(reply.getDate())));

		head.add(new AjaxLink<Void>("edit") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				Fragment fragment = new Fragment(BODY_ID, "editFrag", CommentPanel.this);

				Form<?> form = new Form<Void>(FORM_ID);
				fragment.add(form);
				CommentReply reply = (CommentReply) row.getDefaultModelObject();
				final CommentInput input = new CommentInput("input", new AbstractReadOnlyModel<PullRequest>() {

					@Override
					public PullRequest getObject() {
						return getComment().getRequest();
					}
					
				}, Model.of(reply.getContent()));
				input.setRequired(true);
				form.add(input);
				form.add(new FeedbackPanel("feedback", input).hideAfter(Duration.seconds(5)));
				
				form.add(new AjaxSubmitLink("save") {

					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						CommentReply reply = (CommentReply) row.getDefaultModelObject();
						reply.saveContent(input.getModelObject());

						Fragment fragment = renderReply(reply);
						row.replace(fragment);
						target.add(fragment);
						target.add(head);
						send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
					}
					
					@Override
					protected void onError(AjaxRequestTarget target, Form<?> form) {
						super.onError(target, form);
						target.add(form);
					}
					
				});
				
				form.add(new AjaxLink<Void>("cancel") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						CommentReply reply = (CommentReply) row.getDefaultModelObject();
						Fragment fragment = renderReply(reply);
						row.replace(fragment);
						target.add(fragment);
						target.add(head);
						send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
					}
					
				});
				
				fragment.setOutputMarkupId(true);
				row.replace(fragment);
				target.add(fragment);
				target.add(head);
				send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(SecurityUtils.canModify(getComment()) && row.get(BODY_ID).get(FORM_ID) == null);
			}

		});
		
		head.add(new AjaxLink<Void>("delete") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				CommentReply reply = (CommentReply) row.getDefaultModelObject();
				reply.delete();
				row.remove();
				target.appendJavaScript(String.format("$('#%s').remove();", row.getMarkupId()));
				send(CommentPanel.this, Broadcast.BUBBLE, new CommentResized(target, getComment()));
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(SecurityUtils.canModify(getComment()));
			}

		}.add(new ConfirmBehavior("Do you really want to delete this reply?")));
		
		head.add(newAdditionalReplyOperations("additionalOperations", (CommentReply) row.getDefaultModelObject()));
		head.add(new WebMarkupContainer("anchor").add(AttributeModifier.replace("name", "reply" + reply.getId())));		
		
		row.add(renderReply(reply));
		
		Date lastVisitDate = getComment().getLastVisitDate();
		if (lastVisitDate != null && lastVisitDate.before(reply.getDate())) {
			row.add(AttributeAppender.append("class", " new"));
			avatarColumn.add(AttributeAppender.append("title", "New reply since your last visit"));
		}
		
		return row;
	}

	protected Component newAdditionalCommentOperations(String id, IModel<Comment> comment) {
		return new WebMarkupContainer(id);
	}
	
	protected Component newAdditionalReplyOperations(String id, CommentReply reply) {
		return new WebMarkupContainer(id);
	}
	
	protected Component newActionComponent(String id) {
		return new Label(id, "commented");
	}

	@Override
	protected void onDetach() {
		repliesModel.detach();
		
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(CssHeaderItem.forReference(
				new CssResourceReference(CommentPanel.class, "comment.css")));
	}
	
}
