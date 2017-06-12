package com.gitplex.server.web.page.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.revwalk.RevCommit;

import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.CacheManager;
import com.gitplex.server.manager.ProjectManager;
import com.gitplex.server.model.Project;
import com.gitplex.server.security.ProjectPrivilege;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.util.facade.GroupAuthorizationFacade;
import com.gitplex.server.util.facade.ProjectFacade;
import com.gitplex.server.util.facade.UserAuthorizationFacade;
import com.gitplex.server.web.ComponentRenderer;
import com.gitplex.server.web.WebConstants;
import com.gitplex.server.web.behavior.OnTypingDoneBehavior;
import com.gitplex.server.web.component.avatar.AvatarLink;
import com.gitplex.server.web.component.link.UserLink;
import com.gitplex.server.web.component.link.ViewStateAwarePageLink;
import com.gitplex.server.web.page.layout.LayoutPage;
import com.gitplex.server.web.page.project.blob.ProjectBlobPage;
import com.gitplex.server.web.page.project.commit.CommitDetailPage;
import com.gitplex.server.web.page.project.setting.general.GeneralSettingPage;
import com.gitplex.server.web.util.DateUtils;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;

@SuppressWarnings("serial")
public class ProjectListPage extends LayoutPage {

	private boolean showOrphanProjects;
	
	private final IModel<List<ProjectFacade>> orphanProjectsModel = new LoadableDetachableModel<List<ProjectFacade>>() {

		@Override
		protected List<ProjectFacade> load() {
			CacheManager cacheManager = GitPlex.getInstance(CacheManager.class);
			List<ProjectFacade> projects = new ArrayList<>();
			
			Set<Long> projectIdsWithExplicitAdministrators = new HashSet<>();
			for (UserAuthorizationFacade authorization: cacheManager.getUserAuthorizations().values()) {
				if (authorization.getPrivilege() == ProjectPrivilege.ADMIN)
					projectIdsWithExplicitAdministrators.add(authorization.getProjectId());
			}
			for (GroupAuthorizationFacade authorization: cacheManager.getGroupAuthorizations().values()) {
				if (authorization.getPrivilege() == ProjectPrivilege.ADMIN)
					projectIdsWithExplicitAdministrators.add(authorization.getProjectId());
			}
			
			for (ProjectFacade project: cacheManager.getProjects().values()) {
				if (!projectIdsWithExplicitAdministrators.contains(project.getId()) 
						&& project.matchesQuery(searchInput)) {
					projects.add(project);
				}
			}
			projects.sort(ProjectFacade::compareLastVisit);
			return projects;
		}
		
	};
	
	private final IModel<List<ProjectFacade>> projectsModel = new LoadableDetachableModel<List<ProjectFacade>>() {

		@Override
		protected List<ProjectFacade> load() {
			List<ProjectFacade> projects = new ArrayList<>(GitPlex.getInstance(ProjectManager.class)
					.getAccessibleProjects(getLoginUser()));
			for (Iterator<ProjectFacade> it = projects.iterator(); it.hasNext();) {
				if (!it.next().matchesQuery(searchInput))
					it.remove();
			}
			projects.sort(ProjectFacade::compareLastVisit);
			return projects;
		}
		
	};
	
	private DataTable<ProjectFacade, Void> projectsTable;
	
	private String searchInput;
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		TextField<String> searchField;
		add(searchField = new TextField<String>("filterProjects", Model.of("")));
		searchField.add(new OnTypingDoneBehavior(100) {

			@Override
			protected void onTypingDone(AjaxRequestTarget target) {
				searchInput = searchField.getInput();
				target.add(projectsTable);
			}

		});
		add(new BookmarkablePageLink<Void>("createProject", NewProjectPage.class) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canCreateProjects());
			}
			
		});

		WebMarkupContainer orphanProjectsNote = new WebMarkupContainer("orphanProjectsNote") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(showOrphanProjects);
			}
			
		};
		orphanProjectsNote.setOutputMarkupPlaceholderTag(true);
		add(orphanProjectsNote);
		
		add(new AjaxLink<Void>("showOrphanProjects") {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return showOrphanProjects?"active":"";
					}
					
				}));
				setOutputMarkupPlaceholderTag(true);				
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				showOrphanProjects = !showOrphanProjects;
				target.add(this);
				target.add(projectsTable);
				target.add(orphanProjectsNote);
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.isAdministrator() && !orphanProjectsModel.getObject().isEmpty());
			}
			
		});
		
		List<IColumn<ProjectFacade, Void>> columns = new ArrayList<>();
		
		columns.add(new AbstractColumn<ProjectFacade, Void>(Model.of("Project")) {

			@Override
			public void populateItem(Item<ICellPopulator<ProjectFacade>> cellItem, String componentId, 
					IModel<ProjectFacade> rowModel) {
				Fragment fragment = new Fragment(componentId, "projectFrag", ProjectListPage.this);
				Project project = GitPlex.getInstance(ProjectManager.class).load(rowModel.getObject().getId());
				Link<Void> link; 
				if (showOrphanProjects) {
					link = new BookmarkablePageLink<Void>("link", GeneralSettingPage.class, 
							GeneralSettingPage.paramsOf(project)); 
				} else {
					link = new BookmarkablePageLink<Void>("link", ProjectBlobPage.class, 
							ProjectBlobPage.paramsOf(project)); 
				}
				link.add(new Label("name", project.getName()));
				fragment.add(link);
				cellItem.add(fragment);
				cellItem.add(AttributeAppender.append("class", "project"));
			}
		});

		columns.add(new AbstractColumn<ProjectFacade, Void>(Model.of("Last Author")) {

			@Override
			public void populateItem(Item<ICellPopulator<ProjectFacade>> cellItem, String componentId, 
					IModel<ProjectFacade> rowModel) {
				Project project = GitPlex.getInstance(ProjectManager.class).load(rowModel.getObject().getId());
				RevCommit lastCommit = project.getLastCommit();
				if (lastCommit != null) {
					Fragment fragment = new Fragment(componentId, "authorFrag", ProjectListPage.this);
					fragment.add(new AvatarLink("avatar", lastCommit.getAuthorIdent()));
					fragment.add(new UserLink("name", lastCommit.getAuthorIdent()));
					cellItem.add(fragment);
				} else {
					cellItem.add(new Label(componentId, "<i>N/A</i>").setEscapeModelStrings(false));
				}
				cellItem.add(AttributeAppender.append("class", "author"));
			}
		});
		
		columns.add(new AbstractColumn<ProjectFacade, Void>(Model.of("Last Commit Message")) {

			@Override
			public void populateItem(Item<ICellPopulator<ProjectFacade>> cellItem, String componentId, 
					IModel<ProjectFacade> rowModel) {
				Project project = GitPlex.getInstance(ProjectManager.class).load(rowModel.getObject().getId());
				RevCommit lastCommit = project.getLastCommit();
				if (lastCommit != null) {
					Fragment fragment = new Fragment(componentId, "commitMessageFrag", ProjectListPage.this);
					PageParameters params = CommitDetailPage.paramsOf(project, lastCommit.name());
					Link<Void> link = new BookmarkablePageLink<Void>("link", CommitDetailPage.class, params);
					link.add(new Label("message", lastCommit.getShortMessage()));
					fragment.add(link);
					cellItem.add(fragment);
				} else {
					cellItem.add(new Label(componentId, "<i>N/A</i>").setEscapeModelStrings(false));
				}
				cellItem.add(AttributeAppender.append("class", "commit-message"));
			}
		});
		
		columns.add(new AbstractColumn<ProjectFacade, Void>(Model.of("Last Commit Date")) {

			@Override
			public void populateItem(Item<ICellPopulator<ProjectFacade>> cellItem, String componentId, 
					IModel<ProjectFacade> rowModel) {
				Project project = GitPlex.getInstance(ProjectManager.class).load(rowModel.getObject().getId());
				RevCommit lastCommit = project.getLastCommit();
				if (lastCommit != null) {
					cellItem.add(new Label(componentId, DateUtils.formatAge(lastCommit.getCommitterIdent().getWhen())));
				} else {
					cellItem.add(new Label(componentId, "<i>N/A</i>").setEscapeModelStrings(false));
				}
				cellItem.add(AttributeAppender.append("class", "commit-date"));
			}
		});
		
		SortableDataProvider<ProjectFacade, Void> dataProvider = new SortableDataProvider<ProjectFacade, Void>() {

			@Override
			public Iterator<? extends ProjectFacade> iterator(long first, long count) {
				List<ProjectFacade> projects;
				if (showOrphanProjects)
					projects = orphanProjectsModel.getObject();
				else
					projects = projectsModel.getObject();
				if (first + count <= projects.size())
					return projects.subList((int)first, (int)(first+count)).iterator();
				else
					return projects.subList((int)first, projects.size()).iterator();
			}

			@Override
			public long size() {
				if (showOrphanProjects)
					return orphanProjectsModel.getObject().size();
				else
					return projectsModel.getObject().size();
			}

			@Override
			public IModel<ProjectFacade> model(ProjectFacade object) {
				return Model.of(object);
			}
		};
		
		projectsTable = new DataTable<ProjectFacade, Void>("projects", columns, dataProvider, WebConstants.PAGE_SIZE);		
		projectsTable.addBottomToolbar(new AjaxNavigationToolbar(projectsTable) {

			@Override
			protected PagingNavigator newPagingNavigator(String navigatorId, DataTable<?, ?> table) {
				return new BootstrapAjaxPagingNavigator(navigatorId, table);
			}
			
		});
		projectsTable.addBottomToolbar(new NoRecordsToolbar(projectsTable, Model.of("No Projects Found")));
		projectsTable.setOutputMarkupId(true);
		add(projectsTable);
	}

	@Override
	protected void onDetach() {
		orphanProjectsModel.detach();
		projectsModel.detach();
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new ProjectResourceReference()));
	}

	@Override
	protected List<ComponentRenderer> getBreadcrumbs() {
		List<ComponentRenderer> breadcrumbs = super.getBreadcrumbs();
		
		breadcrumbs.add(new ComponentRenderer() {

			@Override
			public Component render(String componentId) {
				return new ViewStateAwarePageLink<Void>(componentId, ProjectListPage.class) {

					@Override
					public IModel<?> getBody() {
						return Model.of("Projects");
					}
					
				};
			}
			
		});

		return breadcrumbs;
	}

}
