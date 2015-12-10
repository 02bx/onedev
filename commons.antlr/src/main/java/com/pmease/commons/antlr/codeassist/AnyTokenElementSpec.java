package com.pmease.commons.antlr.codeassist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.Token;

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
	public List<TokenNode> matchOnce(AssistStream stream, Node parent, Node previous, boolean fullMatch) {
		List<TokenNode> matches = new ArrayList<>();
		if (!stream.isEof()) {
			Token token = stream.getCurrentToken();
			stream.increaseIndex();
			TokenNode tokenNode = new TokenNode(this, parent, previous, token);
			matches.add(tokenNode);
		} else if (!fullMatch) {
			matches.add(new TokenNode(null, parent, previous, new FakedToken(stream)));
		}
		return matches;
	}

	@Override
	protected String toStringOnce() {
		return ".";
	}

	@Override
	public Set<Integer> getFirstTokenTypes() {
		return null;
	}

	@Override
	protected boolean matchesEmptyOnce() {
		return false;
	}

}
