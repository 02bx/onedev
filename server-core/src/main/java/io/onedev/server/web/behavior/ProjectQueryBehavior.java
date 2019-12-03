package io.onedev.server.web.behavior;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import io.onedev.commons.codeassist.FenceAware;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.commons.codeassist.parser.Element;
import io.onedev.commons.codeassist.parser.ParseExpect;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.server.OneException;
import io.onedev.server.search.entity.project.ProjectQuery;
import io.onedev.server.search.entity.project.ProjectQueryParser;
import io.onedev.server.util.ProjectConstants;
import io.onedev.server.web.behavior.inputassist.ANTLRAssistBehavior;
import io.onedev.server.web.util.SuggestionUtils;

@SuppressWarnings("serial")
public class ProjectQueryBehavior extends ANTLRAssistBehavior {

	public ProjectQueryBehavior() {
		super(ProjectQueryParser.class, "query", false);
	}

	@Override
	protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
		if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
			LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
			if (spec.getRuleName().equals("Quoted")) {
				return new FenceAware(codeAssist.getGrammar(), '"', '"') {

					@Override
					protected List<InputSuggestion> match(String matchWith) {
						if ("criteriaField".equals(spec.getLabel())) {
							return SuggestionUtils.suggest(ProjectConstants.QUERY_FIELDS, matchWith);
						} else if ("orderField".equals(spec.getLabel())) {
							return SuggestionUtils.suggest(new ArrayList<>(ProjectConstants.ORDER_FIELDS.keySet()), matchWith);
						} else if ("criteriaValue".equals(spec.getLabel())) {
							List<Element> fieldElements = terminalExpect.getState().findMatchedElementsByLabel("criteriaField", true);
							List<Element> operatorElements = terminalExpect.getState().findMatchedElementsByLabel("operator", true);
							Preconditions.checkState(operatorElements.size() == 1);
							String operatorName = StringUtils.normalizeSpace(operatorElements.get(0).getMatchedText());
							int operator = ProjectQuery.getOperator(operatorName);							
							if (fieldElements.isEmpty()) {
								return SuggestionUtils.suggestProjects(matchWith);
							} else {
								String fieldName = ProjectQuery.getValue(fieldElements.get(0).getMatchedText());
								try {
									ProjectQuery.checkField(fieldName, operator);
									if (fieldName.equals(ProjectConstants.FIELD_NAME)) {
										if (!matchWith.contains("*"))
											return SuggestionUtils.suggestProjects(matchWith);
										else
											return null;
									} else if (fieldName.equals(ProjectConstants.FIELD_OWNER)) { 
										return SuggestionUtils.suggestUsers(matchWith);
									} else {
										return null;
									}
								} catch (OneException ex) {
								}
							}
						}
						return new ArrayList<>();
					}
					
					@Override
					protected String getFencingDescription() {
						return "quote as literal value";
					}
					
				}.suggest(terminalExpect);
			}
		} 
		return null;
	}
	
	@Override
	protected Optional<String> describe(ParseExpect parseExpect, String suggestedLiteral) {
		parseExpect = parseExpect.findExpectByLabel("operator");
		if (parseExpect != null) {
			List<Element> fieldElements = parseExpect.getState().findMatchedElementsByLabel("criteriaField", false);
			if (!fieldElements.isEmpty()) {
				String fieldName = ProjectQuery.getValue(fieldElements.iterator().next().getMatchedText());
				try {
					ProjectQuery.checkField(fieldName, ProjectQuery.getOperator(suggestedLiteral));
				} catch (OneException e) {
					return null;
				}
			}
		}
		return super.describe(parseExpect, suggestedLiteral);
	}

	@Override
	protected List<String> getHints(TerminalExpect terminalExpect) {
		List<String> hints = new ArrayList<>();
		if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
			LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
			if ("criteriaValue".equals(spec.getLabel()) && ProjectQuery.isInsideQuote(terminalExpect.getUnmatchedText())) {
				List<Element> fieldElements = terminalExpect.getState().findMatchedElementsByLabel("criteriaField", true);
				if (!fieldElements.isEmpty()) {
					String fieldName = ProjectQuery.getValue(fieldElements.get(0).getMatchedText());
					if (fieldName.equals(ProjectConstants.FIELD_NAME) 
							|| fieldName.equals(ProjectConstants.FIELD_DESCRIPTION)) {
						hints.add("Use * for wildcard match");
						hints.add("Use '\\' to escape quotes");
					}
				}
			}
		} 
		return hints;
	}

}
