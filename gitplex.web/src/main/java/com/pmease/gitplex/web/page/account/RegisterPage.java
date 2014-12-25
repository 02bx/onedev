package com.pmease.gitplex.web.page.account;

import java.io.Serializable;

import com.pmease.gitplex.core.GitPlex;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;

import com.pmease.commons.wicket.component.feedback.FeedbackPanel;
import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.commons.wicket.editable.BeanEditor;
import com.pmease.commons.wicket.editable.ErrorContext;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.commons.wicket.editable.ValuePath;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.web.page.LayoutPage;

@SuppressWarnings("serial")
public class RegisterPage extends LayoutPage {

	private BeanEditor<Serializable> beanEditor;
	
	private User user = new User();
	
	@Override
	protected String getPageTitle() {
		return "GitPlex - Sign Up";
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		final Form<?> form = new Form<Void>("form") {

			@Override
			protected void onValidate() {
				super.onValidate();

				if (beanEditor.isValid()) {
					User user = (User) beanEditor.getConvertedInput();
					
					ErrorContext nameContext = beanEditor.getErrorContext(new ValuePath(new PathSegment.Property("name")));
					ErrorContext emailContext = beanEditor.getErrorContext(new ValuePath(new PathSegment.Property("email")));
					
					UserManager userManager = GitPlex.getInstance(UserManager.class);
	
					if (!nameContext.hasError(true) && userManager.findByName(user.getName()) != null) 
						nameContext.addError("This name is already used by another user.");
					if (!emailContext.hasError(true) && userManager.findByEmail(user.getEmail()) != null) 
						emailContext.addError("This email address is already used by another user.");
				}
			}
			
		};
		add(form);
		
		add(new FeedbackPanel("feedback", form));
		
		form.add(beanEditor = BeanContext.editBean("editor", user));

		form.add(new SubmitLink("submit", form) {

			@Override
			public void onSubmit() {
				super.onSubmit();

				UserManager userManager = GitPlex.getInstance(UserManager.class);
				userManager.save(user);
				success("Account has been registered successfully.");
					
				// clear the form fields
				user = new User();
				form.replace(beanEditor = BeanContext.editBean("editor", user));
			}
			
		});
	}

}
