package com.pmease.gitplex.web.page.depot.pullrequest.requestdetail;

import static com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy.MERGE_ALWAYS;
import static com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy.MERGE_IF_NECESSARY;
import static com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy.MERGE_WITH_SQUASH;
import static com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy.REBASE_SOURCE_ONTO_TARGET;
import static com.pmease.gitplex.core.entity.PullRequest.IntegrationStrategy.REBASE_TARGET_ONTO_SOURCE;
import static com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.PullRequestOperation.APPROVE;
import static com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.PullRequestOperation.DELETE_SOURCE_BRANCH;
import static com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.PullRequestOperation.DISAPPROVE;
import static com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.PullRequestOperation.DISCARD;
import static com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.PullRequestOperation.INTEGRATE;
import static com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.PullRequestOperation.REOPEN;
import static com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.PullRequestOperation.RESTORE_SOURCE_BRANCH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.wicket.behavior.markdown.AttachmentSupport;
import com.pmease.commons.wicket.component.DropdownLink;
import com.pmease.commons.wicket.component.backtotop.BackToTop;
import com.pmease.commons.wicket.component.tabbable.PageTab;
import com.pmease.commons.wicket.component.tabbable.PageTabLink;
import com.pmease.commons.wicket.component.tabbable.Tab;
import com.pmease.commons.wicket.component.tabbable.Tabbable;
import com.pmease.commons.wicket.websocket.WebSocketRegion;
import com.pmease.commons.wicket.websocket.WebSocketRenderBehavior;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.entity.PullRequest.Status;
import com.pmease.gitplex.core.entity.PullRequestUpdate;
import com.pmease.gitplex.core.entity.PullRequestVerification;
import com.pmease.gitplex.core.entity.support.DepotAndBranch;
import com.pmease.gitplex.core.entity.support.IntegrationPreview;
import com.pmease.gitplex.core.manager.PullRequestManager;
import com.pmease.gitplex.core.manager.VisitInfoManager;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.component.AccountLink;
import com.pmease.gitplex.web.component.BranchLink;
import com.pmease.gitplex.web.component.comment.CommentInput;
import com.pmease.gitplex.web.component.comment.DepotAttachmentSupport;
import com.pmease.gitplex.web.component.pullrequest.verificationstatus.VerificationStatusPanel;
import com.pmease.gitplex.web.model.EntityModel;
import com.pmease.gitplex.web.page.depot.DepotPage;
import com.pmease.gitplex.web.page.depot.NoBranchesPage;
import com.pmease.gitplex.web.page.depot.pullrequest.PullRequestPage;
import com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.changes.RequestChangesPage;
import com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.codecomments.RequestCodeCommentsPage;
import com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.integrationpreview.IntegrationPreviewPage;
import com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.overview.RequestOverviewPage;
import com.pmease.gitplex.web.util.DateUtils;
import com.pmease.gitplex.web.websocket.PullRequestChanged;
import com.pmease.gitplex.web.websocket.PullRequestChangedRegion;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public abstract class RequestDetailPage extends PullRequestPage {

	protected IModel<PullRequest> requestModel;
	
	private boolean editingTitle;
	
	public RequestDetailPage(PageParameters params) {
		super(params);
		
		if (getDepot().getDefaultBranch() == null) 
			throw new RestartResponseException(NoBranchesPage.class, paramsOf(getDepot()));

		requestModel = new LoadableDetachableModel<PullRequest>() {

			@Override
			protected PullRequest load() {
				return Preconditions.checkNotNull(GitPlex.getInstance(PullRequestManager.class).find(getDepot(), params.get("request").toLong()));
			}
			
		};

	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		PullRequest request = getPullRequest();

		WebMarkupContainer requestTitle = new WebMarkupContainer("requestHead");
		requestTitle.setOutputMarkupId(true);
		add(requestTitle);
		
		requestTitle.add(new Label("title", new AbstractReadOnlyModel<String>() {

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
		
		requestTitle.add(new Label("number", "#" + request.getNumber()) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!editingTitle);
			}
			
		});
		
		requestTitle.add(new AjaxLink<Void>("editLink") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				editingTitle = true;
				
				target.add(requestTitle);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();

				setVisible(!editingTitle && SecurityUtils.canModify(getPullRequest()));
			}
			
		});

		Form<?> form = new Form<Void>("editForm") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(editingTitle);
			}
			
		};
		requestTitle.add(form);
		
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
					GitPlex.getInstance(Dao.class).persist(getPullRequest());
					editingTitle = false;
				}

				target.add(requestTitle);
			}
			
		});
		
		form.add(new AjaxLink<Void>("cancel") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				editingTitle = false;
				
				target.add(requestTitle);
			}
			
		});
		
		add(newStatusAndBranchesContainer());

		WebMarkupContainer summaryContainer = new WebMarkupContainer("requestSummary") {

			@Override
			public void onEvent(IEvent<?> event) {
				super.onEvent(event);

				if (event.getPayload() instanceof PullRequestChanged) {
					Form<?> form = visitChildren(Form.class, new IVisitor<Form<?>, Form<?>>() {

						@Override
						public void component(Form<?> object, IVisit<Form<?>> visit) {
							visit.stop(object);
						}
						
					});
					if (form == null) {
						PullRequestChanged pullRequestChanged = (PullRequestChanged) event.getPayload();
						pullRequestChanged.getPartialPageRequestHandler().add(this);
					}
				}
			}

		};
		summaryContainer.setOutputMarkupPlaceholderTag(true);
		add(summaryContainer);

		summaryContainer.add(newDiscardedNoteContainer());
		summaryContainer.add(newPendingUpdateNoteContainer());
		summaryContainer.add(newPendingApprovalNoteContainer());
		summaryContainer.add(newIntegratedNoteContainer());
		summaryContainer.add(newStatusReasonsContainer());
		summaryContainer.add(newIntegrationPreviewContainer());
		summaryContainer.add(newOperationsContainer());
		
		List<Tab> tabs = new ArrayList<>();
		
		tabs.add(new RequestTab("Overview", RequestOverviewPage.class));
		tabs.add(new RequestTab("File Changes", RequestChangesPage.class));
		tabs.add(new RequestTab("Code Comments", RequestCodeCommentsPage.class));
		tabs.add(new RequestTab("Integration Preview", IntegrationPreviewPage.class));
		
		add(new Tabbable("requestTabs", tabs).setOutputMarkupId(true));
		
		add(new BackToTop("backToTop"));
		
		add(new WebSocketRenderBehavior() {
			
			@Override
			protected void onRender(WebSocketRequestHandler handler) {
				send(getPage(), Broadcast.BREADTH, new PullRequestChanged(handler));				
			}

			@Override
			public void detach(Component component) {
				if (isOnConnect() && SecurityUtils.getAccount() != null) 
					GitPlex.getInstance(VisitInfoManager.class).visit(SecurityUtils.getAccount(), getPullRequest());
				super.detach(component);
			}

		});
	}
	
	private WebMarkupContainer newStatusAndBranchesContainer() {
		WebMarkupContainer statusAndBranchesContainer = new WebMarkupContainer("statusAndBranches");
		
		PullRequest request = getPullRequest();
		
		statusAndBranchesContainer.add(new Label("status", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getPullRequest().getStatus().toString();
			}
			
		}) {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				setOutputMarkupId(true);
				
				add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						PullRequest.Status status = getPullRequest().getStatus();
						if (status == Status.DISCARDED)
							return " label-danger";
						else if (status == Status.INTEGRATED)
							return " label-success";
						else
							return " label-warning";
					}
					
				}));
			}

			@Override
			public void onEvent(IEvent<?> event) {
				super.onEvent(event);

				if (event.getPayload() instanceof PullRequestChanged) {
					PullRequestChanged pullRequestChanged = (PullRequestChanged) event.getPayload();
					pullRequestChanged.getPartialPageRequestHandler().add(this);
				}
			}
			
		});
		
		if (request.getStatus() == Status.INTEGRATED) {
			if (request.getCloseInfo().getClosedBy() != null)
				statusAndBranchesContainer.add(new AccountLink("user", request.getCloseInfo().getClosedBy())); 
			else
				statusAndBranchesContainer.add(new WebMarkupContainer("user").setVisible(false)); 
			
			int commitCount = 0;
			for (PullRequestUpdate update: request.getUpdates())
				commitCount += update.getCommits().size();
			
			statusAndBranchesContainer.add(new Label("action", "integrated " + commitCount + " commits"));
			statusAndBranchesContainer.add(new Label("date", DateUtils.formatAge(request.getCloseInfo().getCloseDate())));
		} else {
			statusAndBranchesContainer.add(new AccountLink("user", request.getSubmitter()));
			statusAndBranchesContainer.add(new Label("action", "wants to integrate"));
			statusAndBranchesContainer.add(new Label("date", DateUtils.formatAge(request.getSubmitDate())));
		}
		
		statusAndBranchesContainer.add(new BranchLink("target", request.getTarget()));
		if (request.getSourceDepot() != null) {
			statusAndBranchesContainer.add(new BranchLink("source", request.getSource()));
		} else {
			statusAndBranchesContainer.add(new Label("source", "unknown") {

				@Override
				protected void onComponentTag(ComponentTag tag) {
					super.onComponentTag(tag);
					tag.setName("span");
				}
				
			});
		}
		return statusAndBranchesContainer;
	}
	
	private WebMarkupContainer newIntegrationPreviewContainer() {
		WebMarkupContainer integrationPreviewContainer = new WebMarkupContainer("integrationPreview") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPullRequest().isOpen());
			}
			
		};
		integrationPreviewContainer.setOutputMarkupId(true);
		
		integrationPreviewContainer.add(new WebMarkupContainer("calculating") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPullRequest().getIntegrationPreview() == null);
			}
			
		});
		integrationPreviewContainer.add(new WebMarkupContainer("conflict") {
			
			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				add(new DropdownLink("resolveInstructionsTrigger") {

					@Override
					protected void onConfigure() {
						super.onConfigure();
						setVisible(getPullRequest().getSource() != null);
					}

					@Override
					protected Component newContent(String id) {
						return new ResolveConflictInstructionPanel(id, new EntityModel<PullRequest>(getPullRequest()));
					}
					
				});
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				IntegrationPreview preview = getPullRequest().getIntegrationPreview();
				setVisible(preview != null && preview.getIntegrated() == null);
			}

		});
		integrationPreviewContainer.add(new WebMarkupContainer("noConflict") {
			
			@Override
			protected void onInitialize() {
				super.onInitialize();

				Link<Void> link = new Link<Void>("preview") {
					
					@Override
					public void onClick() {
						PullRequest request = getPullRequest();
						PageParameters params = IntegrationPreviewPage.paramsOf(request);
						setResponsePage(IntegrationPreviewPage.class, params);
					}
					
				};
				add(link);

				add(new VerificationStatusPanel("verification", requestModel, new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						PullRequest request = getPullRequest();
						IntegrationPreview preview = request.getIntegrationPreview();
						if (preview != null)
							return preview.getIntegrated();
						else
							return null;
					}
					
				}) {

					@Override
					protected Component newStatusComponent(String id, final IModel<PullRequestVerification.Status> statusModel) {
						return new Label(id, new AbstractReadOnlyModel<String>() {

							@Override
							public String getObject() {
								if (statusModel.getObject() == PullRequestVerification.Status.SUCCESSFUL)
									return "successful <i class='caret'></i>";
								else if (statusModel.getObject() == PullRequestVerification.Status.RUNNING)
									return "running <i class='caret'></i>";
								else if (statusModel.getObject() == PullRequestVerification.Status.FAILED) 
									return "failed <i class='caret'></i>";
								else 
									return "";
							}
							
						}) {

							@Override
							protected void onComponentTag(ComponentTag tag) {
								super.onComponentTag(tag);
								
								if (statusModel.getObject() == PullRequestVerification.Status.SUCCESSFUL)
									tag.put("class", "label label-success");
								else if (statusModel.getObject() == PullRequestVerification.Status.RUNNING)
									tag.put("class", "label label-warning");
								else if (statusModel.getObject() == PullRequestVerification.Status.FAILED) 
									tag.put("class", "label label-danger");
							}

							@Override
							protected void onDetach() {
								statusModel.detach();
								
								super.onDetach();
							}
							
						}.setEscapeModelStrings(false);
					}
					
				});
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				IntegrationPreview preview = getPullRequest().getIntegrationPreview();
				setVisible(preview != null && preview.getIntegrated() != null);
			}

		});
		
		return integrationPreviewContainer;
	}
	
	private WebMarkupContainer newOperationsContainer() {
		final WebMarkupContainer operationsContainer = new WebMarkupContainer("operations") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				boolean hasVisibleChildren = false;
				for (int i=0; i<size(); i++) {
					@SuppressWarnings("deprecation")
					Component child = get(i);
					child.configure();
					if (child.isVisible()) {
						hasVisibleChildren = true;
						break;
					}
				}
				
				setVisible(hasVisibleChildren);
			}
			
		};
		operationsContainer.setOutputMarkupId(true);
		
		String confirmId = "confirm";
		
		operationsContainer.add(new AjaxLink<Void>("approve") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				operationsContainer.replace(newOperationConfirm(confirmId, APPROVE, operationsContainer));
				target.add(operationsContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(APPROVE.canOperate(getPullRequest()) && !operationsContainer.get(confirmId).isVisible());
			}
			
		});
		
		operationsContainer.add(new AjaxLink<Void>("disapprove") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				operationsContainer.replace(newOperationConfirm(confirmId, DISAPPROVE, operationsContainer));
				target.add(operationsContainer);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(DISAPPROVE.canOperate(getPullRequest()) && !operationsContainer.get(confirmId).isVisible());
			}
			
		});
		
		operationsContainer.add(new AjaxLink<Void>("integrate") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				operationsContainer.replace(newOperationConfirm(confirmId, INTEGRATE, operationsContainer));
				target.add(operationsContainer);
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(INTEGRATE.canOperate(getPullRequest()) && !operationsContainer.get(confirmId).isVisible());
			}

		});
		
		operationsContainer.add(new AjaxLink<Void>("discard") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				operationsContainer.replace(newOperationConfirm(confirmId, DISCARD, operationsContainer));
				target.add(operationsContainer);
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(DISCARD.canOperate(getPullRequest()) && !operationsContainer.get(confirmId).isVisible());
			}

		});
		operationsContainer.add(new AjaxLink<Void>("reopen") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				operationsContainer.replace(newOperationConfirm(confirmId, REOPEN, operationsContainer));
				target.add(operationsContainer);
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(REOPEN.canOperate(getPullRequest()) && !operationsContainer.get(confirmId).isVisible());
			}

		});
		operationsContainer.add(new AjaxLink<Void>("deleteSourceBranch") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				operationsContainer.replace(newOperationConfirm(confirmId, DELETE_SOURCE_BRANCH, operationsContainer));
				target.add(operationsContainer);
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(DELETE_SOURCE_BRANCH.canOperate(getPullRequest()) && !operationsContainer.get(confirmId).isVisible());
			}

		});
		operationsContainer.add(new AjaxLink<Void>("restoreSourceBranch") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				operationsContainer.replace(newOperationConfirm(confirmId, RESTORE_SOURCE_BRANCH, operationsContainer));
				target.add(operationsContainer);
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(RESTORE_SOURCE_BRANCH.canOperate(getPullRequest()) && !operationsContainer.get(confirmId).isVisible());
			}

		});
		
		operationsContainer.add(new WebMarkupContainer(confirmId).setVisible(false));
		
		return operationsContainer;
	}
	
	private Component newOperationConfirm(String id, PullRequestOperation operation, 
			WebMarkupContainer operationsContainer) {
		PullRequest request = getPullRequest();

		Fragment fragment = new Fragment(id, "operationConfirmFrag", this);
		Form<?> form = new Form<Void>("form");
		fragment.add(form);

		DepotAndBranch source = request.getSource();
		Preconditions.checkNotNull(source);
		
		FormComponent<String> noteInput;
		form.add(noteInput = new CommentInput("note", Model.of("")) {

			@Override
			protected AttachmentSupport getAttachmentSupport() {
				return new DepotAttachmentSupport(requestModel.getObject().getTargetDepot(), 
						requestModel.getObject().getUUID());
			}

			@Override
			protected Depot getDepot() {
				return requestModel.getObject().getTargetDepot();
			}
			
		});
		noteInput.add(AttributeModifier.replace("placeholder", "Leave a note"));
		form.add(operation.newHinter("hint", request));
		form.add(new NotificationPanel("feedback", form));
		form.add(new AjaxButton("submit") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				
				PullRequest request = getPullRequest();
				operation.operate(request, noteInput.getModelObject());
				setResponsePage(getPage().getClass(), paramsOf(getPullRequest()));
			}

		}.add(AttributeModifier.replace("value", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return "Confirm " + WordUtils.capitalizeFully(operation.name()).replace("_", " ").toLowerCase();
			}
			
		})).add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (operation == INTEGRATE)
					return "btn-success";
				else if (operation == DISCARD)
					return "btn-danger";
				else 
					return "btn-primary";
			}
			
		})));
		form.add(new AjaxLink<Void>("cancel") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				fragment.replaceWith(new WebMarkupContainer(id).setVisible(false));
				target.add(operationsContainer);
			}
			
		});		
		
		return fragment;
	}
	
	private WebMarkupContainer newDiscardedNoteContainer() {
		WebMarkupContainer discardedNoteContainer = new WebMarkupContainer("discardedNote") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPullRequest().getStatus() == Status.DISCARDED);
			}
			
		};
		return discardedNoteContainer;
	}
	
	private WebMarkupContainer newPendingUpdateNoteContainer() {
		WebMarkupContainer pendingUpdateNoteContainer = new WebMarkupContainer("pendingUpdateNote") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPullRequest().getStatus() == Status.PENDING_UPDATE);
			}
			
		};
		return pendingUpdateNoteContainer;
	}
	
	private WebMarkupContainer newPendingApprovalNoteContainer() {
		WebMarkupContainer pendingApprovalNoteContainer = new WebMarkupContainer("pendingApprovalNote") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPullRequest().getStatus() == Status.PENDING_APPROVAL);
			}
			
		};
		return pendingApprovalNoteContainer;
	}

	private WebMarkupContainer newIntegratedNoteContainer() {
		WebMarkupContainer integratedNoteContainer = new WebMarkupContainer("integratedNote") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPullRequest().getStatus() == Status.INTEGRATED);
			}
			
		};
		integratedNoteContainer.setOutputMarkupId(true);
		
		integratedNoteContainer.add(new WebMarkupContainer("fastForwarded") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				PullRequest request = getPullRequest();
				IntegrationPreview preview = request.getLastIntegrationPreview();
				setVisible(preview != null && preview.getRequestHead().equals(preview.getIntegrated()));
			}
			
		});
		integratedNoteContainer.add(new WebMarkupContainer("merged") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				PullRequest request = getPullRequest();
				IntegrationPreview preview = request.getLastIntegrationPreview();
				setVisible(preview != null 
						&& !preview.getRequestHead().equals(preview.getIntegrated())
						&& (preview.getIntegrationStrategy() == MERGE_ALWAYS || preview.getIntegrationStrategy() == MERGE_IF_NECESSARY));
			}
			
		});
		integratedNoteContainer.add(new WebMarkupContainer("mergedOutside") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				setVisible(getPullRequest().getLastIntegrationPreview() == null);
			}
			
		});
		integratedNoteContainer.add(new WebMarkupContainer("squashed") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				PullRequest request = getPullRequest();
				IntegrationPreview preview = request.getLastIntegrationPreview();
				setVisible(preview != null 
						&& !preview.getRequestHead().equals(preview.getIntegrated())
						&& preview.getIntegrationStrategy() == MERGE_WITH_SQUASH);
			}
			
		});
		
		integratedNoteContainer.add(new WebMarkupContainer("sourceRebased") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				PullRequest request = getPullRequest();
				IntegrationPreview preview = request.getLastIntegrationPreview();
				setVisible(preview != null 
						&& !preview.getRequestHead().equals(preview.getIntegrated())
						&& preview.getIntegrationStrategy() == REBASE_SOURCE_ONTO_TARGET);
			}
			
		});
		
		integratedNoteContainer.add(new WebMarkupContainer("targetRebased") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				PullRequest request = getPullRequest();
				IntegrationPreview preview = request.getLastIntegrationPreview();
				setVisible(preview != null 
						&& !preview.getRequestHead().equals(preview.getIntegrated())
						&& preview.getIntegrationStrategy() == REBASE_TARGET_ONTO_SOURCE);
			}
			
		});
		
		return integratedNoteContainer;
	}

	private WebMarkupContainer newStatusReasonsContainer() {
		WebMarkupContainer statusReasonsContainer = new WebMarkupContainer("statusReasons") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				PullRequest.Status status = getPullRequest().getStatus();
				setVisible(status == Status.PENDING_APPROVAL || status == Status.PENDING_UPDATE);
			}
			
		};
		statusReasonsContainer.setOutputMarkupId(true);
		
		statusReasonsContainer.add(new ListView<String>("reasons", new AbstractReadOnlyModel<List<String>>() {

			@Override
			public List<String> getObject() {
				return getPullRequest().checkGates(false).getReasons();					
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<String> item) {
				item.add(new Label("reason", item.getModelObject()));
			}

		});
		
		return statusReasonsContainer;
	}
	
	@Override
	protected void onDetach() {
		requestModel.detach();
		super.onDetach();
	}

	public static PageParameters paramsOf(PullRequest request) {
		PageParameters params = DepotPage.paramsOf(request.getTarget().getDepot());
		params.set("request", request.getNumber());
		return params;
	}

	public PullRequest getPullRequest() {
		return requestModel.getObject();
	}
	
	@Override
	public Collection<WebSocketRegion> getWebSocketRegions() {
		return Lists.newArrayList(new PullRequestChangedRegion(getPullRequest().getId()));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(CssHeaderItem.forReference(new RequestDetailResourceReference()));
	}
	
	private class RequestTab extends PageTab {

		public RequestTab(String title, Class<? extends Page> pageClass) {
			super(Model.of(title), pageClass);
		}
		
		@Override
		public Component render(String componentId) {
			if (getMainPageClass() == RequestCodeCommentsPage.class) {
				Fragment fragment = new Fragment(componentId, "codeCommentsTabLinkFrag", RequestDetailPage.this);
				Link<Void> link = new BookmarkablePageLink<Void>("link", RequestCodeCommentsPage.class, paramsOf(getPullRequest())) {

					@Override
					public void onEvent(IEvent<?> event) {
						super.onEvent(event);
						if (event.getPayload() instanceof PullRequestChanged) {
							((PullRequestChanged)event.getPayload()).getPartialPageRequestHandler().add(this);
						}
					}
					
				};
				link.add(AttributeAppender.append("class", new LoadableDetachableModel<String>() {

					@Override
					protected String load() {
						PullRequest request = getPullRequest();
						if (request.getLastCodeCommentEventDate() != null && !request.isVisitedAfter(request.getLastCodeCommentEventDate()))
							return "new";
						else
							return "";
					}
					
				}));
				link.setOutputMarkupId(true);
				fragment.add(link);
				return fragment;
			} else {
				return new PageTabLink(componentId, this) {

					@Override
					protected Link<?> newLink(String linkId, Class<? extends Page> pageClass) {
						return new BookmarkablePageLink<Void>(linkId, pageClass, paramsOf(getPullRequest()));
					}
					
				};
			}
		}
		
	}
	
}