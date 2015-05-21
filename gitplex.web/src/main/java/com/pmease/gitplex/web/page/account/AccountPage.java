package com.pmease.gitplex.web.page.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.wicket.behavior.menu.MenuBehavior;
import com.pmease.commons.wicket.behavior.menu.MenuItem;
import com.pmease.commons.wicket.behavior.menu.MenuPanel;
import com.pmease.commons.wicket.component.tabbable.PageTab;
import com.pmease.commons.wicket.component.tabbable.Tabbable;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.security.SecurityUtils;
import com.pmease.gitplex.web.component.avatar.AvatarByUser;
import com.pmease.gitplex.web.model.UserModel;
import com.pmease.gitplex.web.page.account.notifications.AccountNotificationsPage;
import com.pmease.gitplex.web.page.account.repositories.AccountReposPage;
import com.pmease.gitplex.web.page.account.setting.AvatarEditPage;
import com.pmease.gitplex.web.page.account.setting.PasswordEditPage;
import com.pmease.gitplex.web.page.account.setting.ProfileEditPage;
import com.pmease.gitplex.web.page.layout.LayoutPage;

@SuppressWarnings("serial")
public abstract class AccountPage extends LayoutPage {
	
	private static final String PARAM_USER = "user";
	
	protected final IModel<User> accountModel;
	
	public AccountPage(PageParameters params) {
		super(params);
		
		String name = params.get(PARAM_USER).toString();
		Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
		
		User user = GitPlex.getInstance(UserManager.class).findByName(name);
		if (user == null) 
			throw (new EntityNotFoundException("User " + name + " not found"));
		
		accountModel = new UserModel(user);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new AvatarByUser("avatar", accountModel, false));
		
		Label nameLabel = new Label("accountName", getAccount().getDisplayName());
		add(nameLabel);
		
		WebMarkupContainer accountsMenuTrigger = new WebMarkupContainer("accountsMenuTrigger");
		
		MenuPanel accountsMenu = new MenuPanel("accountsMenu") {

			@Override
			protected List<MenuItem> getMenuItems() {
				List<User> accounts = new ArrayList<>();
				for (User account: GitPlex.getInstance(Dao.class).allOf(User.class)) {
					if (!account.equals(getAccount()))
						accounts.add(account);
				}
				Collections.sort(accounts, new Comparator<User>() {

					@Override
					public int compare(User user1, User user2) {
						return user1.getDisplayName().compareTo(user2.getDisplayName());
					}
					
				});
				List<MenuItem> menuItems = new ArrayList<>();
				for (User account: accounts) {
					final UserModel accountModel = new UserModel(account);
					menuItems.add(new MenuItem() {

						@Override
						public Component newContent(String componentId) {
							Fragment fragment = new Fragment(componentId, "accountsMenuItemFrag", AccountPage.this) {

								@Override
								protected void onDetach() {
									accountModel.detach();
									
									super.onDetach();
								}
								
							};
							Link<Void> link = new Link<Void>("link") {

								@Override
								public void onClick() {
									setResponsePage(getPage().getClass(), AccountPage.paramsOf(accountModel.getObject()));
								}
								
							};
							link.add(new AvatarByUser("avatar", accountModel, false));
							link.add(new Label("name", accountModel.getObject().getDisplayName()));
							fragment.add(link);
							return fragment;
						}

					});
				}
				return menuItems;
			}
			
		};
		add(accountsMenu);
		accountsMenuTrigger.add(new MenuBehavior(accountsMenu).alignWithComponent(nameLabel, 0, 100, 0, 0, 4, true));
		add(accountsMenuTrigger);
		
		add(new Link<Void>("runAs") {

			@Override
			public void onClick() {
				SecurityUtils.getSubject().runAs(getAccount().getPrincipals());
				setResponsePage(getPage().getClass(), getPageParameters());
			}
			
			@Override
			protected void onConfigure() {
				super.onConfigure();
				
				UserManager userManager = GitPlex.getInstance(UserManager.class);
				User currentUser = userManager.getCurrent();
				setVisible(!getAccount().equals(currentUser) && SecurityUtils.canManage(getAccount()));
			}
			
		});
		
		List<PageTab> tabs = new ArrayList<>();
		tabs.add(new AccountTab("Repositories", "fa fa-ext fa-fw fa-repo", AccountReposPage.class));
		
		if (SecurityUtils.canManage(getAccount())) {
			tabs.add(new AccountTab("Notifications", "fa fa-fw fa-bell-o", AccountNotificationsPage.class));
			tabs.add(new AccountTab("Setting", "fa fa-fw fa-cog", ProfileEditPage.class, 
					AvatarEditPage.class, PasswordEditPage.class));
		}
		add(new Tabbable("tabs", tabs));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(AccountPage.class, "account.css")));
	}

	@Override
	protected void onDetach() {
		accountModel.detach();
		
		super.onDetach();
	}
	
	public User getAccount() {
		return accountModel.getObject();
	}
	
	public static PageParameters paramsOf(User user) {
		PageParameters params = new PageParameters();
		params.set(PARAM_USER, user.getName());
		return params;
	}

	public static PageParameters paramsOf(String userName) {
		PageParameters params = new PageParameters();
		params.set(PARAM_USER, userName);
		return params;
	}
	
}
