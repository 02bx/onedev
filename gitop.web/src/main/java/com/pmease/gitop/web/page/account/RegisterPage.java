package com.pmease.gitop.web.page.account;

import javax.validation.constraints.Size;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.bean.validation.Property;
import org.apache.wicket.bean.validation.PropertyValidator;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Objects;
import com.pmease.commons.loader.AppLoader;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.UserManager;
import com.pmease.gitop.model.User;
import com.pmease.gitop.web.GitopSession;
import com.pmease.gitop.web.common.wicket.form.BaseForm;
import com.pmease.gitop.web.common.wicket.form.passwordfield.PasswordFieldElement;
import com.pmease.gitop.web.common.wicket.form.textfield.TextFieldElement;
import com.pmease.gitop.web.page.BasePage;
import com.pmease.gitop.web.page.account.home.AccountHomePage;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public class RegisterPage extends BasePage {

	@NotEmpty
	@Size(min = 6, max = 32, message = "The password should be 6-32 characters.")
	private String password;

	@NotEmpty
	private String confirmPassword;

	@Override
	protected String getPageTitle() {
		return "Gitop - Sign Up";
	}
	
	@Override
	protected void onPageInitialize() {
		super.onPageInitialize();
		
		User user = new User();
		final IModel<User> model = Model.<User>of(user);
		Form<User> form = new BaseForm<User>("form", model);
		add(form);

		form.add(new NotificationPanel("feedback", new ComponentFeedbackMessageFilter(form)));
		form.add(new TextFieldElement<String>(
							"username", "User Name", 
							new PropertyModel<String>(user, "name"))
				.add(new PropertyValidator<String>(new Property(User.class, "name")))
				);
		
		form.add(new TextFieldElement<String>(
							"email", "Email Address",
							new PropertyModel<String>(model, "email"))
				.add(new PropertyValidator<String>(new Property(User.class, "email")))
				.add(new IValidator<String>() {

					@Override
					public void validate(IValidatable<String> validatable) {
						String mail = validatable.getValue();
						UserManager um = Gitop.getInstance(UserManager.class);
						User u = um.findByEmail(mail);
						if (u != null) {
							validatable.error(new ValidationError("This email is already used."));
						}
					}
					
				}));
		
		form.add(new TextFieldElement<String>(
							"displayname", "Display Name",
							new PropertyModel<String>(model, "displayName"))
				.setRequired(false)
				.add(new PropertyValidator<String>(new Property(User.class, "displayName"))));
		
		PasswordFieldElement passField = new PasswordFieldElement("password", "Password",
			new PropertyModel<String>(this, "password"))
			.add(new PropertyValidator<String>())
			.add(new IValidator<String>() {

				@Override
				public void validate(IValidatable<String> validatable) {
					if (!Objects.equal(password, confirmPassword)) {
						validatable.error(new ValidationError().setMessage("Password and confirmed should be identical"));
					}
				}
			})
			.setHelp("Use at least six characters.");
		form.add(passField);
		PasswordFieldElement confirmField = new PasswordFieldElement("confirmPassword",
				"Confirm Password", 
				new PropertyModel<String>(this, "confirmPassword"))
		.add(new PropertyValidator<String>());
		form.add(confirmField);
		form.add(new EqualPasswordInputValidator(passField.getFormComponent(), confirmField.getFormComponent()));
		
		form.add(new AjaxFallbackButton("submit", form) {
			@Override
			protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
				User user = model.getObject();
				user.setPassword(password);
				UserManager um = AppLoader.getInstance(UserManager.class);
				um.save(user);
				GitopSession.get().login(user.getName(), password, false);
				
				setResponsePage(AccountHomePage.class, AccountHomePage.newParams(user));
			}
			
			@Override
			protected void onError(final AjaxRequestTarget target, final Form<?> form) {
				target.add(form);
			}
		});
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}
}
