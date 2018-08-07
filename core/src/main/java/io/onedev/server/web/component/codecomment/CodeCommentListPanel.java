package io.onedev.server.web.component.codecomment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;
import io.onedev.server.OneDev;
import io.onedev.server.entityquery.codecomment.CodeCommentQuery;
import io.onedev.server.manager.CodeCommentManager;
import io.onedev.server.model.CodeComment;
import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.DateUtils;
import io.onedev.server.web.WebConstants;
import io.onedev.server.web.behavior.CodeCommentQueryBehavior;
import io.onedev.server.web.component.datatable.HistoryAwareDataTable;
import io.onedev.server.web.page.project.blob.ProjectBlobPage;
import io.onedev.server.web.page.project.compare.RevisionComparePage;
import io.onedev.server.web.page.project.pullrequests.requestdetail.RequestDetailPage;
import io.onedev.server.web.page.project.pullrequests.requestdetail.changes.RequestChangesPage;
import io.onedev.server.web.page.project.pullrequests.requestlist.RequestListPage;
import io.onedev.server.web.util.PagingHistorySupport;
import io.onedev.utils.StringUtils;

@SuppressWarnings("serial")
public abstract class CodeCommentListPanel extends Panel {

	private static final Logger logger = LoggerFactory.getLogger(RequestListPage.class);
	
	private static final int MAX_COMMENT_LEN = 75;
	
	private IModel<CodeCommentQuery> parsedQueryModel = new LoadableDetachableModel<CodeCommentQuery>() {

		@Override
		protected CodeCommentQuery load() {
			try {
				CodeCommentQuery parsedQuery = CodeCommentQuery.parse(getProject(), query, true);
				if (SecurityUtils.getUser() == null && parsedQuery.needsLogin())  
					error("Please login to perform this query");
				else
					return parsedQuery;
			} catch (Exception e) {
				logger.error("Error parsing code comment query: " + query, e);
				error(e.getMessage());
			}
			return null;
		}
		
	};
	
	private String query;
	
	private DataTable<CodeComment, Void> commentsTable;
	
	public CodeCommentListPanel(String id, String query) {
		super(id);
		this.query = query;
	}

	private CodeCommentManager getCodeCommentManager() {
		return OneDev.getInstance(CodeCommentManager.class);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		TextField<String> input = new TextField<String>("input", Model.of(query));
		input.add(new CodeCommentQueryBehavior(new AbstractReadOnlyModel<Project>() {

			@Override
			public Project getObject() {
				return getProject();
			}
			
		}));
		input.add(new AjaxFormComponentUpdatingBehavior("input"){
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				query = input.getModelObject();
			}
			
		});
		
		Form<?> form = new Form<Void>("query");
		form.add(input);
		form.add(new AjaxButton("submit") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				onQueryUpdated(target);
			}
			
		});
		add(form);
		
		add(new NotificationPanel("feedback", this));

		SortableDataProvider<CodeComment, Void> dataProvider = new SortableDataProvider<CodeComment, Void>() {

			@Override
			public void detach() {
			}

			@Override
			public Iterator<? extends CodeComment> iterator(long first, long count) {
				return getCodeCommentManager().query(getProject(), getPullRequest(), parsedQueryModel.getObject(), 
						(int)first, (int)count).iterator();
			}

			@Override
			public long size() {
				CodeCommentQuery parsedQuery = parsedQueryModel.getObject();
				if (parsedQuery != null)
					return getCodeCommentManager().count(getProject(), getPullRequest(), parsedQuery.getCriteria());
				else
					return 0;
			}

			@Override
			public IModel<CodeComment> model(CodeComment object) {
				Long commentId = object.getId();
				return new LoadableDetachableModel<CodeComment>() {

					@Override
					protected CodeComment load() {
						return OneDev.getInstance(CodeCommentManager.class).load(commentId);
					}
					
				};
			}
			
		};
		
		List<IColumn<CodeComment, Void>> columns = new ArrayList<>();
		
		columns.add(new AbstractColumn<CodeComment, Void>(Model.of("")) {

			@Override
			public void populateItem(Item<ICellPopulator<CodeComment>> cellItem, String componentId, IModel<CodeComment> rowModel) {
				cellItem.add(new Label(componentId, ""));
			}

			@Override
			public String getCssClass() {
				return "new-indicator";
			}
			
		});
		
		columns.add(new AbstractColumn<CodeComment, Void>(Model.of("File")) {

			private void openComment(CodeComment comment) {
				if (!comment.isValid()) {
					CodeCommentListPanel.this.replaceWith(new InvalidCodeCommentPanel(CodeCommentListPanel.this, 
							comment.getId()));
				} else {
					PullRequest request = getPullRequest();
					if (request != null) {
						RequestDetailPage page = (RequestDetailPage) getPage();
						setResponsePage(RequestChangesPage.class, RequestChangesPage.paramsOf(request, page.getPosition(), comment));
					} else {
						String compareCommit = comment.getCompareContext().getCompareCommit();
						if (!compareCommit.equals(comment.getMarkPos().getCommit())
								&& getProject().getRepository().hasObject(ObjectId.fromString(compareCommit))) {
							setResponsePage(RevisionComparePage.class, RevisionComparePage.paramsOf(comment));
						} else {
							setResponsePage(ProjectBlobPage.class, ProjectBlobPage.paramsOf(comment));
						}
					}				
				}
			}
			
			@Override
			public void populateItem(Item<ICellPopulator<CodeComment>> cellItem, String componentId, IModel<CodeComment> rowModel) {
				cellItem.add(new Link<Void>(componentId) {

					@Override
					public void onClick() {
						openComment(rowModel.getObject());
					}

					@Override
					protected void onComponentTag(ComponentTag tag) {
						super.onComponentTag(tag);
						tag.setName("a");
					}

					@Override
					public IModel<?> getBody() {
						return Model.of(rowModel.getObject().getMarkPos().getPath());
					}
					
				});
			}

		});
		
		columns.add(new AbstractColumn<CodeComment, Void>(Model.of("Comment Content")) {

			@Override
			public void populateItem(Item<ICellPopulator<CodeComment>> cellItem, String componentId, IModel<CodeComment> rowModel) {
				CodeComment comment = rowModel.getObject();
				cellItem.add(new Label(componentId, StringUtils.abbreviate(comment.getContent(), MAX_COMMENT_LEN)));
			}

		});
		
		columns.add(new AbstractColumn<CodeComment, Void>(Model.of("Last Activity")) {

			@Override
			public void populateItem(Item<ICellPopulator<CodeComment>> cellItem, String componentId, IModel<CodeComment> rowModel) {
				CodeComment comment = rowModel.getObject();
				Fragment fragment = new Fragment(componentId, "lastActivityFrag", CodeCommentListPanel.this);
				fragment.add(new Label("user", comment.getLastActivity().getUserName()));
				fragment.add(new Label("description", comment.getLastActivity().getDescription()));
				fragment.add(new Label("date", DateUtils.formatAge(comment.getLastActivity().getDate())));
				
				cellItem.add(fragment);
			}

		});
		
		add(commentsTable = new HistoryAwareDataTable<CodeComment, Void>("comments", columns, dataProvider, 
				WebConstants.PAGE_SIZE, getPagingHistorySupport()) {

			@Override
			protected Item<CodeComment> newRowItem(String id, int index, IModel<CodeComment> model) {
				Item<CodeComment> item = super.newRowItem(id, index, model);
				CodeComment comment = model.getObject();
				item.add(AttributeAppender.append("class", 
						comment.isVisitedAfter(comment.getLastActivity().getDate())?"comment":"comment new"));
				return item;
			}
		});
		
		add(new NavigatorLabel("pageInfo", commentsTable) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(commentsTable.getItemCount() != 0);
			}
			
		});
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CodeCommentCssResourceReference()));
	}

	public String getQuery() {
		return query;
	}

	@Override
	protected void onDetach() {
		parsedQueryModel.detach();
		super.onDetach();
	}

	protected abstract Project getProject();
	
	protected abstract PagingHistorySupport getPagingHistorySupport();

	protected abstract void onQueryUpdated(AjaxRequestTarget target);

	@Nullable
	protected abstract PullRequest getPullRequest();
}
