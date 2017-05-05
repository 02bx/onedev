package com.gitplex.server.web.page.depot.setting;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.web.component.tabbable.PageTab;
import com.gitplex.server.web.component.tabbable.Tabbable;
import com.gitplex.server.web.page.depot.DepotPage;
import com.gitplex.server.web.page.depot.setting.authorization.DepotCollaboratorListPage;
import com.gitplex.server.web.page.depot.setting.authorization.DepotEffectivePrivilegePage;
import com.gitplex.server.web.page.depot.setting.authorization.DepotTeamAuthorizationPage;
import com.gitplex.server.web.page.depot.setting.branchprotection.BranchProtectionPage;
import com.gitplex.server.web.page.depot.setting.commitmessagetransform.CommitMessageTransformPage;
import com.gitplex.server.web.page.depot.setting.general.GeneralSettingPage;
import com.gitplex.server.web.page.depot.setting.tagprotection.TagProtectionPage;

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
		tabs.add(new DepotSettingTab("General Setting", "fa fa-fw fa-sliders", GeneralSettingPage.class));
		if (getAccount().isOrganization()) {
			tabs.add(new DepotSettingTab("Team Authorizations", "fa fa-fw fa-group", DepotTeamAuthorizationPage.class));
			tabs.add(new DepotSettingTab("Collaborators", "fa fa-fw fa-user", DepotCollaboratorListPage.class));
			tabs.add(new DepotSettingTab("Effective Privileges", "fa fa-fw fa-key", DepotEffectivePrivilegePage.class));
		} else {
			/*
			 * Team list page is not applicable for user accounts, and effective privilege
			 * page does not make sense for user accounts, as permissions assigned in 
			 * collaborator list page is actually effective as:
			 * 1. site administrator and depot owner is not allowed to be added as collaborator
			 * 2. no other source can access user's depot as user account does not have 
			 * concept of organization member  
			 */
			tabs.add(new DepotSettingTab("Collaborators", "fa fa-fw fa-user", DepotCollaboratorListPage.class));
		}
		tabs.add(new DepotSettingTab("Commit Message Transform", "fa fa-fw fa-exchange", CommitMessageTransformPage.class));
		tabs.add(new DepotSettingTab("Branch Protection", "fa fa-fw fa-lock", BranchProtectionPage.class));
		tabs.add(new DepotSettingTab("Tag Protection", "fa fa-fw fa-lock", TagProtectionPage.class));
		
		add(new Tabbable("depotSettingTabs", tabs));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(CssHeaderItem.forReference(new DepotSettingResourceReference()));
		String script = String.format(""
				+ "var $depotSetting = $('#depot-setting');"
				+ "$depotSetting.find('>table').height($depotSetting.parent().outerHeight());");
		response.render(OnDomReadyHeaderItem.forScript(script));
	}
	
}
