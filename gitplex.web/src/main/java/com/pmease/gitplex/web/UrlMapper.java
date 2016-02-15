package com.pmease.gitplex.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.core.request.mapper.ResourceMapper;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.CompoundRequestMapper;

import com.pmease.commons.util.StringUtils;
import com.pmease.gitplex.core.validation.RepositoryNameValidator;
import com.pmease.gitplex.core.validation.UserNameValidator;
import com.pmease.gitplex.web.page.account.depots.AccountDepotsPage;
import com.pmease.gitplex.web.page.account.depots.NewAccountDepotPage;
import com.pmease.gitplex.web.page.account.notifications.AccountNotificationsPage;
import com.pmease.gitplex.web.page.account.setting.AvatarEditPage;
import com.pmease.gitplex.web.page.account.setting.PasswordEditPage;
import com.pmease.gitplex.web.page.account.setting.ProfileEditPage;
import com.pmease.gitplex.web.page.depot.NoCommitsPage;
import com.pmease.gitplex.web.page.depot.branches.DepotBranchesPage;
import com.pmease.gitplex.web.page.depot.commit.CommitDetailPage;
import com.pmease.gitplex.web.page.depot.commit.DepotCommitsPage;
import com.pmease.gitplex.web.page.depot.compare.RevisionComparePage;
import com.pmease.gitplex.web.page.depot.file.DepotFilePage;
import com.pmease.gitplex.web.page.depot.pullrequest.newrequest.NewRequestPage;
import com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.attachments.RequestAttachmentsPage;
import com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.compare.RequestComparePage;
import com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.overview.RequestOverviewPage;
import com.pmease.gitplex.web.page.depot.pullrequest.requestdetail.updates.RequestUpdatesPage;
import com.pmease.gitplex.web.page.depot.pullrequest.requestlist.RequestListPage;
import com.pmease.gitplex.web.page.depot.setting.gatekeeper.GateKeeperPage;
import com.pmease.gitplex.web.page.depot.setting.general.GeneralSettingPage;
import com.pmease.gitplex.web.page.depot.setting.integrationpolicy.IntegrationPolicyPage;
import com.pmease.gitplex.web.page.depot.tags.DepotTagsPage;
import com.pmease.gitplex.web.page.home.admin.AccountListPage;
import com.pmease.gitplex.web.page.home.admin.MailSettingPage;
import com.pmease.gitplex.web.page.home.admin.NewAccountPage;
import com.pmease.gitplex.web.page.home.admin.QosSettingPage;
import com.pmease.gitplex.web.page.home.admin.SystemSettingPage;
import com.pmease.gitplex.web.page.init.ServerInitPage;
import com.pmease.gitplex.web.page.security.ForgetPage;
import com.pmease.gitplex.web.page.security.LoginPage;
import com.pmease.gitplex.web.page.security.LogoutPage;
import com.pmease.gitplex.web.page.security.RegisterPage;
import com.pmease.gitplex.web.page.test.RunModePage;
import com.pmease.gitplex.web.page.test.TestPage;
import com.pmease.gitplex.web.resource.ArchiveResourceReference;
import com.pmease.gitplex.web.resource.AttachmentResourceReference;
import com.pmease.gitplex.web.resource.BlobResourceReference;

public class UrlMapper extends CompoundRequestMapper {

	public UrlMapper(WebApplication app) {
		add(new MountedMapper("init", ServerInitPage.class));
		addAdministrationPages();
		addAccountPages();
		addRepoPages();
		addSecurityPages();
		
		add(new MountedMapper("/test", TestPage.class));
		add(new MountedMapper("runmode", RunModePage.class));
		
		addResources();
	}

	private void addResources() {
		add(new ResourceMapper("${user}/${repo}/archive", new ArchiveResourceReference()) {

			@Override
			public int getCompatibilityScore(Request request) {
				return 3;
			}
			
		});
		add(new ResourceMapper("${user}/${repo}/raw", new BlobResourceReference()) {

			@Override
			public int getCompatibilityScore(Request request) {
				return 3;
			}
			
		});
		add(new ResourceMapper("${user}/${repo}/pulls/${request}/attachments/${attachment}", new AttachmentResourceReference()) {

			@Override
			public int getCompatibilityScore(Request request) {
				return 8;
			}
			
		});
	}
	
	private void addSecurityPages() {
		add(new MountedMapper("login", LoginPage.class));
		add(new MountedMapper("logout", LogoutPage.class));
		add(new MountedMapper("register", RegisterPage.class));
		add(new MountedMapper("forget", ForgetPage.class));
	}
	
	private void addAdministrationPages() {
		add(new MountedMapper("administration/accounts", AccountListPage.class));
		add(new MountedMapper("administration/accounts/new", NewAccountPage.class));
		add(new MountedMapper("administration/mail-setting", MailSettingPage.class));
		add(new MountedMapper("administration/system-setting", SystemSettingPage.class));
		add(new MountedMapper("administration/qos-setting", QosSettingPage.class));
	}
	
	public List<String> normalize(List<String> urlSegments) {
		List<String> normalized = new ArrayList<String>();
		for (String each: urlSegments) {
			each = StringUtils.remove(each, '/');
			if (each.length() != 0)
				normalized.add(each);
		}
		return normalized;
	}

	private void addAccountPages() {
		add(new MountedMapper("${user}", AccountDepotsPage.class) {

			@Override
			protected boolean urlStartsWith(Url url, String... segments) {
				List<String> urlSegments = normalize(url.getSegments());
				if (urlSegments.size() < 1)
					return false;
				String userName = urlSegments.get(0);
				
				return !UserNameValidator.getReservedNames().contains(userName);
			}

		});
		
		add(new MountedMapper("${user}/repositories", AccountDepotsPage.class));
		add(new MountedMapper("${user}/repositories/new", NewAccountDepotPage.class));
		add(new MountedMapper("${user}/notifications", AccountNotificationsPage.class));
		add(new MountedMapper("${user}/setting/profile", ProfileEditPage.class));
		add(new MountedMapper("${user}/setting/avatar", AvatarEditPage.class));
		add(new MountedMapper("${user}/setting/password", PasswordEditPage.class));
	}

	private void addRepoPages() {
		add(new MountedMapper("${user}/${repo}", DepotFilePage.class) {

			@Override
			protected boolean urlStartsWith(Url url, String... segments) {
				List<String> urlSegments = normalize(url.getSegments());
				if (urlSegments.size() < 2)
					return false;
				String userName = urlSegments.get(0);
				if (UserNameValidator.getReservedNames().contains(userName))
					return false;

				String repositoryName = urlSegments.get(1);
				return !RepositoryNameValidator.getReservedNames().contains(repositoryName);
			}

		});

//		add(new ParameterAwareMountedMapper("${user}/${repo}/browse", RepoFilePage.class));
		add(new MountedMapper("${user}/${repo}/commit", CommitDetailPage.class));
		add(new MountedMapper("${user}/${repo}/commits", DepotCommitsPage.class));
		add(new MountedMapper("${user}/${repo}/compare", RevisionComparePage.class));

		add(new MountedMapper("${user}/${repo}/branches", DepotBranchesPage.class));
		add(new MountedMapper("${user}/${repo}/tags", DepotTagsPage.class));

		add(new MountedMapper("${user}/${repo}/pulls", RequestListPage.class));
		add(new MountedMapper("${user}/${repo}/pulls/new", NewRequestPage.class));
		add(new MountedMapper("${user}/${repo}/pulls/${request}", RequestOverviewPage.class));
		add(new MountedMapper(
				"${user}/${repo}/pulls/${request}/overview", RequestOverviewPage.class));
		add(new MountedMapper(
				"${user}/${repo}/pulls/${request}/updates", RequestUpdatesPage.class));
		add(new MountedMapper(
				"${user}/${repo}/pulls/${request}/compare", RequestComparePage.class));
		add(new MountedMapper(
				"${user}/${repo}/pulls/${request}/attachments", RequestAttachmentsPage.class));

		add(new MountedMapper("${user}/${repo}/setting", GeneralSettingPage.class));
		add(new MountedMapper("${user}/${repo}/setting/general", GeneralSettingPage.class));
		add(new MountedMapper("${user}/${repo}/setting/gate-keeper", GateKeeperPage.class));
		add(new MountedMapper("${user}/${repo}/setting/integration-policy", IntegrationPolicyPage.class));
		
		add(new MountedMapper("${user}/${repo}/no-commits", NoCommitsPage.class));
	}

}
