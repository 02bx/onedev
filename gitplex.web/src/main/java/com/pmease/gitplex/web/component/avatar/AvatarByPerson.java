package com.pmease.gitplex.web.component.avatar;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.eclipse.jgit.lib.PersonIdent;

import com.pmease.commons.wicket.behavior.TooltipBehavior;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.web.avatar.AvatarManager;

@SuppressWarnings("serial")
public class AvatarByPerson extends WebComponent {

	private final IModel<String> avatarUrlModel;
	
	private final boolean withTooltip;

	public AvatarByPerson(String id, IModel<PersonIdent> personModel) {
		this(id, personModel, true);
	}
	
	/**
	 * Display avatar of specified email model.
	 * 
	 * @param id
	 * 			component id
	 * @param userModel
	 * 			model of the user to display avatar for. This model allows to return <tt>null</tt> 
	 * 			to display avatar for unknown user 
	 */
	public AvatarByPerson(String id, IModel<PersonIdent> personModel, boolean withTooltip) {
		super(id, personModel);
		
		avatarUrlModel = new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				AvatarManager avatarManager = GitPlex.getInstance(AvatarManager.class);
				
				PersonIdent person = getPerson();
				User user = GitPlex.getInstance(UserManager.class).findByPerson(person);
				if (user != null) 
					return avatarManager.getAvatarUrl(user);
				else
					return avatarManager.getAvatarUrl(person);
			}
			
		};
		
		this.withTooltip = withTooltip;
	}

	private PersonIdent getPerson() {
		return (PersonIdent) getDefaultModelObject();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (withTooltip)
			add(new TooltipBehavior(Model.of(getPerson().getName())));
	}

	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
		
		tag.setName("img");
		tag.put("src", avatarUrlModel.getObject());
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(CssHeaderItem.forReference(AvatarResourceReference.INSTANCE));
	}

	@Override
	protected void onDetach() {
		avatarUrlModel.detach();
		
		super.onDetach();
	}

}
