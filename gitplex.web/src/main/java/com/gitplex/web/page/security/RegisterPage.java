package com.gitplex.web.page.security;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;

import com.gitplex.core.GitPlex;
import com.gitplex.core.entity.Account;
import com.gitplex.core.manager.AccountManager;
import com.gitplex.core.manager.ConfigManager;
import com.gitplex.core.security.SecurityUtils;
import com.gitplex.web.page.account.AccountPage;
import com.gitplex.web.page.account.setting.AvatarEditPage;
import com.gitplex.web.page.base.BasePage;
import com.gitplex.web.page.home.DashboardPage;
import com.gitplex.commons.wicket.editable.BeanContext;
import com.gitplex.commons.wicket.editable.BeanEditor;
import com.gitplex.commons.wicket.editable.PathSegment;

@SuppressWarnings("serial")
public class RegisterPage extends BasePage {
	
	public RegisterPage() {
		if (!GitPlex.getInstance(ConfigManager.class).getSecuritySetting().isEnableSelfRegister())
			throw new UnauthenticatedException("Account self-register is disabled");
		if (getLoginUser() != null)
			throw new IllegalStateException("Can not sign up an account while signed in");
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
	
		final Account account = new Account();
		final BeanEditor<?> editor = BeanContext.editBean("editor", account, Account.getUserExcludeProperties());
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				AccountManager accountManager = GitPlex.getInstance(AccountManager.class);
				Account accountWithSameName = accountManager.findByName(account.getName());
				if (accountWithSameName != null) {
					editor.getErrorContext(new PathSegment.Property("name"))
							.addError("This name has already been used by another account.");
				} 
				Account accountWithSameEmail = accountManager.findByEmail(account.getEmail());
				if (accountWithSameEmail != null) {
					editor.getErrorContext(new PathSegment.Property("email"))
							.addError("This email has already been used by another account.");
				} 
				if (!editor.hasErrors(true)) {
					accountManager.save(account, null);
					Session.get().success("New account registered");
					SecurityUtils.getSubject().runAs(account.getPrincipals());
					setResponsePage(AvatarEditPage.class, AccountPage.paramsOf(account));
				}
			}
			
		};
		form.add(editor);
		
		form.add(new SubmitLink("save"));
		form.add(new Link<Void>("cancel") {

			@Override
			public void onClick() {
				setResponsePage(DashboardPage.class);
			}
			
		});
		add(form);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new RegisterResourceReference()));
	}

}
