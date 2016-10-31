package com.gitplex.web.component.avatar;

import javax.annotation.Nullable;

import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.lib.PersonIdent;

import com.gitplex.core.GitPlex;
import com.gitplex.core.entity.Account;
import com.gitplex.core.manager.AccountManager;
import com.gitplex.web.avatar.AvatarManager;
import com.gitplex.web.page.account.AccountPage;
import com.gitplex.web.page.account.overview.AccountOverviewPage;
import com.gitplex.commons.wicket.behavior.TooltipBehavior;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;

@SuppressWarnings("serial")
public class AvatarLink extends BookmarkablePageLink<Void> {

	private final Long accountId;
	
	private final PageParameters params;
	
	private String url;
	
	private final String name;
	
	private final TooltipConfig tooltipConfig;
	
	public AvatarLink(String id, Account user) {
		this(id, user, null);
	}
	
	public AvatarLink(String id, Account account, @Nullable TooltipConfig tooltipConfig) {
		super(id, AccountOverviewPage.class);

		AvatarManager avatarManager = GitPlex.getInstance(AvatarManager.class);
		accountId = account.getId();
		params = AccountPage.paramsOf(account);
		name = account.getDisplayName();
		url = avatarManager.getAvatarUrl(account);
		this.tooltipConfig = tooltipConfig;
	}
	
	public AvatarLink(String id, PersonIdent person) {
		this(id, person, null);
	}
	
	public AvatarLink(String id, PersonIdent person, @Nullable TooltipConfig tooltipConfig) {
		super(id, AccountOverviewPage.class);
		
		AvatarManager avatarManager = GitPlex.getInstance(AvatarManager.class);
		
		Account account = GitPlex.getInstance(AccountManager.class).find(person);
		if (account != null) { 
			accountId = account.getId();
			params = AccountPage.paramsOf(account);
			url = avatarManager.getAvatarUrl(account);
			name = account.getDisplayName();
		} else {
			accountId = null;
			params = new PageParameters();
			url = avatarManager.getAvatarUrl(person);
			name = person.getName();
		}
		this.tooltipConfig = tooltipConfig;
	}
	
	@Override
	public PageParameters getPageParameters() {
		return params;
	}

	@Override
	public IModel<?> getBody() {
		return Model.of("<img src='" + url + "' class='avatar'></img>");
	}
	
	@Override
	public void onEvent(IEvent<?> event) {
		super.onEvent(event);
		
		if (event.getPayload() instanceof AvatarChanged) {
			AvatarChanged avatarChanged = (AvatarChanged) event.getPayload();
			if (avatarChanged.getUser().getId().equals(accountId)) {
				AvatarManager avatarManager = GitPlex.getInstance(AvatarManager.class);
				url = avatarManager.getAvatarUrl(avatarChanged.getUser());
				avatarChanged.getPartialPageRequestHandler().add(this);
			}
		}
	}
	
	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
		configure();
		if (!isEnabled())
			tag.setName("span");
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new AvatarResourceReference()));
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		setEnabled(!params.isEmpty());
		setEscapeModelStrings(false);
		
		if (tooltipConfig != null)
			add(new TooltipBehavior(Model.of(name), tooltipConfig));
		
		setOutputMarkupId(true);
	}

}
