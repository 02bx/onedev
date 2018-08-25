package io.onedev.server.web.page.user;

import org.apache.shiro.codec.Base64;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import io.onedev.server.security.SecurityUtils;
import io.onedev.server.web.behavior.clipboard.CopyClipboardBehavior;
import io.onedev.server.web.editable.BeanContext;

@SuppressWarnings("serial")
public class TokenGeneratePage extends UserPage {
	
	private String token;
	
	public TokenGeneratePage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		TokenGenerateBean bean = new TokenGenerateBean();
		
		Form<?> form = new Form<Void>("form") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				
				token = new String(Base64.encode((getUser().getName() + ":" + bean.getPassword()).getBytes()));
				
				bean.setPassword(null);
				replace(BeanContext.editBean("editor", bean));
			}

		};
		add(form);
		
		form.add(BeanContext.editBean("editor", bean));
		
		add(new Label("token", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return token;
			}
			
		}) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(token != null);
			}
			
		});
		add(new WebMarkupContainer("copy").add(new CopyClipboardBehavior(new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return token;
			}
			
		})));
	}
	
	@Override
	protected boolean isPermitted() {
		return SecurityUtils.canManage(getUser());
	}
	
}
