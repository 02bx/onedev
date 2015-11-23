package com.pmease.commons.antlr.codeassist;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.google.common.base.Preconditions;
import com.pmease.commons.antlr.ANTLRv4Lexer;
import com.pmease.commons.antlr.ANTLRv4Parser;
import com.pmease.commons.antlr.ANTLRv4Parser.AlternativeContext;
import com.pmease.commons.antlr.ANTLRv4Parser.AtomContext;
import com.pmease.commons.antlr.ANTLRv4Parser.BlockContext;
import com.pmease.commons.antlr.ANTLRv4Parser.EbnfContext;
import com.pmease.commons.antlr.ANTLRv4Parser.EbnfSuffixContext;
import com.pmease.commons.antlr.ANTLRv4Parser.ElementContext;
import com.pmease.commons.antlr.ANTLRv4Parser.LabeledAltContext;
import com.pmease.commons.antlr.ANTLRv4Parser.LabeledElementContext;
import com.pmease.commons.antlr.ANTLRv4Parser.LabeledLexerElementContext;
import com.pmease.commons.antlr.ANTLRv4Parser.LexerAltContext;
import com.pmease.commons.antlr.ANTLRv4Parser.LexerAtomContext;
import com.pmease.commons.antlr.ANTLRv4Parser.LexerBlockContext;
import com.pmease.commons.antlr.ANTLRv4Parser.LexerElementContext;
import com.pmease.commons.antlr.ANTLRv4Parser.LexerRuleBlockContext;
import com.pmease.commons.antlr.ANTLRv4Parser.LexerRuleSpecContext;
import com.pmease.commons.antlr.ANTLRv4Parser.NotSetContext;
import com.pmease.commons.antlr.ANTLRv4Parser.ParserRuleSpecContext;
import com.pmease.commons.antlr.ANTLRv4Parser.RuleBlockContext;
import com.pmease.commons.antlr.ANTLRv4Parser.RuleSpecContext;
import com.pmease.commons.antlr.ANTLRv4Parser.SetElementContext;
import com.pmease.commons.antlr.codeassist.ElementSpec.Multiplicity;
import com.pmease.commons.util.StringUtils;

public abstract class CodeAssist {

	private final Constructor<? extends Lexer> lexerConstructor;
	
	private final Map<String, RuleSpec> rules = new HashMap<>();
	
	private final Map<String, Integer> tokenTypesByLiteral = new HashMap<>();

	private final Map<String, Integer> tokenTypesByRule = new HashMap<>();
	
	public CodeAssist(Class<? extends Lexer> lexerClass) {
		this(lexerClass, new String[]{getGrammarFile(lexerClass)}, getTokenFile(lexerClass));
	}

	private static String getGrammarFile(Class<?> lexerClass) {
		String lexerName = lexerClass.getName().replace(".", "/");
		return lexerName.substring(0, lexerName.length() - "Lexer".length()) + ".g4";
	}
	
	private static String getTokenFile(Class<?> lexerClass) {
		return lexerClass.getSimpleName() + ".tokens";
	}
	
	/**
	 * Construct object representation of ANTLR grammar file.
	 * 
	 * @param grammarFiles
	 * 			grammar files in class path, relative to class path root
	 * @param tokenFile
	 * 			generated tokens file in class path, relative to class path root
	 */
	public CodeAssist(Class<? extends Lexer> lexerClass, String grammarFiles[], String tokenFile) {
		try {
			this.lexerConstructor = lexerClass.getConstructor(CharStream.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		
		tokenTypesByRule.put("EOF", Token.EOF);
		
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(tokenFile)) {
			Properties props = new Properties();
			props.load(is);
			for (Map.Entry<Object, Object> entry: props.entrySet()) {
				String key = (String) entry.getKey();
				Integer value = Integer.valueOf((String) entry.getValue());
				if (key.startsWith("'"))
					tokenTypesByLiteral.put(key.substring(1, key.length()-1), value);
				else
					tokenTypesByRule.put(key, value);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	
		for (String grammarFile: grammarFiles) {
			try (InputStream is = getClass().getClassLoader().getResourceAsStream(grammarFile)) {
				ANTLRv4Lexer lexer = new ANTLRv4Lexer(new ANTLRInputStream(is));
				CommonTokenStream tokens = new CommonTokenStream(lexer);
				ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
				parser.removeErrorListeners();
				parser.setErrorHandler(new BailErrorStrategy());
				for (RuleSpecContext ruleSpecContext: parser.grammarSpec().rules().ruleSpec()) {
					RuleSpec rule = newRule(ruleSpecContext);
					rules.put(rule.getName(), rule);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private RuleSpec newRule(RuleSpecContext ruleSpecContext) {
		String name;
		List<AlternativeSpec> alternatives = new ArrayList<>();
		ParserRuleSpecContext parserRuleSpecContext = ruleSpecContext.parserRuleSpec();
		if (parserRuleSpecContext != null) {
			name = parserRuleSpecContext.RULE_REF().getText();
			RuleBlockContext ruleBlockContext = parserRuleSpecContext.ruleBlock();
			for (LabeledAltContext labeledAltContext: ruleBlockContext.ruleAltList().labeledAlt())
				alternatives.add(newAltenative(labeledAltContext));
		} else {
			LexerRuleSpecContext lexerRuleSpecContext = ruleSpecContext.lexerRuleSpec();
			name = lexerRuleSpecContext.TOKEN_REF().getText();
			LexerRuleBlockContext lexerRuleBlockContext = lexerRuleSpecContext.lexerRuleBlock();
			for (LexerAltContext lexerAltContext: lexerRuleBlockContext.lexerAltList().lexerAlt())
				alternatives.add(newAltenative(lexerAltContext));
		}
		return new RuleSpec(this, name, alternatives);
	}
	
	private AlternativeSpec newAltenative(LexerAltContext lexerAltContext) {
		List<ElementSpec> elements = new ArrayList<>();
		if (lexerAltContext.lexerElements() != null) {
			for (LexerElementContext lexerElementContext: lexerAltContext.lexerElements().lexerElement()) {
				ElementSpec element = newElement(lexerElementContext);
				if (element != null)
					elements.add(element);
			}
		}
		
		return new AlternativeSpec(this, null, elements);
	}
	
	private AlternativeSpec newAltenative(LabeledAltContext labeledAltContext) {
		String label;
		if (labeledAltContext.id() != null)
			label = labeledAltContext.id().getText();
		else
			label = null;
		
		return newAltenative(label, labeledAltContext.alternative());
	}
	
	private ElementSpec newElement(LexerElementContext lexerElementContext) {
		LabeledLexerElementContext labeledLexerElementContext = lexerElementContext.labeledLexerElement();
		if (labeledLexerElementContext != null) {
			String label = labeledLexerElementContext.id().getText();
			LexerAtomContext lexerAtomContext = labeledLexerElementContext.lexerAtom();
			if (lexerAtomContext != null)
				return newElement(label, lexerAtomContext, lexerElementContext.ebnfSuffix());
			else 
				return newElement(label, labeledLexerElementContext.block(), lexerElementContext.ebnfSuffix());
		} else if (lexerElementContext.lexerAtom() != null) {
			return newElement(null, lexerElementContext.lexerAtom(), lexerElementContext.ebnfSuffix());
		} else if (lexerElementContext.lexerBlock() != null) {
			return newElement(null, lexerElementContext.lexerBlock(), lexerElementContext.ebnfSuffix());
		} else {
			return null;
		}
	}
	
	private AlternativeSpec newAltenative(@Nullable String label, AlternativeContext alternativeContext) {
		List<ElementSpec> elements = new ArrayList<>();
		for (ElementContext elementContext: alternativeContext.element()) {
			ElementSpec element = newElement(elementContext);
			if (element != null)
				elements.add(element);
		}
		
		return new AlternativeSpec(this, label, elements);
	}
	
	@Nullable
	private ElementSpec newElement(ElementContext elementContext) {
		LabeledElementContext labeledElementContext = elementContext.labeledElement();
		if (labeledElementContext != null) {
			String label = labeledElementContext.id().getText();
			AtomContext atomContext = labeledElementContext.atom();
			if (atomContext != null)
				return newElement(label, atomContext, elementContext.ebnfSuffix());
			else 
				return newElement(label, labeledElementContext.block(), elementContext.ebnfSuffix());
		} else if (elementContext.atom() != null) {
			return newElement(null, elementContext.atom(), elementContext.ebnfSuffix());
		} else if (elementContext.ebnf() != null) {
			return newElement(elementContext.ebnf());
		} else {
			return null;
		}
	}
	
	private ElementSpec newElement(String label, AtomContext atomContext, EbnfSuffixContext ebnfSuffixContext) {
		Multiplicity multiplicity = newMultiplicity(ebnfSuffixContext);
		if (atomContext.terminal() != null) {
			if (atomContext.terminal().TOKEN_REF() != null) {
				String ruleName = atomContext.terminal().TOKEN_REF().getText();
				int tokenType = tokenTypesByRule.get(ruleName);
				if (tokenType != Token.EOF)
					return new LexerRuleRefElementSpec(this ,label, multiplicity, tokenType, ruleName);
				else
					return new EofElementSpec(this, label, multiplicity);
			} else {
				String literal = getLiteral(atomContext.terminal().STRING_LITERAL());
				int tokenType = tokenTypesByLiteral.get(literal);
				return new LiteralElementSpec(this, label, multiplicity, tokenType, literal);
			}
		} else if (atomContext.ruleref() != null) {
			return new RuleRefElementSpec(this, label, multiplicity, atomContext.ruleref().RULE_REF().getText());
		} else if (atomContext.notSet() != null) {
			return new NotTokenElementSpec(this, label, multiplicity, getNegativeTokenTypes(atomContext.notSet()));
		} else if (atomContext.DOT() != null) {
			return new AnyTokenElementSpec(this, label, multiplicity);
		} else {
			throw new IllegalStateException();
		}
	}

	private String getLiteral(TerminalNode terminal) {
		String literal = terminal.getText();
		return literal.substring(1, literal.length()-1);
	}
	
	private ElementSpec newElement(String label, LexerAtomContext lexerAtomContext, EbnfSuffixContext ebnfSuffixContext) {
		Multiplicity multiplicity = newMultiplicity(ebnfSuffixContext);
		if (lexerAtomContext.terminal() != null) {
			if (lexerAtomContext.terminal().TOKEN_REF() != null) {
				String ruleName = lexerAtomContext.terminal().TOKEN_REF().getText();
				Integer tokenType = tokenTypesByRule.get(ruleName);
				if (tokenType == null) // fragment rule
					tokenType = 0;
				if (tokenType != Token.EOF)
					return new LexerRuleRefElementSpec(this, label, multiplicity, tokenType, ruleName);
				else
					return new EofElementSpec(this, label, multiplicity);
			} else {
				String literal = getLiteral(lexerAtomContext.terminal().STRING_LITERAL());
				Integer tokenType = tokenTypesByLiteral.get(literal);
				if (tokenType == null)
					tokenType = 0;
				return new LiteralElementSpec(this, label, multiplicity, tokenType, literal);
			}
		} else if (lexerAtomContext.RULE_REF() != null) {
			return new RuleRefElementSpec(this, label, multiplicity, lexerAtomContext.RULE_REF().getText());
		} else if (lexerAtomContext.notSet() != null 
				|| lexerAtomContext.DOT() != null 
				|| lexerAtomContext.LEXER_CHAR_SET()!=null 
				|| lexerAtomContext.range() != null) {
			return new AnyTokenElementSpec(this, label, multiplicity);
		} else {
			throw new IllegalStateException();
		}
	}
	
	private Set<Integer> getNegativeTokenTypes(NotSetContext notSetContext) {
		Set<Integer> negativeTokenTypes = new HashSet<>();
		if (notSetContext.setElement() != null) {
			negativeTokenTypes.add(getTokenType(notSetContext.setElement()));
		} else {
			for (SetElementContext setElementContext: notSetContext.blockSet().setElement())
				negativeTokenTypes.add(getTokenType(setElementContext));
		}
		return negativeTokenTypes;
	}
	
	private int getTokenType(SetElementContext setElementContext) {
		Integer tokenType;
		if (setElementContext.STRING_LITERAL() != null) 
			tokenType = tokenTypesByLiteral.get(getLiteral(setElementContext.STRING_LITERAL()));
		else if (setElementContext.TOKEN_REF() != null)
			tokenType = tokenTypesByRule.get(setElementContext.TOKEN_REF().getText());
		else 
			tokenType = null;
		if (tokenType != null)
			return tokenType;
		else
			throw new IllegalStateException();
	}
	
	private Multiplicity newMultiplicity(@Nullable EbnfSuffixContext ebnfSuffixContext) {
		if (ebnfSuffixContext != null) {
			if (ebnfSuffixContext.STAR() != null)
				return Multiplicity.ZERO_OR_MORE;
			else if (ebnfSuffixContext.PLUS() != null)
				return Multiplicity.ONE_OR_MORE;
			else
				return Multiplicity.ZERO_OR_ONE;
		} else {
			return Multiplicity.ONE;
		}
	}
	
	private ElementSpec newElement(@Nullable String label, BlockContext blockContext, @Nullable EbnfSuffixContext ebnfSuffixContext) {
		List<AlternativeSpec> alternatives = new ArrayList<>();
		for (AlternativeContext alternativeContext: blockContext.altList().alternative())
			alternatives.add(newAltenative(null, alternativeContext));
		String ruleName = UUID.randomUUID().toString();
		RuleSpec rule = new RuleSpec(this, ruleName, alternatives);
		rules.put(ruleName, rule);
		return new RuleRefElementSpec(this, label, newMultiplicity(ebnfSuffixContext), ruleName);
	}
	
	private ElementSpec newElement(@Nullable String label, LexerBlockContext lexerBlockContext, @Nullable EbnfSuffixContext ebnfSuffixContext) {
		List<AlternativeSpec> alternatives = new ArrayList<>();
		for (LexerAltContext lexerAltContext: lexerBlockContext.lexerAltList().lexerAlt())
			alternatives.add(newAltenative(lexerAltContext));
		String ruleName = UUID.randomUUID().toString();
		RuleSpec rule = new RuleSpec(this, ruleName, alternatives);
		rules.put(ruleName, rule);
		return new RuleRefElementSpec(this, label, newMultiplicity(ebnfSuffixContext), ruleName);
	}
	
	private ElementSpec newElement(EbnfContext ebnfContext) {
		if (ebnfContext.blockSuffix() != null)
			return newElement(null, ebnfContext.block(), ebnfContext.blockSuffix().ebnfSuffix());
		else
			return newElement(null, ebnfContext.block(), null);
	}
	
	@Nullable
	public RuleSpec getRule(String ruleName) {
		return rules.get(ruleName);
	}
	
	public final TokenStream lex(String input) {
		try {
			List<Token> tokens = new ArrayList<>();
			Lexer lexer = lexerConstructor.newInstance(new ANTLRInputStream(input));
			lexer.removeErrorListeners();
			Token token;
			do {
				token = lexer.nextToken();
				if (token.getChannel() == Token.DEFAULT_CHANNEL)
					tokens.add(token);
			} while (token.getType() != Token.EOF);
			
			return new TokenStream(tokens);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<CaretAwareText> suggest(CaretAwareText input, String ruleName) {
		List<CaretAwareText> texts = new ArrayList<>();
		RuleSpec rule = Preconditions.checkNotNull(getRule(ruleName));
		String contentBeforeCaret = input.getContentBeforeCaret();
		TokenStream stream = lex(contentBeforeCaret);
		
		if (!stream.isEof()) {
			Token lastToken = stream.getToken(stream.size()-2);
			String matchWith;
			int beyondLastToken = input.getCaret() - lastToken.getStopIndex() -1; 
			if (beyondLastToken > 0) {
				matchWith = contentBeforeCaret.substring(input.getCaret()-beyondLastToken, input.getCaret());
				matchWith = StringUtils.trimStart(matchWith);
				mergeSuggestions(suggest(rule, stream, input, matchWith), texts);
			} else {
				mergeSuggestions(suggest(rule, stream, input, ""), texts);

				matchWith = stream.getToken(stream.size()-2).getText();
				List<Token> tokens = stream.getTokens();
				tokens.remove(tokens.size()-2);
				stream = new TokenStream(tokens);
				mergeSuggestions(suggest(rule, stream, input, matchWith), texts);
			}
		} else {
			mergeSuggestions(suggest(rule, stream, input, ""), texts);
		}
		return texts;
	}
	
	private void mergeSuggestions(List<CaretAwareText> from, List<CaretAwareText> to) {
		Set<String> includedContents = new HashSet<>();
		for (CaretAwareText text: to)
			includedContents.add(text.getContent());
		for (CaretAwareText text: from) {
			if (!includedContents.contains(text.getContent())) {
				includedContents.add(text.getContent());
				to.add(text);
			}
		}
	}
	
	private List<CaretAwareText> suggest(RuleSpec spec, TokenStream stream, 
			CaretAwareText input, String matchWith) {
		List<CaretAwareText> texts = new ArrayList<>();
		
		List<ElementSuggestion> suggestions = new ArrayList<>();
		if (stream.isEof()) {
			suggestions.addAll(spec.suggestFirst(null, matchWith, stream));
		} else {
			List<TokenNode> matches = spec.getPartialMatches(stream, null);
			if (!matches.isEmpty() && stream.isEof()) {
				for (TokenNode match: matches) {
					ElementSpec matchSpec = (ElementSpec) match.getSpec();
					suggestions.addAll(matchSpec.suggestNext(match.getParent(), matchWith, stream));
				}
			}
		}

		int replaceStart = input.getCaret() - matchWith.length();
		for (ElementSuggestion suggestion: suggestions) {
			int replaceEnd = input.getCaret();
			String contentAfterReplaceStart = input.getContent().substring(replaceStart);
			TokenStream streamAfterReplaceStart = lex(contentAfterReplaceStart);
			if (!streamAfterReplaceStart.isEof() && streamAfterReplaceStart.getToken(0).getStartIndex() == 0) {
				ElementSpec elementSpec = (ElementSpec) suggestion.getNode().getSpec();
				if (elementSpec.matchOnce(streamAfterReplaceStart)) {
					if (streamAfterReplaceStart.getIndex() != 0)
						replaceEnd = replaceStart + streamAfterReplaceStart.getPreviousToken().getStopIndex()+1;
				} else if (elementSpec instanceof TokenElementSpec) {
					List<String> tokenMandatories = elementSpec.getMandatories();
					if (!tokenMandatories.isEmpty()) {
						String toBeReplaced = tokenMandatories.get(0);
						if (contentAfterReplaceStart.startsWith(toBeReplaced))
							replaceEnd = replaceStart + toBeReplaced.length();
					}
				}
			}
			
			List<String> mandatories;
			if (input.getCaret() == input.getContent().length() 
					|| Character.isWhitespace(input.getContent().charAt(input.getCaret()))) {
				mandatories = getMandatoriesAfter(suggestion.getNode());
			} else {
				mandatories = new ArrayList<>();
			}
			
			String before = input.getContent().substring(0, replaceStart);
			String after = input.getContent().substring(replaceEnd);

			for (CaretAwareText text: suggestion.getTexts()) {
				String newContent;
				if (stream.size() > 1) { 
					newContent = before + text.getContent();
					TokenStream newStream = lex(newContent);
					if (newStream.size() >= stream.size()) {
						Token lastToken = stream.getToken(stream.size()-2);
						Token newToken = newStream.getToken(stream.size()-2);
						if (lastToken.getStartIndex() != newToken.getStartIndex()
								|| lastToken.getStopIndex() != newToken.getStopIndex()) {
							newContent = before + " " + text.getContent();
						}
					} else {
						newContent = before + " " + text.getContent();
					}
				} else {
					newContent = before + text.getContent();
				}
				int caret = text.getCaret();
				if (caret == text.getContent().length())
					caret = newContent.length();
				else
					caret += before.length() + 1;

				for (String mandatory: mandatories) {
					String prevNewContent = newContent;
					newContent += mandatory;
					if (tokenTypesByLiteral.containsKey(mandatory)) {
						TokenStream newStream = lex(newContent);
						if (!newStream.isEof()) {
							Token lastToken = newStream.getToken(newStream.size()-2);
							if (lastToken.getStartIndex() != newContent.length() - mandatory.length()
									|| lastToken.getStopIndex() != newContent.length()-1) {
								newContent = prevNewContent + " " + mandatory;
							}
						} else {
							newContent = prevNewContent + " " + mandatory;
						}
					}
				}
				
				newContent += after;
				
				if (text.getCaret() == text.getContent().length() 
						&& caret < newContent.length() 
						&& !Character.isWhitespace(newContent.charAt(caret))) {
					caret += skipMandatoriesAfter(suggestion.getNode(), newContent.substring(caret), 0);
				}
				
				texts.add(new CaretAwareText(newContent, caret));
			}
		}
		return texts;
	}
	
	private List<String> getMandatoriesAfter(Node elementNode) {
		List<String> mandatories = new ArrayList<>();
		if (elementNode.getParent().getStart() == null) {
			ElementSpec elementSpec = (ElementSpec) elementNode.getSpec();
			AlternativeSpec alternativeSpec = (AlternativeSpec) elementNode.getParent().getSpec();
			int specIndex = alternativeSpec.getElements().indexOf(elementSpec);
			if (specIndex == alternativeSpec.getElements().size()-1) {
				elementNode = elementNode.getParent().getParent().getParent();
				if (elementNode != null)
					mandatories.addAll(getMandatoriesAfter(elementNode));
			} else {
				elementSpec = alternativeSpec.getElements().get(specIndex+1);
				if (elementSpec.getMultiplicity() == Multiplicity.ONE_OR_MORE 
						|| elementSpec.getMultiplicity() == Multiplicity.ONE) {
					mandatories.addAll(elementSpec.getMandatories());
				}
				elementNode = new Node(elementSpec, elementNode.getParent());
				mandatories.addAll(getMandatoriesAfter(elementNode));
			}
		}
		return mandatories;
	}
	
	private int skipMandatoriesAfter(Node elementNode, String content, int offset) {
		ElementSpec elementSpec = (ElementSpec) elementNode.getSpec();
		if (elementSpec.getMultiplicity() == Multiplicity.ONE 
				|| elementSpec.getMultiplicity() == Multiplicity.ZERO_OR_ONE) {
			AlternativeSpec alternativeSpec = (AlternativeSpec) elementNode.getParent().getSpec();
			int specIndex = alternativeSpec.getElements().indexOf(elementSpec);
			if (specIndex == alternativeSpec.getElements().size()-1) {
				elementNode = elementNode.getParent().getParent().getParent();
				if (elementNode != null)
					return skipMandatoriesAfter(elementNode, content, offset);
			} else {
				elementSpec = alternativeSpec.getElements().get(specIndex+1);
				if (elementSpec.getMultiplicity() == Multiplicity.ONE
						|| elementSpec.getMultiplicity() == Multiplicity.ONE_OR_MORE) {
					CaretMove move = elementSpec.skipMandatories(content, offset);
					offset = move.getOffset();
					if (!move.isStop()) {
						elementNode = new Node(elementSpec, elementNode.getParent());
						return skipMandatoriesAfter(elementNode, content, offset);
					}
				}
			}
		}
		return offset;
	}
	
	protected abstract List<CaretAwareText> suggest(ElementSpec spec, Node parent, 
			String matchWith, TokenStream stream);
	
}
