package com.pmease.gitplex.web.page.security;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.wicket.behavior.testform.TestFormBehavior;
import com.pmease.commons.wicket.behavior.testform.TestResult;
import com.pmease.commons.wicket.component.feedback.FeedbackPanel;
import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.ConfigManager;
import com.pmease.gitplex.core.manager.MailManager;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.web.page.base.BasePage;

@SuppressWarnings("serial")
public class ForgetPage extends BasePage {

	private static final Logger logger = LoggerFactory.getLogger(ForgetPage.class);
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		final HelperBean bean = new HelperBean();
		Form<?> form = new Form<Void>("form");
		form.add(new FeedbackPanel("feedback", form));		
		form.add(BeanContext.editBean("editor", bean));
		
		form.add(new AjaxSubmitLink("reset") {
			private TestFormBehavior testBehavior;
			
			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				add(testBehavior = new TestFormBehavior() {

					@Override
					protected TestResult test() {
						UserManager userManager = GitPlex.getInstance(UserManager.class);
						User user = userManager.findByEmail(bean.getEmail());
						if (user != null) {
							ConfigManager configManager = GitPlex.getInstance(ConfigManager.class);
							if (configManager.getMailSetting() != null) {
								String password = RandomStringUtils.random(10, true, true);
								user.setPassword(password);
								
								MailManager mailManager = GitPlex.getInstance(MailManager.class);
								try {
									String mailBody = String.format("Dear %s, "
										+ "<p style='margin: 16px 0;'>"
										+ "Per your request, password of your account \"%s\" has been reset to:<br>"
										+ "%s<br><br>"
										+ "Please login and change the password in your earliest convenience."
										+ "<p style='margin: 16px 0;'>"
										+ "-- Sent by GitPlex", 
										user.getDisplayName(), user.getName(), password);

									mailManager.sendMailNow(configManager.getMailSetting(), Arrays.asList(user), 
											"Your GitPlex password has been reset", mailBody);
									return new TestResult.Successful("Please check your email for the reset password.");
								} catch (Exception e) {
									logger.error("Error sending password reset email", e);
									return new TestResult.Failed("Error sending password reset email: " + e.getMessage());
								}
							} else {
								return new TestResult.Failed("Unable to send password reset email as smtp setting is not defined");
							}
						} else {
							return new TestResult.Failed("No account found with email " + bean.getEmail());
						}
					}
					
				});
			}

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				
				target.add(form);
				target.focusComponent(null);
				testBehavior.requestTest(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(form);
			}
			
		});
		
		add(form);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(CssHeaderItem.forReference(new CssResourceReference(ForgetPage.class, "forget.css")));
	}

	@Editable
	public static class HelperBean implements Serializable {
		
		private String email;

		@Editable(name="Please specify email of your account")
		@NotEmpty
		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
		
	}
}
