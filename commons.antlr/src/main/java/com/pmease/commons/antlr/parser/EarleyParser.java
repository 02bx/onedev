package com.pmease.commons.antlr.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.Token;

import com.google.common.collect.Lists;
import com.pmease.commons.antlr.grammar.ElementSpec;
import com.pmease.commons.antlr.grammar.RuleRefElementSpec;
import com.pmease.commons.antlr.grammar.RuleSpec;
import com.pmease.commons.antlr.grammar.TerminalElementSpec;

/**
 * A earley parser (https://en.wikipedia.org/wiki/Earley_parser) to parse user inputs. 
 * It is very suited for code assistance purpose as it can provide partial 
 * parse results and tell us what terminals are expected next. Note that for large 
 * inputs (thousands of lines for example), our earley implementation performs bad 
 * and consumes a lot of memory, but it serves our purpose to provide a mini code 
 * assistance for web input 
 * 
 * @author robin
 *
 */
public class EarleyParser {

	private final RuleSpec rule;
	
	private final List<Token> tokens;
	
	private int tokenIndex = 0;
	
	private final List<State> states = new ArrayList<>();

	public EarleyParser(RuleSpec rule, List<Token> tokens) {
		this.rule = rule;
		this.tokens = tokens;
		
		Set<Node> nodes = new HashSet<>();
		for (int i=0; i<rule.getAlternatives().size(); i++) 
			nodes.add(new Node(tokenIndex, rule, i, 0, false, new ArrayList<Element>()));

		while (!nodes.isEmpty()) {
			State state = new State(this, tokenIndex, nodes);
			states.add(state);
			for (Node node: Lists.newArrayList(state.getNodes())) // avoid concurrent modification
				process(node, state);

			if (tokenIndex == tokens.size())
				break;
			
			nodes = new HashSet<>();
			for (Node node: state.getNodes()) 
				scan(node, nodes);
			tokenIndex++;
		}
	}
	
	private void process(Node node, State state) {
		if (!node.isCompleted()) { // predict
			ElementSpec expectedElementSpec = node.getExpectedElementSpec();
			if (expectedElementSpec instanceof RuleRefElementSpec) {
				RuleRefElementSpec ruleRefElement = (RuleRefElementSpec) expectedElementSpec;
				RuleSpec elementRule = ruleRefElement.getRule();
				for (int i=0; i<elementRule.getAlternatives().size(); i++) {
					Node predictedNode = new Node(tokenIndex, 
							elementRule, i, 0, false, new ArrayList<Element>());
					if (state.addNode(predictedNode))
						process(predictedNode, state);
				}
			}
			if (expectedElementSpec.isOptional() || node.isExpectedElementSpecMatchedOnce()) {
				Node advancedNode = new Node(node.getBeginTokenIndex(), node.getRuleSpec(), 
						node.getAlternativeSpecIndex(), node.getExpectedElementSpecIndex()+1, false, 
						new ArrayList<>(node.getElements()));
				if (state.addNode(advancedNode))
					process(advancedNode, state);
			}
		} else { // complete
			State startState = states.get(node.getBeginTokenIndex());
			Collection<Node> startNodes;
			if (node.getBeginTokenIndex() == state.getEndTokenIndex())
				startNodes = Lists.newArrayList(startState.getNodes()); // avoid concurrent modification
			else
				startNodes = startState.getNodes();
			for (Node startNode: startNodes) {
				if (!startNode.isCompleted()) {
					ElementSpec expectedElementSpec = startNode.getExpectedElementSpec();
					if (expectedElementSpec instanceof RuleRefElementSpec) {
						RuleRefElementSpec ruleRefElement = (RuleRefElementSpec) expectedElementSpec;
						if (ruleRefElement.getRuleName().equals(node.getRuleSpec().getName())) {
							Node advancedNode;
							List<Element> elements = new ArrayList<>(startNode.getElements());
							if (!node.getElements().isEmpty())
								elements.add(new Element(this, ruleRefElement, state.getEndTokenIndex(), node));
							if (!expectedElementSpec.isMultiple()) {
								advancedNode = new Node(startNode.getBeginTokenIndex(), 
										startNode.getRuleSpec(), startNode.getAlternativeSpecIndex(), 
										startNode.getExpectedElementSpecIndex()+1, false, elements);
							} else {
								advancedNode = new Node(startNode.getBeginTokenIndex(), 
										startNode.getRuleSpec(), startNode.getAlternativeSpecIndex(), 
										startNode.getExpectedElementSpecIndex(), true, elements);
							}
							if (state.addNode(advancedNode))
								process(advancedNode, state);
						}
					}
				}
			}
		}
	}
	
	private void scan(Node node, Set<Node> nextNodes) {
		if (!node.isCompleted()) {
			ElementSpec expectedElementSpec = node.getExpectedElementSpec();
			int tokenType = tokens.get(tokenIndex).getType();
			if ((expectedElementSpec instanceof TerminalElementSpec) 
					&& ((TerminalElementSpec)expectedElementSpec).isToken(tokenType)) {
				Node scanNode;
				List<Element> elements = new ArrayList<>(node.getElements());
				elements.add(new Element(this, expectedElementSpec, tokenIndex+1, null));
				if (!expectedElementSpec.isMultiple()) {
					scanNode = new Node(node.getBeginTokenIndex(), node.getRuleSpec(), 
							node.getAlternativeSpecIndex(), node.getExpectedElementSpecIndex()+1, 
							false, elements);
				} else {
					scanNode = new Node(node.getBeginTokenIndex(), node.getRuleSpec(), 
							node.getAlternativeSpecIndex(), node.getExpectedElementSpecIndex(), 
							true, elements);
				}
				nextNodes.add(scanNode);
			}
		}
	}
	
	public List<State> getStates() {
		return states;
	}
	
	public RuleSpec getRule() {
		return rule;
	}

	public List<Token> getTokens() {
		return tokens;
	}

	public List<Node> getMatches() {
		if (states.size() == tokens.size()+1) 
			return states.get(tokens.size()).getMatches(rule.getName());
		else
			return new ArrayList<>();
	}
	
	public boolean matches() {
		return !getMatches().isEmpty();
	}
	
	@Nullable
	public Token getLastMatchedToken() {
		int endOfMatch = getEndOfMatch();
		if (endOfMatch > 0)
			return tokens.get(endOfMatch-1);
		else
			return null;
	}
	
	public List<Token> getMatchedTokens() {
		int endOfMatch = getEndOfMatch();
		if (endOfMatch > 0)
			return tokens.subList(0, endOfMatch);
		else
			return new ArrayList<>();
	}
	
	/**
	 * Get the next token index after the match. 
	 * 
	 * @param tokens
	 * 			tokens to match against
	 * @return
	 * 			next token index after the match.
	 * 			<ul><li>greater than 0 if the rule matches part of the tokens
	 * 			<li>0 if the rule does not match any tokens, and the rule allows to be empty
	 * 			<li>-1 if the rule does not match any tokens, and the rule does not allow 
	 * 			to be empty
	 */
	public int getEndOfMatch() {
		for (int i=states.size()-1; i>=0; i--) {
			State state = states.get(i);
			if (!state.getMatches(rule.getName()).isEmpty())
				return i;
		}
		return -1;
	}
	
}
