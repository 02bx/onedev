package com.pmease.gitop.web.page.repository.pullrequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.pmease.commons.git.GitPerson;
import com.pmease.commons.wicket.behavior.dropdown.DropdownBehavior;
import com.pmease.commons.wicket.behavior.dropdown.DropdownPanel;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.AuthorizationManager;
import com.pmease.gitop.core.manager.PullRequestManager;
import com.pmease.gitop.core.manager.UserManager;
import com.pmease.gitop.core.manager.VoteManager;
import com.pmease.gitop.model.Branch;
import com.pmease.gitop.model.PullRequest;
import com.pmease.gitop.model.PullRequest.Status;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.Vote;
import com.pmease.gitop.model.gatekeeper.voteeligibility.VoteEligibility;
import com.pmease.gitop.model.permission.ObjectPermission;
import com.pmease.gitop.web.component.label.AgeLabel;
import com.pmease.gitop.web.component.link.AvatarLink.Mode;
import com.pmease.gitop.web.component.link.PersonLink;
import com.pmease.gitop.web.page.repository.RepositoryBasePage;
import com.pmease.gitop.web.page.repository.pullrequest.activity.RequestActivitiesPanel;

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;

@SuppressWarnings("serial")
public class RequestDetailPanel extends Panel {

	private boolean editingTitle;
	
	private Action action;
	
	private String comment;
	
	public RequestDetailPanel(String id, IModel<PullRequest> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		final WebMarkupContainer head = new WebMarkupContainer("head");
		head.setOutputMarkupId(true);
		add(head);
		
		head.add(new Label("title", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getPullRequest().getTitle();
			}
			
		}) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!editingTitle);
			}
			
		});
		
		head.add(new AjaxLink<Void>("editTitle") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				editingTitle = true;
				
				target.add(head);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();

				setVisible(Gitop.getInstance(AuthorizationManager.class)
						.canModify(getPullRequest()));
			}
			
		});

		Form<?> form = new Form<Void>("titleEditor") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(editingTitle);
			}
			
		};
		head.add(form);
		
		form.add(new TextField<String>("title", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				if (StringUtils.isNotBlank(getPullRequest().getTitle()))
					return getPullRequest().getTitle();
				else
					return "";
			}

			@Override
			public void setObject(String object) {
				getPullRequest().setTitle(object);
			}
			
		}));
		
		form.add(new AjaxButton("save") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				
				if (StringUtils.isNotBlank(getPullRequest().getTitle())) {
					Gitop.getInstance(PullRequestManager.class).save(getPullRequest());
					editingTitle = false;
				}

				target.add(head);
			}
			
		});
		
		form.add(new AjaxLink<Void>("cancel") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				editingTitle = false;
				
				target.add(head);
			}
			
		});
		
		head.add(new Label("id", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return "#" + getPullRequest().getId();
			}
			
		}));
		
		head.add(new RequestStatusPanel("status", new AbstractReadOnlyModel<PullRequest>() {

			@Override
			public PullRequest getObject() {
				return getPullRequest();
			}
			
		}) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!getPullRequest().isOpen());
			}
			
		});
		
		PullRequest request = getPullRequest();
		User submittedBy = request.getSubmittedBy();
		if (submittedBy != null) {
			GitPerson person = new GitPerson(submittedBy.getName(), submittedBy.getEmailAddress());
			add(new PersonLink("user", person, Mode.NAME_AND_AVATAR));
		} else {
			add(new Label("<i>System</i>").setEscapeModelStrings(false));
		}
		
		Link<Void> targetLink = new Link<Void>("targetLink") {

			@Override
			public void onClick() {
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				Branch target = getPullRequest().getTarget();
				setEnabled(SecurityUtils.getSubject().isPermitted(
						ObjectPermission.ofRepositoryRead(target.getRepository())));
			}
			
		};
		add(targetLink);
		targetLink.add(new Label("targetLabel", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				PullRequest request = getPullRequest();
				Branch target = request.getTarget();
				RepositoryBasePage page = (RepositoryBasePage) getPage();
				if (page.getRepository().equals(target.getRepository())) {
					return target.getName();
				} else {
					return target.getRepository().toString() + ":" + target.getName();
				}
			}
			
		}));
		
		Link<Void> sourceLink = new Link<Void>("sourceLink") {

			@Override
			public void onClick() {
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				Branch source = getPullRequest().getSource();
				setVisible(source != null);
				setEnabled(SecurityUtils.getSubject().isPermitted(
						ObjectPermission.ofRepositoryRead(source.getRepository())));
			}
			
		};
		add(sourceLink);
		sourceLink.add(new Label("sourceLabel", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				PullRequest request = getPullRequest();
				Branch source = request.getSource();
				RepositoryBasePage page = (RepositoryBasePage) getPage();
				if (page.getRepository().equals(source.getRepository())) {
					return source.getName();
				} else {
					return source.getRepository().toString() + ":" + source.getName();
				}
			}
			
		}));
		
		add(new AgeLabel("date", new AbstractReadOnlyModel<Date>() {

			@Override
			public Date getObject() {
				return getPullRequest().getCreateDate();
			}
			
		}));

		final WebMarkupContainer statusContainer = new WebMarkupContainer("status") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPullRequest().isOpen());
			}
			
		};
		statusContainer.setOutputMarkupId(true);
		add(statusContainer);
		
		statusContainer.add(new RequestStatusPanel("message", new AbstractReadOnlyModel<PullRequest>() {

			@Override
			public PullRequest getObject() {
				return getPullRequest();
			}
			
		}) {

			@Override
			protected void onConfigure() {
				super.onConfigure();

				PullRequest request = getPullRequest();
				setVisible(request.getStatus() == Status.PENDING_APPROVAL 
						|| request.getStatus() == Status.PENDING_UPDATE
						|| request.getStatus() == Status.PENDING_INTEGRATE);
			}
			
		});
		
		statusContainer.add(new ListView<String>("reasons", new LoadableDetachableModel<List<String>>() {

			@Override
			protected List<String> load() {
				return getPullRequest().getCheckResult().getReasons();
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<String> item) {
				item.add(new Label("reason", item.getModelObject()));
			}

		});
		

		WebMarkupContainer mergeContainer = new WebMarkupContainer("merge") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				PullRequest request = getPullRequest();
				setVisible(request.isOpen());
			}
			
		};
		mergeContainer.add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (getPullRequest().getMergeInfo().getMergeHead() != null)
					return "success";
				else
					return "warning";
			}
			
		}));
		statusContainer.add(mergeContainer);
		
		mergeContainer.add(new WebMarkupContainer("noConflicts") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPullRequest().getMergeInfo().getMergeHead() != null);
			}
			
		}); 
		
		WebMarkupContainer conflictsContainer = new WebMarkupContainer("conflicts") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPullRequest().getMergeInfo().getMergeHead() == null);
			}
			
		}; 
		mergeContainer.add(conflictsContainer);
		
		DropdownPanel helpDropdown = new DropdownPanel("helpDropdown") {

			@Override
			protected Component newContent(String id) {
				return new Fragment(id, "conflictHelpFrag", RequestDetailPanel.this);
			}
			
		};
		conflictsContainer.add(helpDropdown);
		conflictsContainer.add(new WebMarkupContainer("helpTrigger")
				.add(new DropdownBehavior(helpDropdown).clickMode(false)));
		
		WebMarkupContainer actionsContainer = new WebMarkupContainer("actions") {

			@Override
			protected void onConfigure() {
				super.onConfigure();

				boolean visible = false;
				for (Component link: visitChildren(AjaxLink.class)) {
					link.configure();
					if (link.isVisible())
						visible = true;
				}
				setVisible(visible);
			}
			
		};
		statusContainer.add(actionsContainer);
		
		final AjaxLink<Void> approveLink;
		actionsContainer.add(approveLink = new AjaxLink<Void>("approve") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				action = Action.Approve;
				target.add(statusContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				PullRequest request = getPullRequest();
				
				if (request.getStatus() == Status.PENDING_APPROVAL) {
					User currentUser = Gitop.getInstance(UserManager.class).getCurrent();
					if (currentUser != null) {
						if (Gitop.getInstance(VoteManager.class).find(
								currentUser, request.getLatestUpdate()) != null) {
							setVisible(false);
						} else {
							boolean canVote = false;
							for (VoteEligibility each: request.getCheckResult().getVoteEligibilities()) {
								if (each.canVote(currentUser, request)) {
									canVote = true;
									break;
								}
							}
							setVisible(canVote);
						}
					} else {
						setVisible(false);
					}
				} else {
					setVisible(false);
				}
			}
			
		});
		
		actionsContainer.add(new AjaxLink<Void>("disapprove") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				action = Action.Disapprove;
				target.add(statusContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				approveLink.configure();
				setVisible(approveLink.isVisible());
			}
			
		});
		
		actionsContainer.add(new AjaxLink<Void>("integrate") {

			@Override
			protected void onConfigure() {
				super.onConfigure();

				RepositoryBasePage page = (RepositoryBasePage) getPage();
				
				setVisible(SecurityUtils.getSubject().isPermitted(
							ObjectPermission.ofRepositoryWrite(page.getRepository())) 
						&& getPullRequest().getMergeInfo().getMergeHead() != null
						&& getPullRequest().getStatus() == Status.PENDING_INTEGRATE);
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				action = Action.Integrate;
				target.add(statusContainer);
			}
			
		});
		
		actionsContainer.add(new AjaxLink<Void>("discard") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(Gitop.getInstance(AuthorizationManager.class)
						.canModify(getPullRequest()));
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				action = Action.Discard;
				target.add(statusContainer);
			}
			
		});
		
		Form<?> commentEditor = new Form<Void>("commentEditor") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(action != null);
			}
			
		};
		statusContainer.add(commentEditor);
		
		commentEditor.add(new TextArea<String>("comment", 
				new PropertyModel<String>(this, "comment")));
		
		commentEditor.add(new Button("confirm") {

			@Override
			public void onSubmit() {
				super.onSubmit();
				
				if (StringUtils.isBlank(comment))
					comment = null;
				User currentUser = Gitop.getInstance(UserManager.class).getCurrent();
				if (action == Action.Approve) {
					Gitop.getInstance(VoteManager.class).vote(getPullRequest(), 
							currentUser, Vote.Result.APPROVE, comment);
				} else if (action == Action.Disapprove) {
					Gitop.getInstance(VoteManager.class).vote(getPullRequest(), 
							currentUser, Vote.Result.DISAPPROVE, comment);
				} else if (action == Action.Integrate) {
					Gitop.getInstance(PullRequestManager.class).merge(
							getPullRequest(), currentUser, comment);
				} else {
					Gitop.getInstance(PullRequestManager.class).discard(
							getPullRequest(), currentUser, comment);
				}
				action = null;
				comment = null;
			}
			
		}.add(AttributeModifier.replace("value", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return "Confirm " + action.name();
			}
			
		})).add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (action == Action.Approve)
					return "btn-primary";
				else if (action == Action.Disapprove)
					return "btn-primary";
				else if (action == Action.Integrate)
					return "btn-success";
				else
					return "btn-danger";
			}
			
		})));
		commentEditor.add(new AjaxLink<Void>("cancel") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				action = null;
				comment = null;
				target.add(statusContainer);
			}
			
		});
		
		List<ITab> tabs = new ArrayList<>();
		tabs.add(new ITab() {

			@Override
			public IModel<String> getTitle() {
				return Model.of("Activities");
			}

			@Override
			public WebMarkupContainer getPanel(String containerId) {
				return new RequestActivitiesPanel(containerId, new AbstractReadOnlyModel<PullRequest>() {

					@Override
					public PullRequest getObject() {
						return getPullRequest();
					}
					
				});
			}

			@Override
			public boolean isVisible() {
				return true;
			}
			
		});
		tabs.add(new ITab() {

			@Override
			public IModel<String> getTitle() {
				return Model.of("Commits");
			}

			@Override
			public WebMarkupContainer getPanel(String containerId) {
				return new RequestCommitsPanel(containerId, new AbstractReadOnlyModel<PullRequest>() {

					@Override
					public PullRequest getObject() {
						return getPullRequest();
					}
					
				});
			}

			@Override
			public boolean isVisible() {
				return true;
			}
			
		});
		add(new AjaxBootstrapTabbedPanel<>("tabs", tabs));
	}
	
	public PullRequest getPullRequest() {
		return (PullRequest) getDefaultModelObject();
	}
	
	private static enum Action {Approve, Disapprove, Integrate, Discard}
}