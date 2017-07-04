package com.gitplex.server.web.page.test;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.wicket.markup.html.link.Link;

import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.web.page.base.BasePage;

@SuppressWarnings("serial")
public class TestPage extends BasePage {

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new Link<Void>("test") {

			@Override
			public void onClick() {
				System.out.println(SecurityUtils.getSecurityManager().authenticate(new UsernamePasswordToken("robinshine", "sysvalue8")));
			}
			
		});
		
	}

}
