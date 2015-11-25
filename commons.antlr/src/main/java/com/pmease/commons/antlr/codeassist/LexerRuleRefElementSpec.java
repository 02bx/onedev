package com.pmease.commons.antlr.codeassist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.Token;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.pmease.commons.util.StringUtils;

public class LexerRuleRefElementSpec extends TokenElementSpec {

	private static final long serialVersionUID = 1L;

	private final String ruleName;
	
	private transient Optional<RuleSpec> rule;
	
	public LexerRuleRefElementSpec(CodeAssist codeAssist, String label, Multiplicity multiplicity, 
			int tokenType, String ruleName) {
		super(codeAssist, label, multiplicity, tokenType);

		this.ruleName = ruleName;
	}

	public String getRuleName() {
		return ruleName;
	}
	
	public RuleSpec getRule() {
		if (rule == null)
			rule = Optional.fromNullable(codeAssist.getRule(ruleName));
		return rule.orNull();
	}

	@Override
	public List<ElementSuggestion> doSuggestFirst(Node parent, ParseTree parseTree, 
			String matchWith, Set<String> checkedRules) {
		if (!checkedRules.contains(ruleName) && getRule() != null) {
			checkedRules.add(ruleName);
			return getRule().suggestFirst(parseTree, new Node(this, parent, null), matchWith, checkedRules);
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	public CaretMove skipMandatories(String content, int offset) {
		if (getRule() != null) {
			List<AlternativeSpec> alternatives = getRule().getAlternatives();
			if (alternatives.size() == 1) {
				AlternativeSpec alternativeSpec = alternatives.get(0);
				for (ElementSpec elementSpec: alternativeSpec.getElements()) {
					if (elementSpec.getMultiplicity() == Multiplicity.ZERO_OR_ONE 
							|| elementSpec.getMultiplicity() == Multiplicity.ZERO_OR_MORE) {
						return new CaretMove(offset, true);
					} else if (elementSpec.getMultiplicity() == Multiplicity.ONE_OR_MORE) {
						CaretMove move = elementSpec.skipMandatories(content, offset);
						return new CaretMove(move.getOffset(), true);
					} else {
						CaretMove move = elementSpec.skipMandatories(content, offset);
						offset = move.getOffset();
						if (move.isStop())
							return new CaretMove(offset, true);
					}
				}
				return new CaretMove(offset, false);
			} else {
				return new CaretMove(offset, true);
			}
		} else {
			return new CaretMove(offset, true);
		}
	}

	@Override
	public List<String> getMandatories(Set<String> checkedRules) {
		if (!checkedRules.contains(ruleName)) {
			checkedRules.add(ruleName);
		
			List<String> mandatories = new ArrayList<>();
			if (getRule() != null) {
				List<AlternativeSpec> alternatives = getRule().getAlternatives();
				if (alternatives.size() == 1) {
					for (ElementSpec elementSpec: alternatives.get(0).getElements()) {
						if (elementSpec.getMultiplicity() == Multiplicity.ONE 
								|| elementSpec.getMultiplicity() == Multiplicity.ONE_OR_MORE) {
							mandatories.addAll(elementSpec.getMandatories(new HashSet<>(checkedRules)));
						}
					}
				} 
			}
			if (!mandatories.isEmpty())
				return Lists.newArrayList(StringUtils.join(mandatories, ""));
			else
				return mandatories;
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	protected boolean matchOnce(AssistStream stream, Map<String, Integer> checkedIndexes) {
		if (stream.isEof()) {
			return false;
		} else if (stream.getCurrentToken().getType() == type) {
			stream.increaseIndex();
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected List<TokenNode> getPartialMatchesOnce(AssistStream stream, 
			Node parent, Node previous, Map<String, Integer> checkedIndexes) {
		Preconditions.checkArgument(!stream.isEof());
		
		Token token = stream.getCurrentToken();
		if (token.getType() == type) {
			stream.increaseIndex();
			return Lists.newArrayList(new TokenNode(this, parent, previous, token));
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	public String toString() {
		return "lexer_rule_ref: " + ruleName;
	}
	
}
