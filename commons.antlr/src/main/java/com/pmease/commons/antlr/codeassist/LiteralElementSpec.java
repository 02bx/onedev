package com.pmease.commons.antlr.codeassist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.Token;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class LiteralElementSpec extends TokenElementSpec {

	private static final long serialVersionUID = 1L;
	
	private final String literal;
	
	public LiteralElementSpec(CodeAssist codeAssist, String label, Multiplicity multiplicity, 
			int tokenType, String literal) {
		super(codeAssist, label, multiplicity, tokenType);
		
		this.literal = literal;
	}

	public String getLiteral() {
		return literal;
	}

	@Override
	public List<ElementSuggestion> doSuggestFirst(Node parent, ParseTree parseTree, 
			String matchWith, Set<String> checkedRules) {
		if (!matchWith.equals(literal) 
				&& literal.toLowerCase().startsWith(matchWith.toLowerCase())) {
			InputSuggestion text = new InputSuggestion(literal, literal.length(), literal);
			return Lists.newArrayList(new ElementSuggestion(new Node(this, parent, null), Lists.newArrayList(text)));
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	public MandatoryScan scanMandatories(Set<String> checkedRules) {
		return new MandatoryScan(Lists.newArrayList(literal), false);
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
	protected List<TokenNode> getPartialMatchesOnce(AssistStream stream, Node parent, 
			Node previous, Map<String, Integer> checkedIndexes) {
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
		return "literal: '" + literal + "'";
	}
	
}
