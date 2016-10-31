package com.gitplex.web.component.accountchoice;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;

import com.gitplex.core.entity.Account;
import com.gitplex.commons.wicket.component.select2.Select2Choice;

@SuppressWarnings("serial")
public class AccountSingleChoice extends Select2Choice<Account> {

	public AccountSingleChoice(String id, IModel<Account> model, 
			AbstractAccountChoiceProvider choiceProvider) {
		super(id, model, choiceProvider);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		getSettings().setPlaceholder("Choose an account ...");
		getSettings().setFormatResult("gitplex.accountChoiceFormatter.formatResult");
		getSettings().setFormatSelection("gitplex.accountChoiceFormatter.formatSelection");
		getSettings().setEscapeMarkup("gitplex.accountChoiceFormatter.escapeMarkup");
	}

	@Override
	protected void onBeforeRender() {
		getSettings().setAllowClear(!isRequired());
		super.onBeforeRender();
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(new AccountChoiceResourceReference()));
	}
	
}
