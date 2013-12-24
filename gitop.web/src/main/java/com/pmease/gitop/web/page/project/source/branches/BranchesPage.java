package com.pmease.gitop.web.page.project.source.branches;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.gitop.web.page.project.ProjectCategoryPage;

@SuppressWarnings("serial")
public class BranchesPage extends ProjectCategoryPage {

	public BranchesPage(PageParameters params) {
		super(params);
	}

	@Override
	protected String getPageTitle() {
		return getProject() + " - branches";
	}

}
