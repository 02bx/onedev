package com.gitplex.server.web;

import org.apache.wicket.core.request.handler.BookmarkablePageRequestHandler;
import org.apache.wicket.core.request.handler.IPageClassRequestHandler;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.CompoundRequestMapper;

import com.gitplex.server.web.page.account.collaborators.AccountCollaboratorListPage;
import com.gitplex.server.web.page.account.collaborators.CollaboratorDepotListPage;
import com.gitplex.server.web.page.account.collaborators.CollaboratorEffectivePrivilegePage;
import com.gitplex.server.web.page.account.collaborators.CollaboratorPrivilegeSourcePage;
import com.gitplex.server.web.page.account.members.MemberEffectivePrivilegePage;
import com.gitplex.server.web.page.account.members.MemberListPage;
import com.gitplex.server.web.page.account.members.MemberPrivilegeSourcePage;
import com.gitplex.server.web.page.account.members.MemberTeamListPage;
import com.gitplex.server.web.page.account.members.NewMembersPage;
import com.gitplex.server.web.page.account.overview.AccountOverviewPage;
import com.gitplex.server.web.page.account.overview.NewDepotPage;
import com.gitplex.server.web.page.account.overview.NewOrganizationPage;
import com.gitplex.server.web.page.account.setting.AvatarEditPage;
import com.gitplex.server.web.page.account.setting.PasswordEditPage;
import com.gitplex.server.web.page.account.setting.ProfileEditPage;
import com.gitplex.server.web.page.account.tasks.TaskListPage;
import com.gitplex.server.web.page.account.teams.NewTeamPage;
import com.gitplex.server.web.page.account.teams.TeamDepotListPage;
import com.gitplex.server.web.page.account.teams.TeamEditPage;
import com.gitplex.server.web.page.account.teams.TeamListPage;
import com.gitplex.server.web.page.account.teams.TeamMemberListPage;
import com.gitplex.server.web.page.admin.DatabaseBackupPage;
import com.gitplex.server.web.page.admin.MailSettingPage;
import com.gitplex.server.web.page.admin.SecuritySettingPage;
import com.gitplex.server.web.page.admin.SystemSettingPage;
import com.gitplex.server.web.page.admin.account.NewUserPage;
import com.gitplex.server.web.page.admin.account.UserListPage;
import com.gitplex.server.web.page.depot.NoBranchesPage;
import com.gitplex.server.web.page.depot.blob.DepotBlobPage;
import com.gitplex.server.web.page.depot.branches.DepotBranchesPage;
import com.gitplex.server.web.page.depot.commit.CommitDetailPage;
import com.gitplex.server.web.page.depot.commit.DepotCommitsPage;
import com.gitplex.server.web.page.depot.compare.RevisionComparePage;
import com.gitplex.server.web.page.depot.pullrequest.newrequest.NewRequestPage;
import com.gitplex.server.web.page.depot.pullrequest.requestdetail.changes.RequestChangesPage;
import com.gitplex.server.web.page.depot.pullrequest.requestdetail.codecomments.RequestCodeCommentsPage;
import com.gitplex.server.web.page.depot.pullrequest.requestdetail.mergepreview.MergePreviewPage;
import com.gitplex.server.web.page.depot.pullrequest.requestdetail.overview.RequestOverviewPage;
import com.gitplex.server.web.page.depot.pullrequest.requestlist.RequestListPage;
import com.gitplex.server.web.page.depot.setting.authorization.DepotCollaboratorListPage;
import com.gitplex.server.web.page.depot.setting.authorization.DepotEffectivePrivilegePage;
import com.gitplex.server.web.page.depot.setting.authorization.DepotTeamAuthorizationPage;
import com.gitplex.server.web.page.depot.setting.branchprotection.BranchProtectionPage;
import com.gitplex.server.web.page.depot.setting.commitmessagetransform.CommitMessageTransformPage;
import com.gitplex.server.web.page.depot.setting.general.GeneralSettingPage;
import com.gitplex.server.web.page.depot.setting.tagprotection.TagProtectionPage;
import com.gitplex.server.web.page.depot.tags.DepotTagsPage;
import com.gitplex.server.web.page.init.ServerInitPage;
import com.gitplex.server.web.page.init.WelcomePage;
import com.gitplex.server.web.page.layout.CreateDepotPage;
import com.gitplex.server.web.page.security.ForgetPage;
import com.gitplex.server.web.page.security.LoginPage;
import com.gitplex.server.web.page.security.LogoutPage;
import com.gitplex.server.web.page.security.RegisterPage;
import com.gitplex.server.web.page.test.TestPage;
import com.gitplex.server.web.util.mapper.DepotMapper;
import com.gitplex.server.web.util.mapper.DepotResourceMapper;
import com.gitplex.server.web.util.mapper.MapperUtils;
import com.gitplex.server.web.util.mapper.WebPageMapper;
import com.gitplex.server.web.util.resource.ArchiveResourceReference;
import com.gitplex.server.web.util.resource.AttachmentResourceReference;
import com.gitplex.server.web.util.resource.RawBlobResourceReference;

public class UrlMapper extends CompoundRequestMapper {

	public UrlMapper(WebApplication app) {
		add(new WebPageMapper("init", ServerInitPage.class));
		add(new WebPageMapper("welcome", WelcomePage.class));
		addAdministrationPages();
		addAccountPages();
		addDepotPages();
		addSecurityPages();
		
		add(new WebPageMapper("test", TestPage.class));
		add(new WebPageMapper("new-repository", CreateDepotPage.class));
		
		addResources();
	}

	private void addResources() {
		add(new DepotResourceMapper("${account}/${depot}/archive/${revision}", new ArchiveResourceReference()));
		add(new DepotResourceMapper("${account}/${depot}/raw/${revision}/${path}", new RawBlobResourceReference()));
		add(new DepotResourceMapper("${account}/${depot}/attachment/${uuid}/${attachment}", 
				new AttachmentResourceReference()));
	}
	
	private void addSecurityPages() {
		add(new WebPageMapper("login", LoginPage.class));
		add(new WebPageMapper("logout", LogoutPage.class));
		add(new WebPageMapper("register", RegisterPage.class));
		add(new WebPageMapper("forget", ForgetPage.class));
	}
	
	private void addAdministrationPages() {
		add(new WebPageMapper("administration/users", UserListPage.class));
		add(new WebPageMapper("administration/users/new", NewUserPage.class));
		add(new WebPageMapper("administration/settings/system", SystemSettingPage.class));
		add(new WebPageMapper("administration/settings/mail", MailSettingPage.class));
		add(new WebPageMapper("administration/settings/backup", DatabaseBackupPage.class));
		add(new WebPageMapper("administration/settings/security", SecuritySettingPage.class));
	}
	
	private void addAccountPages() {
		add(new WebPageMapper("${account}", AccountOverviewPage.class) {

			@Override
			public IRequestHandler mapRequest(Request request) {
				if (MapperUtils.getAccountSegments(request.getUrl()) == 1)
					return super.mapRequest(request);
				else
					return null;
			}
			
		});
		
		add(new WebPageMapper("accounts/${account}/new-depot", NewDepotPage.class));
		add(new WebPageMapper("accounts/${account}/new-organization", NewOrganizationPage.class));
		add(new WebPageMapper("accounts/${account}/tasks", TaskListPage.class));
		add(new WebPageMapper("accounts/${account}/settings/profile", ProfileEditPage.class));
		add(new WebPageMapper("accounts/${account}/settings/avatar", AvatarEditPage.class));
		add(new WebPageMapper("accounts/${account}/settings/password", PasswordEditPage.class));

		add(new WebPageMapper("accounts/${account}/members", MemberListPage.class));
		add(new WebPageMapper("accounts/${account}/members/${member}/teams", MemberTeamListPage.class));
		add(new WebPageMapper("accounts/${account}/members/${member}/depots", MemberEffectivePrivilegePage.class));
		add(new WebPageMapper("accounts/${account}/members/${member}/depots/${depot}", MemberPrivilegeSourcePage.class));
		add(new WebPageMapper("accounts/${account}/members/new", NewMembersPage.class));
		add(new WebPageMapper("accounts/${account}/teams", TeamListPage.class));
		add(new WebPageMapper("accounts/${account}/teams/new", NewTeamPage.class));
		add(new WebPageMapper("accounts/${account}/teams/${team}/setting", TeamEditPage.class));
		add(new WebPageMapper("accounts/${account}/teams/${team}/members", TeamMemberListPage.class));
		add(new WebPageMapper("accounts/${account}/teams/${team}/depots", TeamDepotListPage.class));

		add(new WebPageMapper("accounts/${account}/collaborators", AccountCollaboratorListPage.class));
		add(new WebPageMapper(
				"accounts/${account}/collaborators/${collaborator}/depots", 
				CollaboratorDepotListPage.class));
		add(new WebPageMapper(
				"accounts/${account}/collaborators/${collaborator}/effective", 
				CollaboratorEffectivePrivilegePage.class));
		add(new WebPageMapper(
				"accounts/${account}/collaborators/${collaborator}/effective/${depot}", 
				CollaboratorPrivilegeSourcePage.class));
	}

	private void addDepotPages() {
		add(new WebPageMapper("${account}/${depot}", DepotBlobPage.class) {

			@Override
			public IRequestHandler mapRequest(Request request) {
				if (MapperUtils.getDepotSegments(request.getUrl()) == 2)
					return super.mapRequest(request);
				else
					return null;
			}
			
		});

		add(new DepotMapper("${account}/${depot}/blob/#{revision}/#{path}", DepotBlobPage.class) {
			
			/*
			 * This logic is added to prevent url "/<account>/<depot>" from being redirected to 
			 * "/<account>/<depot>/blob"
			 */
			@Override
			public Url mapHandler(IRequestHandler requestHandler) {
				if (requestHandler instanceof BookmarkablePageRequestHandler 
						|| requestHandler instanceof RenderPageRequestHandler) {
					IPageClassRequestHandler pageClassRequestHandler = (IPageClassRequestHandler) requestHandler;
					if (pageClassRequestHandler.getPageClass() == DepotBlobPage.class 
							&& pageClassRequestHandler.getPageParameters().get("revision").toString() == null) {
						return null;
					}
				}
				return super.mapHandler(requestHandler);
			}
			
		});
		add(new DepotMapper("${account}/${depot}/commit/${revision}", CommitDetailPage.class));
		add(new DepotMapper("${account}/${depot}/commits", DepotCommitsPage.class));
		add(new DepotMapper("${account}/${depot}/compare", RevisionComparePage.class));

		add(new DepotMapper("${account}/${depot}/branches", DepotBranchesPage.class));
		add(new DepotMapper("${account}/${depot}/tags", DepotTagsPage.class));

		add(new DepotMapper("${account}/${depot}/pulls", RequestListPage.class));
		add(new DepotMapper("${account}/${depot}/pulls/new", NewRequestPage.class));
		add(new DepotMapper("${account}/${depot}/pulls/${request}", RequestOverviewPage.class));
		add(new DepotMapper(
				"${account}/${depot}/pulls/${request}/overview", RequestOverviewPage.class));
		add(new DepotMapper(
				"${account}/${depot}/pulls/${request}/code-comments", RequestCodeCommentsPage.class));
		add(new DepotMapper(
				"${account}/${depot}/pulls/${request}/changes", RequestChangesPage.class));
		add(new DepotMapper(
				"${account}/${depot}/pulls/${request}/merge-preview", MergePreviewPage.class));

		add(new DepotMapper("${account}/${depot}/settings/general", GeneralSettingPage.class));
		add(new DepotMapper("${account}/${depot}/settings/team-authorization", DepotTeamAuthorizationPage.class));
		add(new DepotMapper("${account}/${depot}/settings/collaborators", DepotCollaboratorListPage.class));
		add(new DepotMapper("${account}/${depot}/settings/effective-privilege", DepotEffectivePrivilegePage.class));
		add(new DepotMapper("${account}/${depot}/settings/branch-protection", BranchProtectionPage.class));
		add(new DepotMapper("${account}/${depot}/settings/tag-protection", TagProtectionPage.class));
		add(new DepotMapper("${account}/${depot}/settings/commit-message-transform", CommitMessageTransformPage.class));
		
		add(new DepotMapper("${account}/${depot}/no-branches", NoBranchesPage.class));
	}

}
