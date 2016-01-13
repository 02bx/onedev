package com.pmease.gitplex.web.component.avatar;

import javax.annotation.Nullable;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.Model;
import org.eclipse.jgit.lib.PersonIdent;

import com.pmease.commons.wicket.behavior.TooltipBehavior;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.web.avatar.AvatarManager;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;

@SuppressWarnings("serial")
public class Avatar extends WebComponent {

	private final String url;
	
	private final String name;
	
	private final TooltipConfig tooltipConfig;
	
	public Avatar(String id, User user, @Nullable TooltipConfig tooltipConfig) {
		super(id);

		AvatarManager avatarManager = GitPlex.getInstance(AvatarManager.class);
		url = avatarManager.getAvatarUrl(user);
		name = user.getDisplayName();
		this.tooltipConfig = tooltipConfig;
	}
	
	public Avatar(String id, PersonIdent person, @Nullable TooltipConfig tooltipConfig) {
		super(id);
		
		AvatarManager avatarManager = GitPlex.getInstance(AvatarManager.class);
		
		User user = GitPlex.getInstance(UserManager.class).findByPerson(person);
		if (user != null) { 
			url = avatarManager.getAvatarUrl(user);
			name = user.getDisplayName();
		} else {
			url = avatarManager.getAvatarUrl(person);
			name = person.getName();
		}
		this.tooltipConfig = tooltipConfig;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (tooltipConfig != null)
			add(new TooltipBehavior(Model.of(name), tooltipConfig));
	}

	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
		
		tag.setName("img");
		tag.append("class", "avatar", " ");
		tag.put("src", url);
	}

}
