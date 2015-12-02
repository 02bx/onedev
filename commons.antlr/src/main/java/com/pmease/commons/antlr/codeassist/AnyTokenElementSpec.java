package com.pmease.commons.antlr.codeassist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.Token;

import com.google.common.collect.Lists;

public class AnyTokenElementSpec extends ElementSpec {

	private static final long serialVersionUID = 1L;

	public AnyTokenElementSpec(CodeAssist codeAssist, String label, Multiplicity multiplicity) {
		super(codeAssist, label, multiplicity);
	}

	@Override
	public List<ElementSuggestion> doSuggestFirst(ParseTree parseTree, Node parent, 
			String matchWith, Set<String> checkedRules) {
		return new ArrayList<ElementSuggestion>();
	}

	@Override
	public MandatoryScan scanMandatories(Set<String> checkedRules) {
		return MandatoryScan.stop();
	}

	@Override
	public List<TokenNode> matchOnce(AssistStream stream, 
			Node parent, Node previous, Map<String, Integer> checkedIndexes) {
		if (stream.isEof()) {
			return null;
		} else {
			Token token = stream.getCurrentToken();
			stream.increaseIndex();
			TokenNode tokenNode = new TokenNode(this, parent, previous, token);
			return Lists.newArrayList(tokenNode);
		}
	}

	@Override
	protected String asString() {
		return "any";
	}
	
}
