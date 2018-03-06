package io.onedev.server.web.component.avatar;

import javax.annotation.Nullable;

import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.jgit.lib.PersonIdent;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import io.onedev.server.OneDev;
import io.onedev.server.manager.UserManager;
import io.onedev.server.model.User;
import io.onedev.server.web.behavior.TooltipBehavior;
import io.onedev.server.web.component.link.ViewStateAwarePageLink;
import io.onedev.server.web.page.user.UserPage;
import io.onedev.server.web.page.user.UserProfilePage;
import io.onedev.server.web.util.avatar.AvatarManager;

@SuppressWarnings("serial")
public class AvatarLink extends ViewStateAwarePageLink<Void> {

	private final Long userId;
	
	private final PageParameters params;
	
	private final String tooltip;
	
	private String url;
	
	public AvatarLink(String id, @Nullable User user) {
		this(id, user, false);
	}
	
	public AvatarLink(String id, @Nullable User user, boolean showTooltip) {
		super(id, UserProfilePage.class);

		AvatarManager avatarManager = OneDev.getInstance(AvatarManager.class);
		if (user == null) {
			userId = null;
			params = new PageParameters();
		} else if (user.getId() == null) {
			userId = null;
			params = new PageParameters();
		} else {
			userId = user.getId();
			params = UserPage.paramsOf(user);
		}
		if (showTooltip) {
			if (user != null)
				tooltip = user.getDisplayName();
			else
				tooltip = OneDev.NAME;
		} else {
			tooltip = null;
		}
		url = avatarManager.getAvatarUrl(user!=null?user.getFacade():null);
	}
	
	public AvatarLink(String id, PersonIdent person) {
		this(id, person, false);
	}
	
	public AvatarLink(String id, PersonIdent person, boolean showTooltip) {
		super(id, UserProfilePage.class);
		
		AvatarManager avatarManager = OneDev.getInstance(AvatarManager.class);
		
		User user = OneDev.getInstance(UserManager.class).find(person);
		if (user != null) { 
			userId = user.getId();
			params = UserPage.paramsOf(user);
			url = avatarManager.getAvatarUrl(user.getFacade());
		} else {
			userId = null;
			params = new PageParameters();
			url = avatarManager.getAvatarUrl(person);
		}
		
		if (showTooltip)
			tooltip = person.getName();
		else
			tooltip = null;
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
			if (avatarChanged.getUser().getId().equals(userId)) {
				AvatarManager avatarManager = OneDev.getInstance(AvatarManager.class);
				url = avatarManager.getAvatarUrl(avatarChanged.getUser().getFacade());
				avatarChanged.getHandler().add(this);
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
		
		if (tooltip != null)
			add(new TooltipBehavior(Model.of(tooltip), new TooltipConfig()));
		
		setOutputMarkupId(true);
	}

}
