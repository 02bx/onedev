package io.onedev.server.web.component.stringchoice;

import java.util.List;

import org.apache.wicket.model.IModel;

import io.onedev.server.web.component.select2.Select2Choice;

@SuppressWarnings("serial")
public class StringSingleChoice extends Select2Choice<String> {

	public StringSingleChoice(String id, IModel<String> model, List<String> choices) {
		super(id, model, new StringChoiceProvider(choices));
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		getSettings().setPlaceholder("Select below...");
		getSettings().setFormatResult("onedev.server.choiceFormatter.formatResult");
		getSettings().setFormatSelection("onedev.server.choiceFormatter.formatSelection");
		getSettings().setEscapeMarkup("onedev.server.choiceFormatter.escapeMarkup");
		
		setConvertEmptyInputStringToNull(true);
	}

}
