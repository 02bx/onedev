package com.pmease.gitop.web.page.project.settings;

import org.apache.wicket.request.mapper.parameter.PageParameters;

@SuppressWarnings("serial")
public class RepositoryAuditLogPage extends AbstractRepositorySettingPage {

	public RepositoryAuditLogPage(PageParameters params) {
		super(params);
	}

	@Override
	protected String getPageTitle() {
		return "Audit Log - " + getProject();
	}
}
