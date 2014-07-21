package com.pmease.gitop.web.page.account.home;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pmease.gitop.model.Membership;
import com.pmease.gitop.model.Repository;
import com.pmease.gitop.model.Team;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.permission.ObjectPermission;
import com.pmease.gitop.web.component.user.AvatarByUser;
import com.pmease.gitop.web.model.UserModel;
import com.pmease.gitop.web.page.account.AccountPage;
import com.pmease.gitop.web.page.account.setting.members.MemberListView;

@SuppressWarnings("serial")
public class AccountHomePage extends AccountPage {

	public static enum Category {
		REPOSITORIES("Repositories"), 
		MEMBERS("Members");
		
		final String displayName;
		Category(String displayName) {
			this.displayName = displayName;
		}
	}
	
	private Category category = Category.REPOSITORIES;
	
	public AccountHomePage(PageParameters params) {
		super(params);
		
		String tab = params.get("tab").toString();
		if (!Strings.isNullOrEmpty(tab)) {
			category = Category.valueOf(tab.toUpperCase());
		}
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new AvatarByUser("avatar", accountModel));
		add(new Label("fullname", new PropertyModel<String>(accountModel, "fullName")));
		add(new Label("username", new PropertyModel<String>(accountModel, "name")));
		
		add(new ListView<Category>("category", Lists.newArrayList(Category.values())) {

			@Override
			protected void populateItem(ListItem<Category> item) {
				Category current = item.getModelObject();
				PageParameters params = paramsOf(accountModel.getObject());
				if (current != Category.REPOSITORIES) {
					params.add("tab", current.name().toLowerCase());
				}
				
				AbstractLink link = new BookmarkablePageLink<Void>("link", 
						AccountHomePage.class, params);
				link.add(new Label("name", current.displayName));
				item.add(link);
				item.add(AttributeAppender.append("class", category == current ? "active" : ""));
			}
		});
		
		add(createContent("content"));
	}
	
	private Component createContent(String id) {
		switch (category) {
		case REPOSITORIES:
			IModel<List<Repository>> repos = new LoadableDetachableModel<List<Repository>>() {

				@Override
				protected List<Repository> load() {
					User account = accountModel.getObject();
					List<Repository> repositories = Lists.newArrayList();
					for (Repository each : account.getRepositories()) {
						if (SecurityUtils.getSubject().isPermitted(ObjectPermission.ofRepositoryRead(each))) {
							repositories.add(each);
						}
					}
					
					return repositories;
				}
				
			};
			
			return new RepositoryListPanel(id, repos);
			
		case MEMBERS:
			IModel<List<User>> model = new LoadableDetachableModel<List<User>>() {

				@Override
				protected List<User> load() {
					User account = getAccount();
					Collection<Team> teams = account.getTeams();
					Set<User> users = Sets.newHashSet();
					
					for (Team each : teams) {
						for (Membership membership : each.getMemberships()) {
							users.add(membership.getUser());
						}
					}
					
					List<User> result = Lists.newArrayList(users);
					Collections.sort(result);
					return result;
				}
				
			};
			return new MemberListView(id, new UserModel(getAccount()), model) {
				@Override
				protected Component createMemberTeams(String id, final IModel<User> user) {
					return new WebMarkupContainer(id).setVisibilityAllowed(false);
				}
			};
			
		default:
			throw new IllegalArgumentException("tab " + category);
		}
		
	}
	
	@Override
	protected boolean isPermitted() {
		return SecurityUtils.getSubject().isPermitted(ObjectPermission.ofUserRead(getAccount()));
	}
	
	@Override
	protected String getPageTitle() {
		if (getAccount().getFullName() != null) 
			return getAccount().getName() + " (" + getAccount().getFullName() + ")";
		else
			return getAccount().getName();
	}
	
}
