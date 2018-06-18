package io.onedev.server.web.component.issuelist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;
import io.onedev.server.OneDev;
import io.onedev.server.manager.IssueManager;
import io.onedev.server.manager.ProjectManager;
import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.model.support.issue.IssueField;
import io.onedev.server.model.support.issue.query.IssueQuery;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.DateUtils;
import io.onedev.server.web.WebConstants;
import io.onedev.server.web.behavior.IssueQueryBehavior;
import io.onedev.server.web.component.IssueStateLabel;
import io.onedev.server.web.component.datatable.HistoryAwareDataTable;
import io.onedev.server.web.component.datatable.selectioncolumn.SelectionColumn;
import io.onedev.server.web.component.link.UserLink;
import io.onedev.server.web.component.modal.ModalLink;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.page.project.issues.fieldvalues.FieldValuesPanel;
import io.onedev.server.web.page.project.issues.issuedetail.activities.IssueActivitiesPage;
import io.onedev.server.web.page.project.issues.milestones.MilestoneDetailPage;
import io.onedev.server.web.page.project.issues.newissue.NewIssuePage;
import io.onedev.server.web.util.PagingHistorySupport;
import io.onedev.server.web.util.QueryPosition;
import io.onedev.utils.StringUtils;

@SuppressWarnings("serial")
public abstract class IssueListPanel extends GenericPanel<String> {

	private static final Logger logger = LoggerFactory.getLogger(IssueListPanel.class);
	
	private IModel<IssueQuery> parsedQueryModel = new LoadableDetachableModel<IssueQuery>() {

		@Override
		protected IssueQuery load() {
			try {
				IssueQuery additionalQuery = IssueQuery.parse(getProject(), getQuery(), true);
				if (SecurityUtils.getUser() == null && additionalQuery.needsLogin()) { 
					error("Please login to perform this query");
				} else { 
					if (SecurityUtils.getUser() == null && getBaseQuery().needsLogin())
						error("Please login to show the issues");
					else
						return IssueQuery.merge(getBaseQuery(), additionalQuery);
				}
			} catch (Exception e) {
				logger.error("Error parsing issue query: " + getQuery(), e);
				error(e.getMessage());
			}
			return null;
		}
		
	};
	
	private DataTable<Issue, Void> issuesTable;
	
	private SelectionColumn<Issue, Void> selectionColumn;
	
	private Component querySave;
	
	private ModalLink batchEditSelected;
	
	private SortableDataProvider<Issue, Void> dataProvider;	
	
	public IssueListPanel(String id, IModel<String> queryModel) {
		super(id, queryModel);
	}
	
	private IssueManager getIssueManager() {
		return OneDev.getInstance(IssueManager.class);
	}
	
	private String getQuery() {
		return getModelObject();
	}
	
	@Override
	protected void onDetach() {
		parsedQueryModel.detach();
		super.onDetach();
	}
	
	protected abstract Project getProject();

	protected IssueQuery getBaseQuery() {
		return new IssueQuery();
	}

	protected abstract PagingHistorySupport getPagingHistorySupport();
	
	protected abstract void onQueryUpdated(AjaxRequestTarget target);
	
	@Nullable
	protected abstract QuerySaveSupport getQuerySaveSupport();
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		TextField<String> input = new TextField<String>("input", getModel());
		input.add(new IssueQueryBehavior(new AbstractReadOnlyModel<Project>() {

			@Override
			public Project getObject() {
				return getProject();
			}
			
		}));
		input.add(new AjaxFormComponentUpdatingBehavior("input"){
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				if (SecurityUtils.getUser() != null && getQuerySaveSupport() != null)
					target.add(querySave);
			}
			
		});
		
		Form<?> form = new Form<Void>("query");
		form.add(input);
		form.add(new AjaxButton("submit") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				target.add(IssueListPanel.this);
				onQueryUpdated(target);
			}
			
		});
		add(form);
		
		add(new ModalLink("displayFields") {

			@Override
			protected Component newContent(String id, ModalPanel modal) {
				Fragment fragment = new Fragment(id, "fieldsFrag", IssueListPanel.this);

				FieldsEditBean bean = new FieldsEditBean();
				bean.setFields(getProject().getIssueListFields());
				Form<?> form = new Form<Void>("form") {

					@Override
					protected void onError() {
						super.onError();
						RequestCycle.get().find(AjaxRequestTarget.class).add(this);
					}

				};
				form.add(BeanContext.editBean("editor", bean));

				form.add(new AjaxLink<Void>("close") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						modal.close();
					}
					
				});
				
				form.add(new AjaxButton("save") {

					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						super.onSubmit(target, form);
						getProject().setIssueListFields((ArrayList<String>) bean.getFields());
						OneDev.getInstance(ProjectManager.class).save(getProject());
						modal.close();
						onQueryUpdated(target);
					}
					
				});
				
				form.add(new AjaxLink<Void>("cancel") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						modal.close();
					}
					
				});
				form.setOutputMarkupId(true);
				fragment.add(form);
				return fragment;
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canManage(getProject()) && issuesTable.getItemCount() != 0);
			}
			
		});

		String query;
		if (parsedQueryModel.getObject() != null)
			query = parsedQueryModel.getObject().toString();
		else
			query = null;
		add(new BookmarkablePageLink<Void>("newIssue", NewIssuePage.class, NewIssuePage.paramsOf(getProject(), query)) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canRead(getProject()));
			}
			
		});
		
		add(querySave = new AjaxLink<Void>("saveQuery") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setEnabled(StringUtils.isNotBlank(getQuery()));
				setVisible(SecurityUtils.getUser() != null && getQuerySaveSupport() != null);
			}

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				configure();
				if (!isEnabled()) {
					tag.put("disabled", "disabled");
					tag.put("title", "Input query to save");
				}
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				getQuerySaveSupport().onSaveQuery(target);
			}		
			
		});
		
		add(batchEditSelected = new ModalLink("batchEditSelected") {

			@Override
			protected Component newContent(String id, ModalPanel modal) {
				return new BatchEditPanel(id) {

					@Override
					protected Project getProject() {
						return IssueListPanel.this.getProject();
					}

					@Override
					protected void onCancel(AjaxRequestTarget target) {
						modal.close();
					}

					@Override
					protected void onUpdated(AjaxRequestTarget target) {
						modal.close();
						onQueryUpdated(target);
					}

					@Override
					protected Iterator<Issue> getIssueIterator() {
						List<Issue> issues = new ArrayList<>();
						for (IModel<Issue> each: selectionColumn.getSelections())
							issues.add(each.getObject());
						return issues.iterator();
					}

					@Override
					protected int getIssueCount() {
						return selectionColumn.getSelections().size();
					}

				};
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(selectionColumn != null && !selectionColumn.getSelections().isEmpty());
			}
			
		});

		batchEditSelected.setOutputMarkupPlaceholderTag(true);
		
		add(new ModalLink("batchEditAll") {

			@Override
			protected Component newContent(String id, ModalPanel modal) {
				return new BatchEditPanel(id) {

					@Override
					protected Project getProject() {
						return IssueListPanel.this.getProject();
					}

					@Override
					protected void onCancel(AjaxRequestTarget target) {
						modal.close();
					}

					@Override
					protected void onUpdated(AjaxRequestTarget target) {
						modal.close();
						onQueryUpdated(target);
					}

					@Override
					protected Iterator<? extends Issue> getIssueIterator() {
						return dataProvider.iterator(0, issuesTable.getItemCount());
					}

					@Override
					protected int getIssueCount() {
						return (int) issuesTable.getItemCount();
					}

				};
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canManage(getProject()) && issuesTable.getItemCount() != 0);
			}
			
		});
		
		dataProvider = new SortableDataProvider<Issue, Void>() {

			@Override
			public void detach() {
			}

			@Override
			public Iterator<? extends Issue> iterator(long first, long count) {
				return getIssueManager().query(getProject(), parsedQueryModel.getObject(), (int)first, (int)count).iterator();
			}

			@Override
			public long size() {
				IssueQuery parsedQuery = parsedQueryModel.getObject();
				if (parsedQuery != null)
					return getIssueManager().count(getProject(), parsedQuery.getCriteria());
				else
					return 0;
			}

			@Override
			public IModel<Issue> model(Issue object) {
				Long issueId = object.getId();
				return new LoadableDetachableModel<Issue>() {

					@Override
					protected Issue load() {
						return OneDev.getInstance(IssueManager.class).load(issueId);
					}
					
				};
			}
			
		};
		
		add(new NotificationPanel("feedback", this));
		
		List<IColumn<Issue, Void>> columns = new ArrayList<>();
		
		if (SecurityUtils.canManage(getProject())) {
			columns.add(selectionColumn = new SelectionColumn<Issue, Void>() {

				@Override
				protected void onSelectionChange(AjaxRequestTarget target) {
					target.add(batchEditSelected);
				}
				
			});
		}
		
		for (String field: getProject().getIssueListFields()) {
			switch (field) {
			case Issue.NUMBER:
				columns.add(new AbstractColumn<Issue, Void>(Model.of("#")) {

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						cellItem.add(new Label(componentId, rowModel.getObject().getNumber()));
					}
				});
				break;
			case Issue.STATE:
				columns.add(new AbstractColumn<Issue, Void>(Model.of(Issue.STATE)) {

					@Override
					public String getCssClass() {
						return "state";
					}

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						cellItem.add(new IssueStateLabel(componentId, rowModel));
					}
				});
				break;
			case Issue.TITLE:
				columns.add(new AbstractColumn<Issue, Void>(Model.of(Issue.TITLE)) {

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						Fragment fragment = new Fragment(componentId, "linkFrag", IssueListPanel.this);
						OddEvenItem<?> row = cellItem.findParent(OddEvenItem.class);
						QueryPosition position = new QueryPosition(parsedQueryModel.getObject().toString(), (int)issuesTable.getItemCount(), 
								(int)issuesTable.getCurrentPage() * WebConstants.PAGE_SIZE + row.getIndex());
						Link<Void> link = new BookmarkablePageLink<Void>("link", IssueActivitiesPage.class, 
								IssueActivitiesPage.paramsOf(rowModel.getObject(), position));
						link.add(new Label("label", rowModel.getObject().getTitle()));
						fragment.add(link);
						cellItem.add(fragment);
					}
				});
				break;
			case Issue.SUBMITTER:
				columns.add(new AbstractColumn<Issue, Void>(Model.of(Issue.SUBMITTER)) {

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						Issue issue = rowModel.getObject();
						Fragment fragment = new Fragment(componentId, "userFrag", IssueListPanel.this);
						User submitter = User.getForDisplay(issue.getSubmitter(), issue.getSubmitterName());
						fragment.add(new UserLink("link", submitter));
						cellItem.add(fragment);
					}
				});
				break;
			case Issue.SUBMIT_DATE:
				columns.add(new AbstractColumn<Issue, Void>(Model.of(Issue.SUBMIT_DATE)) {

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						cellItem.add(new Label(componentId, DateUtils.formatAge(rowModel.getObject().getSubmitDate())));
					}
				});
				break;
			case Issue.UPDATE_DATE:
				columns.add(new AbstractColumn<Issue, Void>(Model.of(Issue.UPDATE_DATE)) {

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						cellItem.add(new Label(componentId, DateUtils.formatAge(rowModel.getObject().getLastActivity().getDate())));
					}
				});
				break;
			case Issue.MILESTONE:
				columns.add(new AbstractColumn<Issue, Void>(Model.of(Issue.MILESTONE)) {

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						Issue issue = rowModel.getObject();
						if (issue.getMilestone() != null) {
							Fragment fragment = new Fragment(componentId, "linkFrag", IssueListPanel.this);
							Link<Void> link = new BookmarkablePageLink<Void>("link", MilestoneDetailPage.class, 
									MilestoneDetailPage.paramsOf(issue.getMilestone(), null));
							link.add(new Label("label", issue.getMilestoneName()));
							fragment.add(link);
							cellItem.add(fragment);
						} else {
							cellItem.add(new Label(componentId, "<i>No milestone</i>").setEscapeModelStrings(false));
						}
					}
				});
				break;
			case Issue.VOTES:
				columns.add(new AbstractColumn<Issue, Void>(Model.of(Issue.VOTES)) {

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						cellItem.add(new Label(componentId, rowModel.getObject().getNumOfVotes()));
					}
				});
				break;
			case Issue.COMMENTS:
				columns.add(new AbstractColumn<Issue, Void>(Model.of(Issue.COMMENTS)) {

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						cellItem.add(new Label(componentId, rowModel.getObject().getNumOfVotes()));
					}
				});
				break;
			default:
				columns.add(new AbstractColumn<Issue, Void>(Model.of(field)) {

					@Override
					public void populateItem(Item<ICellPopulator<Issue>> cellItem, String componentId,
							IModel<Issue> rowModel) {
						cellItem.add(new FieldValuesPanel(componentId) {

							@Override
							protected Issue getIssue() {
								return rowModel.getObject();
							}

							@Override
							protected IssueField getField() {
								return rowModel.getObject().getEffectiveFields().get(field);
							}
							
						}.setRenderBodyOnly(true));
					}

					@Override
					public String getCssClass() {
						return "custom-field";
					}
					
				});
			}
		}
		
		add(issuesTable = new HistoryAwareDataTable<Issue, Void>("issues", columns, dataProvider, 
				WebConstants.PAGE_SIZE, getPagingHistorySupport()) {

			@Override
			protected Item<Issue> newRowItem(String id, int index, IModel<Issue> model) {
				Item<Issue> item = super.newRowItem(id, index, model);
				Issue issue = model.getObject();
				item.add(AttributeAppender.append("class", 
						issue.isVisitedAfter(issue.getLastActivity().getDate())?"issue":"issue new"));
				return item;
			}
		});
		
		add(new NavigatorLabel("pageInfo", issuesTable) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(issuesTable.getItemCount() != 0);
			}
			
		});
		
		setOutputMarkupId(true);
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new IssueListResourceReference()));
	}
	
}
