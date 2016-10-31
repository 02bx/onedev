package com.gitplex.web.page.account.teams;

import java.util.Collection;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;

import com.gitplex.core.entity.Account;
import com.gitplex.web.component.accountchoice.AccountChoiceResourceReference;
import com.gitplex.commons.wicket.component.select2.Select2MultiChoice;

public class MemberChoices extends Select2MultiChoice<Account> {

	private static final long serialVersionUID = 1L;

	public MemberChoices(String id, IModel<Account> organizationModel, IModel<Collection<Account>> membersModel) {
		super(id, membersModel, new MemberChoiceProvider(organizationModel));
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		getSettings().setPlaceholder("Choose team members...");
		getSettings().setFormatResult("gitplex.accountChoiceFormatter.formatResult");
		getSettings().setFormatSelection("gitplex.accountChoiceFormatter.formatSelection");
		getSettings().setEscapeMarkup("gitplex.accountChoiceFormatter.escapeMarkup");
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(new AccountChoiceResourceReference()));
	}

}
