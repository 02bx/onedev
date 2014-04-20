package com.pmease.gitop.web.component.link;

import javax.annotation.Nullable;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.pmease.commons.wicket.behavior.TooltipBehavior;
import com.pmease.gitop.model.User;
import com.pmease.gitop.web.component.avatar.AvatarImage;
import com.pmease.gitop.web.page.PageSpec;
import com.pmease.gitop.web.page.account.home.AccountHomePage;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;

/**
 * Displays git person, name and avatar
 *
 */
@SuppressWarnings("serial")
public class UserLink extends Panel {

	public static enum Mode {NAME, AVATAR, NAME_AND_AVATAR}
	
	private final Mode mode;
	
	private User user;
	
	private TooltipConfig tooltipConfig;
	
	public UserLink(String id, User user, Mode mode) {
		super(id);

		this.user = user;
		this.mode = mode;
	}
	
	public UserLink(String id, User user) {
		this(id, user, Mode.NAME_AND_AVATAR);
	}

	public UserLink withTooltipConfig(@Nullable TooltipConfig tooltipConfig) {
		this.tooltipConfig = tooltipConfig;
		return this;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		WebMarkupContainer link = new BookmarkablePageLink<Void>("link", AccountHomePage.class, PageSpec.forUser(user));
		add(link);
		
		if (mode == Mode.NAME_AND_AVATAR || mode == Mode.AVATAR) {
			AvatarImage avatar = new AvatarImage("avatar", user);
			if (tooltipConfig != null)
				avatar.add(new TooltipBehavior(Model.of(user.getDisplayName()), tooltipConfig));
			link.add(avatar);
		} else {
			link.add(new WebMarkupContainer("avatar").setVisible(false));
		}
		
		if (mode == Mode.NAME_AND_AVATAR || mode == Mode.NAME) {
			link.add(new Label("name", user.getDisplayName()));
		} else {
			link.add(new Label("name").setVisible(false));
		}
		
	}

	@Override
	protected void onDetach() {
		user = null;
		super.onDetach();
	}

}
