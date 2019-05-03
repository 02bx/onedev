package io.onedev.server.web.page.project.builds.list;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.BuildQuerySettingManager;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.model.BuildQuerySetting;
import io.onedev.server.model.Project;
import io.onedev.server.model.support.NamedBuildQuery;
import io.onedev.server.model.support.QuerySetting;
import io.onedev.server.search.entity.build.BuildQuery;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.component.build.list.BuildListPanel;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.page.project.ProjectPage;
import io.onedev.server.web.page.project.savedquery.NamedQueriesBean;
import io.onedev.server.web.page.project.savedquery.SaveQueryPanel;
import io.onedev.server.web.page.project.savedquery.SavedQueriesPanel;
import io.onedev.server.web.util.PagingHistorySupport;
import io.onedev.server.web.util.QuerySaveSupport;

@SuppressWarnings("serial")
public class BuildListPage extends ProjectPage {

	private static final String PARAM_CURRENT_PAGE = "currentPage";
	
	private static final String PARAM_QUERY = "query";
	
	private String query;
	
	public BuildListPage(PageParameters params) {
		super(params);
		query = params.get(PARAM_QUERY).toOptionalString();
		if (query != null && query.length() == 0) {
			List<String> queries = new ArrayList<>();
			if (getProject().getBuildQuerySettingOfCurrentUser() != null) { 
				for (NamedBuildQuery namedQuery: getProject().getBuildQuerySettingOfCurrentUser().getUserQueries())
					queries.add(namedQuery.getQuery());
			}
			for (NamedBuildQuery namedQuery: getProject().getSavedBuildQueries())
				queries.add(namedQuery.getQuery());
			query = null;
			for (String each: queries) {
				try {
					if (SecurityUtils.getUser() != null || !BuildQuery.parse(getProject(), each, true).needsLogin()) {  
						query = each;
						break;
					}
				} catch (Exception e) {
				}
			} 
		}
	}

	private BuildQuerySettingManager getBuildQuerySettingManager() {
		return OneDev.getInstance(BuildQuerySettingManager.class);		
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		SavedQueriesPanel<NamedBuildQuery> savedQueries;
		add(savedQueries = new SavedQueriesPanel<NamedBuildQuery>("side") {

			@Override
			protected NamedQueriesBean<NamedBuildQuery> newNamedQueriesBean() {
				return new NamedBuildQueriesBean();
			}

			@Override
			protected boolean needsLogin(NamedBuildQuery namedQuery) {
				return BuildQuery.parse(getProject(), namedQuery.getQuery(), true).needsLogin();
			}

			@Override
			protected Link<Void> newQueryLink(String componentId, NamedBuildQuery namedQuery) {
				return new BookmarkablePageLink<Void>(componentId, BuildListPage.class, BuildListPage.paramsOf(getProject(), namedQuery.getQuery()));
			}

			@Override
			protected QuerySetting<NamedBuildQuery> getQuerySetting() {
				return getProject().getBuildQuerySettingOfCurrentUser();
			}

			@Override
			protected ArrayList<NamedBuildQuery> getProjectQueries() {
				return getProject().getSavedBuildQueries();
			}

			@Override
			protected void onSaveQuerySetting(QuerySetting<NamedBuildQuery> querySetting) {
				getBuildQuerySettingManager().save((BuildQuerySetting) querySetting);
			}

			@Override
			protected void onSaveProjectQueries(ArrayList<NamedBuildQuery> projectQueries) {
				getProject().setSavedBuildQueries(projectQueries);
				OneDev.getInstance(ProjectManager.class).save(getProject());
			}

		});
		
		PagingHistorySupport pagingHistorySupport = new PagingHistorySupport() {

			@Override
			public PageParameters newPageParameters(int currentPage) {
				PageParameters params = paramsOf(getProject(), query);
				params.add(PARAM_CURRENT_PAGE, currentPage+1);
				return params;
			}
			
			@Override
			public int getCurrentPage() {
				return getPageParameters().get(PARAM_CURRENT_PAGE).toInt(1)-1;
			}
			
		};
		
		add(new BuildListPanel("main", new PropertyModel<String>(this, "query")) {

			@Override
			protected Project getProject() {
				return BuildListPage.this.getProject();
			}

			@Override
			protected PagingHistorySupport getPagingHistorySupport() {
				return pagingHistorySupport;
			}

			@Override
			protected void onQueryUpdated(AjaxRequestTarget target) {
				setResponsePage(BuildListPage.class, paramsOf(getProject(), query));
			}

			@Override
			protected QuerySaveSupport getQuerySaveSupport() {
				return new QuerySaveSupport() {

					@Override
					public void onSaveQuery(AjaxRequestTarget target) {
						new ModalPanel(target)  {

							@Override
							protected Component newContent(String id) {
								return new SaveQueryPanel(id) {

									@Override
									protected void onSaveForMine(AjaxRequestTarget target, String name) {
										BuildQuerySetting setting = getProject().getBuildQuerySettingOfCurrentUser();
										NamedBuildQuery namedQuery = setting.getUserQuery(name);
										if (namedQuery == null) {
											namedQuery = new NamedBuildQuery(name, query);
											setting.getUserQueries().add(namedQuery);
										} else {
											namedQuery.setQuery(query);
										}
										getBuildQuerySettingManager().save(setting);
										target.add(savedQueries);
										close();
									}

									@Override
									protected void onSaveForAll(AjaxRequestTarget target, String name) {
										NamedBuildQuery namedQuery = getProject().getSavedBuildQuery(name);
										if (namedQuery == null) {
											namedQuery = new NamedBuildQuery(name, query);
											getProject().getSavedBuildQueries().add(namedQuery);
										} else {
											namedQuery.setQuery(query);
										}
										OneDev.getInstance(ProjectManager.class).save(getProject());
										target.add(savedQueries);
										close();
									}

									@Override
									protected void onCancel(AjaxRequestTarget target) {
										close();
									}
									
								};
							}
							
						};
					}

					@Override
					public boolean isSavedQueriesVisible() {
						savedQueries.configure();
						return savedQueries.isVisible();
					}

				};
			}

		});
		
	}
	
	public static PageParameters paramsOf(Project project, @Nullable String query) {
		PageParameters params = paramsOf(project);
		if (query != null)
			params.add(PARAM_QUERY, query);
		return params;
	}
	
}
