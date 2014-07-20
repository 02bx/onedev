package com.pmease.gitop.web.component.repository;

import org.apache.wicket.model.IModel;

import com.pmease.commons.wicket.component.select2.Select2Choice;
import com.pmease.gitop.model.Repository;

@SuppressWarnings("serial")
public class ComparableRepositoryChoice extends Select2Choice<Repository> {

	/**
	 * Constructor with model.
	 * 
	 * @param id
	 * 			component id of the choice
	 * @param currentRepositoryModel
	 * 			model of current repository from which to calculate comparable repositories. Note that 
	 * 			model.getObject() should never return null
	 * @param selectedRepositoryModel
	 * 			model of selected repository
	 */
	public ComparableRepositoryChoice(String id, IModel<Repository> currentRepositoryModel, IModel<Repository> selectedRepositoryModel) {
		super(id, selectedRepositoryModel, new ComparableRepositoryChoiceProvider(currentRepositoryModel));
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		getSettings().setPlaceholder("Typing to find a repository...");
		getSettings().setFormatResult("gitop.choiceFormatter.comparableRepository.formatResult");
		getSettings().setFormatSelection("gitop.choiceFormatter.comparableRepository.formatSelection");
		getSettings().setEscapeMarkup("gitop.choiceFormatter.comparableRepository.escapeMarkup");
	}

}
