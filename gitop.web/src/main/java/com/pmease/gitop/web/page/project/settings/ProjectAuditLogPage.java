package com.pmease.gitop.web.page.project.settings;

import org.apache.wicket.request.mapper.parameter.PageParameters;

@SuppressWarnings("serial")
public class ProjectAuditLogPage extends AbstractProjectSettingPage {

	public ProjectAuditLogPage(PageParameters params) {
		super(params);
	}

	@Override
	protected Category getCategory() {
		return Category.AUDIT_LOG;
	}

}
