package io.onedev.server.web.page.admin.user.buildsetting;

import java.io.Serializable;

import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.OneDev;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.web.editable.PropertyContext;

@SuppressWarnings("serial")
public class UserBuildPreserveRulesPage extends UserBuildSettingPage {

	public UserBuildPreserveRulesPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				OneDev.getInstance(UserManager.class).save(getUser());
				getSession().success("Build preserve rules have been saved");
				setResponsePage(UserBuildPreserveRulesPage.class, UserBuildPreserveRulesPage.paramsOf(getUser()));
			}
			
		};
		
		form.add(new FencedFeedbackPanel("feedback", form));
		
		form.add(PropertyContext.editModel("editor", new AbstractReadOnlyModel<Serializable>() {

			@Override
			public Serializable getObject() {
				return getUser().getBuildSetting();
			}
			
		}, "buildPreservations"));
		
		add(form);
	}

}
