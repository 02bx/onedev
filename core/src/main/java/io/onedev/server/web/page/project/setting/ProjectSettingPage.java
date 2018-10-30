package io.onedev.server.web.page.project.setting;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.component.sidebar.SideBar;
import io.onedev.server.web.component.tabbable.PageTab;
import io.onedev.server.web.component.tabbable.Tab;
import io.onedev.server.web.page.project.ProjectPage;
import io.onedev.server.web.page.project.setting.avatar.AvatarEditPage;
import io.onedev.server.web.page.project.setting.branchprotection.BranchProtectionPage;
import io.onedev.server.web.page.project.setting.commitmessagetransform.CommitMessageTransformPage;
import io.onedev.server.web.page.project.setting.configuration.ConfigurationEditPage;
import io.onedev.server.web.page.project.setting.configuration.ConfigurationListPage;
import io.onedev.server.web.page.project.setting.configuration.NewConfigurationPage;
import io.onedev.server.web.page.project.setting.general.GeneralSettingPage;
import io.onedev.server.web.page.project.setting.issueworkflow.fields.IssueFieldsPage;
import io.onedev.server.web.page.project.setting.issueworkflow.states.IssueStatesPage;
import io.onedev.server.web.page.project.setting.issueworkflow.statetransitions.StateTransitionsPage;
import io.onedev.server.web.page.project.setting.tagprotection.TagProtectionPage;
import io.onedev.server.web.page.project.setting.team.NewTeamPage;
import io.onedev.server.web.page.project.setting.team.TeamEditPage;
import io.onedev.server.web.page.project.setting.team.TeamListPage;
import io.onedev.server.web.page.project.setting.webhook.WebHooksPage;

@SuppressWarnings("serial")
public abstract class ProjectSettingPage extends ProjectPage {

	public ProjectSettingPage(PageParameters params) {
		super(params);
	}

	@Override
	protected boolean isPermitted() {
		return SecurityUtils.canAdministrate(getProject().getFacade());
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new SideBar("projectSettingSidebar", null) {

			@Override
			protected List<? extends Tab> newTabs() {
				List<PageTab> tabs = new ArrayList<>();
				tabs.add(new ProjectSettingTab("General Setting", "fa fa-fw fa-sliders", GeneralSettingPage.class));
				tabs.add(new ProjectSettingTab("Edit Avatar", "fa fa-fw fa-picture-o", AvatarEditPage.class));
				tabs.add(new ProjectSettingTab("Teams", "fa fa-fw fa-group", TeamListPage.class, NewTeamPage.class, TeamEditPage.class));
				tabs.add(new ProjectSettingTab("Branch Protection", "fa fa-fw fa-lock", BranchProtectionPage.class));
				tabs.add(new ProjectSettingTab("Tag Protection", "fa fa-fw fa-lock", TagProtectionPage.class));
				tabs.add(new ProjectSettingTab("Commit Message Transform", "fa fa-fw fa-comments", CommitMessageTransformPage.class));
				tabs.add(new ProjectSettingTab("Issue Fields", "fa fa-fw fa-list-ul", IssueFieldsPage.class));
				tabs.add(new ProjectSettingTab("Issue States", "fa fa-fw fa-check", IssueStatesPage.class));
				tabs.add(new ProjectSettingTab("Issue State Transitions", "fa fa-fw fa-exchange", StateTransitionsPage.class));
				tabs.add(new ProjectSettingTab("Web Hooks", "fa fa-fw fa-volume-up", WebHooksPage.class));
				tabs.add(new ProjectSettingTab("Build Configurations", "fa fa-fw fa-cube", ConfigurationListPage.class, 
						NewConfigurationPage.class, ConfigurationEditPage.class));
				return tabs;
			}
			
		});
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(CssHeaderItem.forReference(new ProjectSettingResourceReference()));
		String script = String.format(""
				+ "var $projectSetting = $('#project-setting');"
				+ "$projectSetting.find('>table').height($projectSetting.parent().outerHeight());");
		response.render(OnDomReadyHeaderItem.forScript(script));
	}
	
}
