package com.pmease.gitplex.web.page.account.setting;

import java.io.Serializable;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import com.pmease.commons.editable.annotation.Editable;
import com.pmease.commons.editable.annotation.Password;
import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.User;

@SuppressWarnings("serial")
public class PasswordEditPage extends AccountSettingPage {
	
	public PasswordEditPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		User account = getAccount();
		
		sidebar.add(new Label("title", "Change Password of " + account.getDisplayName()));
		
		final PasswordBean bean = new PasswordBean();
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();

				User account = getAccount();
				account.setPassword(bean.getPassword());
				GitPlex.getInstance(UserManager.class).save(account);
				Session.get().info("Password has been updated");
				backToPrevPage();
			}
			
		};
		sidebar.add(form);
		
		form.add(BeanContext.editBean("editor", bean));
		
		form.add(new Link<Void>("cancel") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(prevPageRef != null);
			}

			@Override
			public void onClick() {
				backToPrevPage();
			}
			
		});
	}
	
	@Editable
	public static class PasswordBean implements Serializable {

		private static final long serialVersionUID = 1L;

		private String password;
		
		@Editable
		@Password(confirmative=true)
		@NotEmpty
		@Length(min=5)
		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
		
	}

}
