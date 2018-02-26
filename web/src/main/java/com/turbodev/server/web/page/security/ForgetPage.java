package com.turbodev.server.web.page.security;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turbodev.launcher.loader.AppLoader;
import com.turbodev.server.TurboDev;
import com.turbodev.server.manager.ConfigManager;
import com.turbodev.server.manager.MailManager;
import com.turbodev.server.manager.UserManager;
import com.turbodev.server.model.User;
import com.turbodev.server.util.editable.annotation.Editable;
import com.turbodev.server.web.behavior.testform.TestFormBehavior;
import com.turbodev.server.web.behavior.testform.TestResult;
import com.turbodev.server.web.editable.BeanContext;
import com.turbodev.server.web.page.base.BasePage;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public class ForgetPage extends BasePage {

	private static final Logger logger = LoggerFactory.getLogger(ForgetPage.class);
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		final HelperBean bean = new HelperBean();
		Form<?> form = new Form<Void>("form");
		form.add(new NotificationPanel("feedback", form));		
		form.add(BeanContext.editBean("editor", bean));
		
		form.add(new AjaxButton("reset") {
			private TestFormBehavior testBehavior;
			
			@Override
			protected void onInitialize() {
				super.onInitialize();
				
				add(testBehavior = new TestFormBehavior() {

					@Override
					protected TestResult test() {
						UserManager userManager = TurboDev.getInstance(UserManager.class);
						User user = userManager.findByName(bean.getUserNameOrEmailAddress());
						if (user == null) {
							user = userManager.findByEmail(bean.getUserNameOrEmailAddress());
						}
						if (user == null) {
							return new TestResult.Failed("No user found with name or email: " + bean.getUserNameOrEmailAddress());
						} else {
							ConfigManager configManager = TurboDev.getInstance(ConfigManager.class);
							if (configManager.getMailSetting() != null) {
								String password = RandomStringUtils.random(10, true, true);								
								user.setPassword(AppLoader.getInstance(PasswordService.class).encryptPassword(password));
								userManager.save(user);
								
								MailManager mailManager = TurboDev.getInstance(MailManager.class);
								try {
									String mailBody = String.format("Dear %s, "
										+ "<p style='margin: 16px 0;'>"
										+ "Per your request, password of your user \"%s\" has been reset to:<br>"
										+ "%s<br><br>"
										+ "Please login and change the password in your earliest convenience."
										+ "<p style='margin: 16px 0;'>"
										+ "-- Sent by TurboDev", 
										user.getDisplayName(), user.getName(), password);

									mailManager.sendMail(configManager.getMailSetting(), Arrays.asList(user.getEmail()), 
											"Your TurboDev password has been reset", mailBody);
									return new TestResult.Successful("Please check your email " + user.getEmail() + " for the reset password.");
								} catch (Exception e) {
									logger.error("Error sending password reset email", e);
									return new TestResult.Failed("Error sending password reset email: " + e.getMessage());
								}
							} else {
								return new TestResult.Failed("Unable to send password reset email as smtp setting is not defined");
							}
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

	@Editable
	public static class HelperBean implements Serializable {
		
		private String userNameOrEmailAddress;

		@Editable(name="Please specify your user name or email address")
		@NotEmpty
		public String getUserNameOrEmailAddress() {
			return userNameOrEmailAddress;
		}

		public void setUserNameOrEmailAddress(String userNameOrEmailAddress) {
			this.userNameOrEmailAddress = userNameOrEmailAddress;
		}
		
	}
}
