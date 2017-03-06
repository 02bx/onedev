package com.gitplex.server.web.util.depotaccess;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.gitplex.server.model.OrganizationMembership;
import com.gitplex.server.web.component.link.ViewStateAwarePageLink;
import com.gitplex.server.web.page.account.members.MemberTeamListPage;

@SuppressWarnings("serial")
public class IsOrganizationAdminPanel extends Panel {

	private final IModel<OrganizationMembership> membershipModel;
	
	public IsOrganizationAdminPanel(String id, IModel<OrganizationMembership> membershipModel) {
		super(id);
		this.membershipModel = membershipModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new ViewStateAwarePageLink<Void>("member", 
				MemberTeamListPage.class, MemberTeamListPage.paramsOf(membershipModel.getObject())));
		add(new Label("organizationName", membershipModel.getObject().getOrganization().getName()));
	}

	@Override
	protected void onDetach() {
		membershipModel.detach();
		
		super.onDetach();
	}

}
