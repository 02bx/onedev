package com.pmease.gitplex.web.editable.accountchoice;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import com.pmease.commons.wicket.editable.ErrorContext;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.commons.wicket.editable.PropertyDescriptor;
import com.pmease.commons.wicket.editable.PropertyEditor;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.manager.AccountManager;
import com.pmease.gitplex.web.component.accountchoice.AccountChoiceProvider;
import com.pmease.gitplex.web.component.accountchoice.AccountSingleChoice;

@SuppressWarnings("serial")
public class AccountSingleChoiceEditor extends PropertyEditor<String> {

	private AccountSingleChoice input;
	
	private boolean organization;
	
	public AccountSingleChoiceEditor(String id, PropertyDescriptor propertyDescriptor, 
			IModel<String> propertyModel, boolean organization) {
		super(id, propertyDescriptor, propertyModel);
		this.organization = organization;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		Account account;
		if (getModelObject() != null)
			account = GitPlex.getInstance(AccountManager.class).findByName(getModelObject());
		else
			account = null;
		
    	input = new AccountSingleChoice("input", Model.of(account), new AccountChoiceProvider(organization));
        input.setConvertEmptyInputStringToNull(true);
        
        // add this to control allowClear flag of select2
    	input.setRequired(propertyDescriptor.isPropertyRequired());
        
        add(input);
	}

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		return null;
	}

	@Override
	protected String convertInputToValue() throws ConversionException {
		Account account = input.getConvertedInput();
		if (account != null)
			return account.getName();
		else
			return null;
	}

}
