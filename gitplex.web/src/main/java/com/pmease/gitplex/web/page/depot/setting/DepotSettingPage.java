package com.pmease.gitplex.web.page.depot.setting;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import com.pmease.commons.wicket.component.tabbable.PageTab;
import com.pmease.commons.wicket.component.tabbable.Tabbable;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.page.depot.DepotPage;
import com.pmease.gitplex.web.page.depot.setting.authorization.DepotAuthorizationPage;
import com.pmease.gitplex.web.page.depot.setting.authorization.DepotCollaboratorListPage;
import com.pmease.gitplex.web.page.depot.setting.authorization.DepotTeamListPage;
import com.pmease.gitplex.web.page.depot.setting.gatekeeper.GateKeeperPage;
import com.pmease.gitplex.web.page.depot.setting.general.GeneralSettingPage;
import com.pmease.gitplex.web.page.depot.setting.integrationpolicy.IntegrationPolicyPage;

@SuppressWarnings("serial")
public abstract class DepotSettingPage extends DepotPage {

	public DepotSettingPage(PageParameters params) {
		super(params);
	}

	@Override
	protected boolean isPermitted() {
		return SecurityUtils.canManage(getDepot());
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		List<PageTab> tabs = new ArrayList<>();
		tabs.add(new DepotSettingTab("General Setting", GeneralSettingPage.class));
		if (getAccount().isOrganization()) {
			tabs.add(new DepotSettingTab("Authorizations", 
					DepotTeamListPage.class, DepotAuthorizationPage.class));
		} else {
			/*
			 * Team list page is not applicable for user accounts, and effective privilege
			 * page does not make sense for user accounts, as permissions assigned in 
			 * collaborator list page is actually effective as:
			 * 1. site administrator and depot owner is not allowed to be added as collaborator
			 * 2. no other source can access user's depot as user account does not have 
			 * concept of organization member  
			 */
			tabs.add(new DepotSettingTab("Collaborators", DepotCollaboratorListPage.class));
		}
		tabs.add(new DepotSettingTab("Gate Keepers", GateKeeperPage.class));
		tabs.add(new DepotSettingTab("Integration Policies", IntegrationPolicyPage.class));
		
		add(new Tabbable("depotSettingTabs", tabs));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(CssHeaderItem.forReference(new CssResourceReference(DepotSettingPage.class, "depot-setting.css")));
	}
	
}
