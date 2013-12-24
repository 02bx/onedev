package com.pmease.gitop.web.page.project.source.tags;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.gitop.web.page.project.ProjectCategoryPage;

@SuppressWarnings("serial")
public class TagsPage extends ProjectCategoryPage {

	public TagsPage(PageParameters params) {
		super(params);
	}

	@Override
	protected String getPageTitle() {
		return getProject() + " - tags";
	}

}
