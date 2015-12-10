package com.pmease.commons.antlr.codeassist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Optional;

public class RuleSpec extends Spec {

	private static final long serialVersionUID = 1L;

	private final String name;
	
	private final List<AlternativeSpec> alternatives;
	
	private Optional<Set<Integer>> firstTokenTypesOptional;
	
	private Boolean matchesEmpty;

	public RuleSpec(CodeAssist codeAssist, String name, List<AlternativeSpec> alternatives) {
		super(codeAssist);
		
		this.name = name;
		this.alternatives = alternatives;
	}
	
	public String getName() {
		return name;
	}

	public List<AlternativeSpec> getAlternatives() {
		return alternatives;
	}

	@Override
	public List<ElementSuggestion> suggestFirst(ParseTree parseTree, Node parent, 
			String matchWith, Set<String> checkedRules) {
		parent = new Node(this, parent, null);
		List<ElementSuggestion> first = new ArrayList<>();
		if (!checkedRules.contains(name)) {
			checkedRules.add(name);
			for (AlternativeSpec alternative: alternatives)
				first.addAll(alternative.suggestFirst(parseTree, parent, matchWith, new HashSet<>(checkedRules)));
		} 
		
		return first;
	}

	@Override
	public List<TokenNode> match(AssistStream stream, Node parent, Node previous, boolean fullMatch) {
		if (stream.isEof()) {
			if (fullMatch && !matchesEmpty())
				return new ArrayList<>();
			else
				return initMatches(stream, parent, previous);
		} 
		
		if (getFirstTokenTypes() != null) {
			int tokenType = stream.getCurrentToken().getType();
			if (!getFirstTokenTypes().contains(tokenType)) {
				if (matchesEmpty())
					return initMatches(stream, parent, previous);
				else
					return new ArrayList<>();
			}
		}

		List<TokenNode> matches = new ArrayList<>();
		int index = stream.getIndex();
		parent = new Node(this, parent, previous);
		boolean fakedAdded = false;
		for (AlternativeSpec alternative: alternatives) {
			for (TokenNode match: alternative.match(stream, parent, parent, fullMatch)) {
				if (match.getToken().getTokenIndex() == index-1) {
					if (!fakedAdded) {
						matches.add(new TokenNode(null, parent, parent, new FakedToken(index-1)));
						fakedAdded = true;
					}
				} else {
					matches.add(match);
				}
			}
			stream.setIndex(index);
		}
		codeAssist.prune(matches, stream);
		return matches;
	}

	@Override
	public Set<Integer> getFirstTokenTypes() {
		if (firstTokenTypesOptional == null) {
			// return this if this rule is invoked recursively
			Set<Integer> recursiveTokenTypes = new HashSet<>();
			firstTokenTypesOptional = Optional.of(recursiveTokenTypes);
			
			Set<Integer> leadingTokenTypes =  new HashSet<>();
			for (AlternativeSpec alternative: alternatives) {
				Set<Integer> alternativeLeadingTokenTypes = alternative.getFirstTokenTypes();
				if (alternativeLeadingTokenTypes == null) {
					leadingTokenTypes = null;
					break;
				} else {
					leadingTokenTypes.addAll(alternativeLeadingTokenTypes);
				}
			}
			firstTokenTypesOptional = Optional.fromNullable(leadingTokenTypes);
		}
		return firstTokenTypesOptional.orNull();
	}
	
	@Override
	public boolean matchesEmpty() {
		if (matchesEmpty == null) {
			// return this if this rule is invoked recursively
			matchesEmpty = false; 
			
			boolean allowEmpty = false;
			for (AlternativeSpec alternative: alternatives) {
				if (alternative.matchesEmpty()) {
					allowEmpty = true;
					break;
				}
			}
			matchesEmpty = allowEmpty;
		}
		return matchesEmpty;
	}
	
	@Override
	public String toString() {
		List<String> alternativeStrings = new ArrayList<>();
		for (AlternativeSpec alternative: alternatives)
			alternativeStrings.add(alternative.toString());
		return StringUtils.join(alternativeStrings, " | ");
	}

}
