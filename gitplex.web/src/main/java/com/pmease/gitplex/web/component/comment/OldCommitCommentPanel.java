package com.pmease.gitplex.web.component.comment;

import java.util.Date;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.wicket.behavior.ConfirmBehavior;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.OldCommitComment;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.web.component.comment.event.CommitCommentRemoved;
import com.pmease.gitplex.web.component.comment.event.CommitCommentUpdated;
import com.pmease.gitplex.web.component.label.AgeLabel;
import com.pmease.gitplex.web.component.user.UserLink;
import com.pmease.gitplex.web.component.wiki.WikiTextPanel;

@SuppressWarnings("serial")
public class OldCommitCommentPanel extends Panel {

	public OldCommitCommentPanel(String id, IModel<OldCommitComment> model) {
		super(id, model);
	
		this.setOutputMarkupId(true);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(newCommentContent());
		add(createCommentHead("head"));
	}
	
	protected Component createCommentHead(String id) {
		Fragment frag = new Fragment(id, "headfrag", this);
		
		frag.add(new UserLink("author", Model.of(getCommitComment().getAuthor())));
//		frag.add(new WebMarkupContainer("authorType") {
//			@Override
//			protected void onConfigure() {
//				super.onConfigure();
//				
//				User author = getCommitComment().getAuthor();
//				setVisibilityAllowed(Objects.equal(author, repositoryModel.getObject().getOwner()));
//			}
//		});
		
		frag.add(new AgeLabel("age", new AbstractReadOnlyModel<Date>() {

			@Override
			public Date getObject() {
				return getCommitComment().getUpdateDate();
			}
			
		}).setOutputMarkupId(true));
		
		frag.add(newEditLink("editlink"));
		frag.add(newRemoveLink("removelink"));
		return frag;
	}
	
	protected Component newEditLink(String id) {
		return new AjaxLink<Void>(id) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				onEdit(target);
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				User current = GitPlex.getInstance(UserManager.class).getCurrent();
				
				if (current == null) {
					setVisibilityAllowed(false);
				} else {
					setVisibilityAllowed(Objects.equal(current, getCommitComment().getAuthor()));
				}
			}
			
		};
	}
	
	protected Component newRemoveLink(String id) {
		return new AjaxLink<Void>(id) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				onDelete(target);
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				User current = GitPlex.getInstance(UserManager.class).getCurrent();
				if (current == null) {
					setVisibilityAllowed(false);
				} else {
					setVisibilityAllowed(Objects.equal(current, getCommitComment().getAuthor())
							|| current.isAdmin());
				}
			}
		}.add(new ConfirmBehavior("Are you sure you want to delete this comment?"));
	}
	
	private Component newCommentContent() {
		return new WikiTextPanel("content", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				String str = getCommitComment().getContent();
				if (Strings.isNullOrEmpty(str)) {
					return "Nothing to be shown";
				} else {
					return str;
				}
			}
			
		}).setOutputMarkupId(true);
	}
	
	protected void onEdit(AjaxRequestTarget target) {
		Component c = new OldCommitCommentEditor("content", Model.of(getCommitComment().getContent())) {
			
			private void updateCommentLabel(AjaxRequestTarget target) {
				Component label = newCommentContent();
				OldCommitCommentPanel.this.addOrReplace(label);
				target.add(label);
			}

			@Override
			protected void onCancel(AjaxRequestTarget target, Form<?> form) {
				updateCommentLabel(target);
			}

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				String comment = getCommentText();
				if (Strings.isNullOrEmpty(comment)) {
					form.error("Comment can not be empty");
					target.add(form);
					return;
				}
				
				OldCommitComment cc = getCommitComment();
				cc.setUpdateDate(new Date());
				cc.setContent(comment);
				
				GitPlex.getInstance(Dao.class).persist(cc);
				
				send(getPage(), Broadcast.DEPTH, new CommitCommentUpdated(target, cc));
				updateCommentLabel(target);
			}
			
			@Override
			protected IModel<String> getCancelButtonLabel() {
				return Model.of("Cancel");
			}
			
			@Override
			protected IModel<String> getSubmitButtonLabel() {
				return Model.of("Update comment");
			}
		};
		
		c.setOutputMarkupId(true);
		addOrReplace(c);
		target.add(c);
	}
	
	protected void onDelete(AjaxRequestTarget target) {
		OldCommitComment comment = getCommitComment();
		GitPlex.getInstance(Dao.class).remove(comment);
		send(getPage(), Broadcast.BREADTH, new CommitCommentRemoved(target, comment));
	}
	
	protected OldCommitComment getCommitComment() {
		return (OldCommitComment) getDefaultModelObject();
	}
	
}
