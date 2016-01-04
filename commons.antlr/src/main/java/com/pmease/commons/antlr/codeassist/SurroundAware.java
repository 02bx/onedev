package com.pmease.commons.antlr.codeassist;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.Token;

import com.pmease.commons.antlr.grammar.ElementSpec;
import com.pmease.commons.antlr.grammar.Grammar;
import com.pmease.commons.util.pattern.Highlight;

public abstract class SurroundAware {
	
	private final Grammar grammar;
	
	private final String prefix;
	
	private final String suffix;
	
	public SurroundAware(Grammar grammar, String prefix, String suffix) {
		this.grammar = grammar;
		this.prefix = prefix;
		this.suffix = suffix;
	}
	
	private boolean matches(ElementSpec spec, String content) {
		List<Token> tokens = grammar.lex(content);
		if (tokens.isEmpty()) {
			return content.length() == 0 && spec.isAllowEmpty();
		} else {
			int start = tokens.get(0).getStartIndex();
			int stop = tokens.get(tokens.size()-1).getStopIndex()+1;
			return start == 0 && stop == content.length() && spec.getEndOfMatch(tokens) == tokens.size();
		}
	}
	
	public List<InputSuggestion> suggest(ElementSpec spec, String matchWith) {
		String unsurroundedMatchWith = matchWith;
		if (matchWith.startsWith(prefix))
			unsurroundedMatchWith = unsurroundedMatchWith.substring(prefix.length());
		unsurroundedMatchWith = unsurroundedMatchWith.trim();
		if (unsurroundedMatchWith.endsWith(suffix))
			return new ArrayList<>();
		
		List<InputSuggestion> suggestions = match(unsurroundedMatchWith);
		if (suggestions != null) {
			List<InputSuggestion> checkedSuggestions = new ArrayList<>();
			
			for (InputSuggestion suggestion: suggestions) {
				String content = suggestion.getContent();
				int caret = suggestion.getCaret();
				if (!matches(spec, content)) {
					content = prefix + content + suffix;
					Highlight highlight = suggestion.getHighlight();
					if (caret != -1) 
						caret += prefix.length();
					if (highlight != null)
						highlight = new Highlight(highlight.getFrom()+prefix.length(), highlight.getTo()+prefix.length());
					checkedSuggestions.add(new InputSuggestion(content, caret, true, suggestion.getDescription(), highlight));
				} else {
					checkedSuggestions.add(suggestion);
				}
			}
			
			/*
			 * Check to see if the matchWith should be surrounded and return as a suggestion if no other 
			 * suggestions. For instance, you may have a rule requiring that value containing spaces 
			 * should be quoted, in this case, below code will suggest you to quote the value if it 
			 * contains spaces as otherwise it will fail the match below
			 */
			if (checkedSuggestions.isEmpty() && matchWith.length() != 0 && !matches(spec, unsurroundedMatchWith)) {
				unsurroundedMatchWith = prefix + unsurroundedMatchWith + suffix;
				if (matches(spec, unsurroundedMatchWith)) {
					Highlight highlight = new Highlight(1, unsurroundedMatchWith.length()-1);
					checkedSuggestions.add(new InputSuggestion(unsurroundedMatchWith, getSurroundDescription(), highlight));
				}
			}
			
			if (checkedSuggestions.isEmpty())
				checkedSuggestions.add(new InputSuggestion(prefix, null, null));
			return checkedSuggestions;
		} else {
			return null;
		}
	}
	
	protected String getSurroundDescription() {
		return null;
	}
	
	/**
	 * Match with provided string to give a list of suggestions
	 * 
	 * @param surroundlessMatchWith
	 * 			string with surrounding literals removed 
	 * @return
	 * 			a list of suggestions. If you do not have any suggestions and want code assist to 
	 * 			drill down the element to provide default suggestions, return a <tt>null</tt> value 
	 * 			instead of an empty list
	 */
	@Nullable
	protected abstract List<InputSuggestion> match(String surroundlessMatchWith);
}
