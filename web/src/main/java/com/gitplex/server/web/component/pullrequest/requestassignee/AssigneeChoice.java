package com.gitplex.server.web.component.pullrequest.requestassignee;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;

import com.gitplex.server.model.Account;
import com.gitplex.server.model.Depot;
import com.gitplex.server.web.component.accountchoice.AccountChoiceResourceReference;
import com.gitplex.server.web.component.select2.Select2Choice;

@SuppressWarnings("serial")
public class AssigneeChoice extends Select2Choice<Assignee> {

	public AssigneeChoice(String id, IModel<Depot> depotModel, final IModel<Account> assigneeModel) {
		super(id, new IModel<Assignee>() {

			@Override
			public void detach() {
			}

			@Override
			public Assignee getObject() {
				Account user = assigneeModel.getObject();
				if (user != null)
					return new Assignee(user, null);
				else
					return null;
			}

			@Override
			public void setObject(Assignee object) {
				if (object != null)
					assigneeModel.setObject(object.getUser());
				else
					assigneeModel.setObject(null);
			}
			
		}, new AssigneeProvider(depotModel));
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		// getSettings().setMinimumInputLength(1);
		getSettings().setPlaceholder("Type to find an assignee");
		getSettings().setFormatResult("gitplex.server.accountChoiceFormatter.formatResult");
		getSettings().setFormatSelection("gitplex.server.accountChoiceFormatter.formatSelection");
		getSettings().setEscapeMarkup("gitplex.server.accountChoiceFormatter.escapeMarkup");
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(new AccountChoiceResourceReference()));
	}

}
