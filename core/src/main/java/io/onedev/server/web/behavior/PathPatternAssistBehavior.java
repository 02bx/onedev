package io.onedev.server.web.behavior;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.google.common.collect.Lists;

import io.onedev.codeassist.InputCompletion;
import io.onedev.codeassist.InputStatus;
import io.onedev.codeassist.InputSuggestion;
import io.onedev.server.model.Project;
import io.onedev.server.web.behavior.inputassist.InputAssistBehavior;
import io.onedev.server.web.util.SuggestionUtils;
import io.onedev.utils.Range;

@SuppressWarnings("serial")
public class PathPatternAssistBehavior extends InputAssistBehavior {

	private final IModel<Project> projectModel;
	
	public PathPatternAssistBehavior(IModel<Project> projectModel) {
		this.projectModel = projectModel;
	}

	@Override
	public void detach(Component component) {
		super.detach(component);
		projectModel.detach();
	}

	@Override
	protected List<InputCompletion> getSuggestions(InputStatus inputStatus) {
		List<InputCompletion> completions = new ArrayList<>();
		for (InputSuggestion suggestion: SuggestionUtils.suggestPath(projectModel.getObject(), 
				inputStatus.getContentBeforeCaret().trim(), null)) {
			int caret = suggestion.getCaret();
			if (caret == -1)
				caret = suggestion.getContent().length();
			completions.add(new InputCompletion(suggestion.getContent(), suggestion.getContent(), caret, null, suggestion.getMatch()));
		}
		return completions;
	}
	
	@Override
	protected List<String> getHints(InputStatus inputStatus) {
		return Lists.newArrayList("Use * to match any part of the path");
	}

	@Override
	protected List<Range> getErrors(String inputContent) {
		return null;
	}
	
	@Override
	protected int getAnchor(String content) {
		for (int i=0; i<content.length(); i++) {
			if (!Character.isWhitespace(content.charAt(i)))
				return i;
		}
		return content.length();
	}
	
}
