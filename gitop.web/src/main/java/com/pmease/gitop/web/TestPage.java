package com.pmease.gitop.web;

import org.apache.wicket.markup.html.form.Form;

import com.pmease.commons.editable.EditContext;
import com.pmease.commons.wicket.editable.EditHelper;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.ProjectManager;
import com.pmease.gitop.core.manager.UserManager;
import com.pmease.gitop.core.model.Project;
import com.pmease.gitop.web.page.BasePage;

@SuppressWarnings("serial")
public class TestPage extends BasePage {

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		final EditContext editContext = EditHelper.getContext(new Project());
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				editContext.validate();
				if (!editContext.hasValidationError(true)) {
					Project project = (Project) editContext.getBean();
					project.setOwner(Gitop.getInstance(UserManager.class).getRootUser());
					Gitop.getInstance(ProjectManager.class).save(project);
				}
			}
			
		};
		
		form.add(EditHelper.renderForEdit(editContext, "editor"));
		
		add(form);
	}

	@Override
	protected String getPageTitle() {
		return "Test page used by Robin";
	}

}
