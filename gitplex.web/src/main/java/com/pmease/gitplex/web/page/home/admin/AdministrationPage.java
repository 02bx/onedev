package com.pmease.gitplex.web.page.home.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.resource.CssResourceReference;

import com.pmease.commons.wicket.component.tabbable.PageTab;
import com.pmease.commons.wicket.component.tabbable.Tabbable;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.page.layout.LayoutPage;

@SuppressWarnings("serial")
public abstract class AdministrationPage extends LayoutPage {

	@Override
	protected boolean isPermitted() {
		return SecurityUtils.canManageSystem();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		List<PageTab> tabs = new ArrayList<>();
		tabs.add(new AdministrationTab("All Accounts", "fa fa-fw fa-users", AccountListPage.class));
		tabs.add(new AdministrationTab("System Setting", "fa fa-fw fa-gear", SystemSettingPage.class));
		tabs.add(new AdministrationTab("Mail Setting", "fa fa-fw fa-envelope", MailSettingPage.class));
		tabs.add(new AdministrationTab("QoS Setting", "fa fa-fw fa-signal", QosSettingPage.class));
		
		add(new Tabbable("tabs", tabs));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(AdministrationPage.class, "administration.css")));
	}

	@Override
	protected Component newContextHead(String componentId) {
		return new Label(componentId, "Administration");
	}

}
