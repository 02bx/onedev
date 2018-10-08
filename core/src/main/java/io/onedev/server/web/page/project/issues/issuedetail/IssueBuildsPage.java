package io.onedev.server.web.page.project.issues.issuedetail;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.model.Issue;
import io.onedev.server.model.Project;
import io.onedev.server.search.entity.build.BuildQuery;
import io.onedev.server.search.entity.build.FixedIssueCriteria;
import io.onedev.server.web.component.build.list.BuildListPanel;
import io.onedev.server.web.util.PagingHistorySupport;
import io.onedev.server.web.util.QueryPosition;
import io.onedev.server.web.util.QuerySaveSupport;

@SuppressWarnings("serial")
public class IssueBuildsPage extends IssueDetailPage {

	private static final String PARAM_QUERY = "query";
	
	private static final String PARAM_CURRENT_PAGE = "currentPage";

	private String query;
	
	public IssueBuildsPage(PageParameters params) {
		super(params);
		query = params.get(PARAM_QUERY).toString();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new BuildListPanel("builds", new PropertyModel<String>(this, "query")) {

			@Override
			protected Project getProject() {
				return getIssue().getProject();
			}

			@Override
			protected BuildQuery getBaseQuery() {
				return new BuildQuery(new FixedIssueCriteria(getIssue()), new ArrayList<>());
			}

			@Override
			protected PagingHistorySupport getPagingHistorySupport() {
				return new PagingHistorySupport() {
					
					@Override
					public PageParameters newPageParameters(int currentPage) {
						PageParameters params = paramsOf(getIssue(), getPosition(), query);
						params.add(PARAM_CURRENT_PAGE, currentPage+1);
						return params;
					}
					
					@Override
					public int getCurrentPage() {
						return getPageParameters().get(PARAM_CURRENT_PAGE).toInt(1)-1;
					}
					
				};
			}

			@Override
			protected void onQueryUpdated(AjaxRequestTarget target) {
				setResponsePage(IssueBuildsPage.class, IssueBuildsPage.paramsOf(getIssue(), getPosition(), query));
			}

			@Override
			protected QuerySaveSupport getQuerySaveSupport() {
				return null;
			}

		});
	}

	public static PageParameters paramsOf(Issue issue, @Nullable QueryPosition position, @Nullable String query) {
		PageParameters params = paramsOf(issue, position);
		if (query != null)
			params.add(PARAM_QUERY, query);
		return params;
	}
	
}
