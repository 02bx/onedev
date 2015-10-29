package com.pmease.gitplex.web.component.branchchoice;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;

import com.pmease.commons.wicket.component.select2.Select2Choice;
import com.vaynberg.wicket.select2.ChoiceProvider;

@SuppressWarnings("serial")
public class BranchSingleChoice extends Select2Choice<String> {

	private final boolean allowEmpty;
	
	private final String placeholder;
	
	public BranchSingleChoice(String id, IModel<String> model, ChoiceProvider<String> branchesProvider, 
			boolean allowEmpty, String placeholder) {
		super(id, model, branchesProvider);
		
		this.allowEmpty = allowEmpty;
		this.placeholder = placeholder;
	}

	public BranchSingleChoice(String id, IModel<String> model, ChoiceProvider<String> branchesProvider, 
			boolean allowEmpty) {
		this(id, model, branchesProvider, allowEmpty, null);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		getSettings().setAllowClear(allowEmpty);
		
		if (placeholder != null) 
			getSettings().setPlaceholder(placeholder);
		else
			getSettings().setPlaceholder("Choose a branch ...");
		
		getSettings().setFormatResult("gitplex.branchChoiceFormatter.formatResult");
		getSettings().setFormatSelection("gitplex.branchChoiceFormatter.formatSelection");
		getSettings().setEscapeMarkup("gitplex.branchChoiceFormatter.escapeMarkup");
		
		setConvertEmptyInputStringToNull(true);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(BranchChoiceResourceReference.INSTANCE));
	}
	
}