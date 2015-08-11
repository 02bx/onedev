package com.pmease.gitplex.web.page.repository.pullrequest.requestlist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.commons.wicket.behavior.menu.CheckItem;
import com.pmease.commons.wicket.behavior.menu.LinkItem;
import com.pmease.commons.wicket.behavior.menu.MenuBehavior;
import com.pmease.commons.wicket.behavior.menu.MenuItem;
import com.pmease.commons.wicket.behavior.menu.MenuPanel;
import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.web.Constants;
import com.pmease.gitplex.web.DateUtils;
import com.pmease.gitplex.web.component.avatar.AvatarMode;
import com.pmease.gitplex.web.component.branchlink.BranchLink;
import com.pmease.gitplex.web.component.pullrequest.requestlink.RequestLink;
import com.pmease.gitplex.web.component.pullrequest.requeststatus.RequestStatusPanel;
import com.pmease.gitplex.web.component.userlink.UserLink;
import com.pmease.gitplex.web.page.repository.RepositoryPage;
import com.pmease.gitplex.web.page.repository.pullrequest.PullRequestPage;
import com.pmease.gitplex.web.page.repository.pullrequest.newrequest.NewRequestPage;
import com.pmease.gitplex.web.page.repository.pullrequest.requestlist.SearchOption.Status;

@SuppressWarnings("serial")
public class RequestListPage extends PullRequestPage {

	private static final Map<SortOption, String> sortNames = new LinkedHashMap<>();
	
	static {
		sortNames.put(new SortOption("createDate", false), "Newest");
		sortNames.put(new SortOption("createDate", true), "Oldest");
		sortNames.put(new SortOption("lastEventDate", false), "Recently updated");
		sortNames.put(new SortOption("lastEventDate", true), "Least recently updated");
	}
	
	private SearchOption searchOption;
	
	private SortOption sortOption;
	
	public RequestListPage(PageParameters params) {
		super(params);
		
		searchOption = new SearchOption(params);
		sortOption = new SortOption(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		MenuPanel filterMenu = new MenuPanel("filterMenu") {

			@Override
			protected List<MenuItem> getMenuItems() {
				List<MenuItem> menuItems = new ArrayList<>();

				User currentUser = GitPlex.getInstance(UserManager.class).getCurrent();
				if (currentUser != null) {
					final Long userId = currentUser.getId();
					menuItems.add(new LinkItem("Open requests assigned to me") {

						@Override
						public void onClick() {
							searchOption = new SearchOption();
							searchOption.setStatus(Status.OPEN);
							searchOption.setAssigneeId(userId);
							
							setResponsePage(RequestListPage.class, paramsOf(getRepository(), searchOption, sortOption));
						}
						
					});
					menuItems.add(new LinkItem("Open requests submitted by me") {

						@Override
						public void onClick() {
							searchOption = new SearchOption();
							searchOption.setStatus(Status.OPEN);
							searchOption.setSubmitterId(userId);

							setResponsePage(RequestListPage.class, paramsOf(getRepository(), searchOption, sortOption));
						}
						
					});
				}
				menuItems.add(new LinkItem("All open requests") {

					@Override
					public void onClick() {
						searchOption = new SearchOption();
						searchOption.setStatus(Status.OPEN);
						
						setResponsePage(RequestListPage.class, paramsOf(getRepository(), searchOption, sortOption));
					}
					
				});
				menuItems.add(new LinkItem("All closed requests") {

					@Override
					public void onClick() {
						searchOption = new SearchOption();
						searchOption.setStatus(Status.CLOSED);
						
						setResponsePage(RequestListPage.class, paramsOf(getRepository(), searchOption, sortOption));
					}
					
				});
				return menuItems;
			}
			
		};
		add(filterMenu);
		add(new WebMarkupContainer("filters").add(new MenuBehavior(filterMenu)));
		
		MenuPanel sortMenu = new MenuPanel("sortMenu") {

			@Override
			protected List<MenuItem> getMenuItems() {
				List<MenuItem> menuItems = new ArrayList<>();
				
				for (Map.Entry<SortOption, String> entry: sortNames.entrySet()) {
					final SortOption sortOption = entry.getKey();
					final String displayName = entry.getValue();
					menuItems.add(new CheckItem() {

						@Override
						public void onClick(AjaxRequestTarget target) {
							setResponsePage(RequestListPage.class, paramsOf(getRepository(), searchOption, sortOption));
						}

						@Override
						protected String getLabel() {
							return displayName;
						}

						@Override
						protected boolean isChecked() {
							return RequestListPage.this.sortOption.equals(sortOption);
						}
						
					});
				}

				return menuItems;
			}
			
		};
		add(sortMenu);
		
		add(new WebMarkupContainer("sortBy").add(new MenuBehavior(sortMenu)));
		
		add(new Link<Void>("newRequest") {

			@Override
			public void onClick() {
				RepositoryPage page = (RepositoryPage) getPage();
				setResponsePage(NewRequestPage.class, NewRequestPage.paramsOf(page.getRepository()));
			}
			
		});

		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				setResponsePage(RequestListPage.class, paramsOf(getRepository(), searchOption, sortOption));
			}
			
		};
		form.add(BeanContext.editBean("editor", searchOption));
		add(form);
		
		List<IColumn<PullRequest, Void>> columns = new ArrayList<>();
		columns.add(new AbstractColumn<PullRequest, Void>(Model.of("Pull Request")) {

			@Override
			public void populateItem(Item<ICellPopulator<PullRequest>> cellItem,
					String componentId, final IModel<PullRequest> rowModel) {
				PullRequest request = rowModel.getObject();
				Fragment fragment = new Fragment(componentId, "requestFrag", RequestListPage.this);
				fragment.add(new Label("id", "#" + request.getId()));
				fragment.add(new RequestLink("title", rowModel));
				fragment.add(new RequestStatusPanel("status", rowModel));
				fragment.add(new UserLink("submitter", new LoadableDetachableModel<User>() {

					@Override
					protected User load() {
						return rowModel.getObject().getSubmitter();
					}
					
				}, AvatarMode.NAME));
				fragment.add(new BranchLink("targetBranch", Model.of(request.getTarget())));
				fragment.add(new BranchLink("source", Model.of(request.getSource())));
				fragment.add(new Label("age", DateUtils.formatAge(request.getCreateDate())));
				fragment.add(new UserLink("assignee", new LoadableDetachableModel<User>() {

					@Override
					protected User load() {
						return rowModel.getObject().getSubmitter();
					}
					
				}, AvatarMode.NAME).setVisible(request.getAssignee() != null));
				
				cellItem.add(fragment);
				
				cellItem.add(AttributeAppender.append("class", "request"));
			}
			
		});

		IDataProvider<PullRequest> dataProvider = new IDataProvider<PullRequest>() {

			@Override
			public void detach() {
			}

			@Override
			public Iterator<? extends PullRequest> iterator(long first, long count) {
				RepositoryPage page = (RepositoryPage) getPage();
				
				EntityCriteria<PullRequest> criteria = searchOption.getCriteria(page.getRepository());
				criteria.addOrder(sortOption.getOrder());
				return GitPlex.getInstance(Dao.class).query(criteria, (int)first, (int)count).iterator();
			}

			@Override
			public long size() {
				RepositoryPage page = (RepositoryPage) getPage();
				return GitPlex.getInstance(Dao.class).count(searchOption.getCriteria(page.getRepository()));
			}

			@Override
			public IModel<PullRequest> model(PullRequest object) {
				final Long pullRequestId = object.getId();
				return new LoadableDetachableModel<PullRequest>() {

					@Override
					protected PullRequest load() {
						return GitPlex.getInstance(Dao.class).load(PullRequest.class, pullRequestId);
					}
					
				};
			}
			
		};
		DataTable<PullRequest, Void> dataTable = new DataTable<>("pullRequests", columns, 
				dataProvider, Constants.DEFAULT_PAGE_SIZE);
		dataTable.addTopToolbar(new NavigationToolbar(dataTable));
		dataTable.addBottomToolbar(new NoRecordsToolbar(dataTable));
		dataTable.addBottomToolbar(new NavigationToolbar(dataTable));
		add(dataTable);		
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(CssHeaderItem.forReference(
				new CssResourceReference(RequestListPage.class, "request-list.css")));
	}

	public static PageParameters paramsOf(Repository repository, SearchOption searchOption, SortOption sortOption) {
		PageParameters params = paramsOf(repository);
		searchOption.fillPageParams(params);
		sortOption.fillPageParams(params);
		return params;
	}
	
	@Override
	protected void onSelect(AjaxRequestTarget target, Repository repository) {
		setResponsePage(RequestListPage.class, paramsOf(repository));
	}
	
}
