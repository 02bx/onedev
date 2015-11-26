package com.pmease.commons.wicket.behavior.inputassist;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import com.pmease.commons.antlr.AntlrUtils;
import com.pmease.commons.antlr.codeassist.AssistStream;
import com.pmease.commons.antlr.codeassist.CodeAssist;
import com.pmease.commons.antlr.codeassist.InputStatus;
import com.pmease.commons.antlr.codeassist.InputSuggestion;
import com.pmease.commons.antlr.codeassist.Node;
import com.pmease.commons.antlr.codeassist.ParseTree;
import com.pmease.commons.util.StringUtils;

@SuppressWarnings("serial")
public abstract class ANTLRAssistBehavior extends InputAssistBehavior {

	private final Class<? extends Lexer> lexerClass;
	
	private final Class<? extends Parser> parserClass;
	
	private transient Constructor<? extends Lexer> lexerCtor;
	
	private transient Constructor<? extends Parser> parserCtor;
	
	private transient Method ruleMethod;
	
	private final CodeAssist codeAssist;
	
	private final String ruleName;
	
	public ANTLRAssistBehavior(Class<? extends Parser> parserClass, String ruleName) {
		this(parserClass, AntlrUtils.getLexerClass(parserClass), ruleName);
	}
	
	public ANTLRAssistBehavior(Class<? extends Parser> parserClass, 
			Class<? extends Lexer> lexerClass, String ruleName) {
		this(parserClass, lexerClass, 
				new String[]{AntlrUtils.getDefaultGrammarFile(lexerClass)}, 
				AntlrUtils.getDefaultTokenFile(lexerClass), ruleName);
	}
	
	public ANTLRAssistBehavior(Class<? extends Parser> parserClass, Class<? extends Lexer> lexerClass, 
			String grammarFiles[], String tokenFile, String ruleName) {
		this.lexerClass = lexerClass;
		this.parserClass = parserClass;
		
		codeAssist = new CodeAssist(lexerClass, grammarFiles, tokenFile) {

			@Override
			protected List<InputSuggestion> suggest(ParseTree parseTree, Node elementNode, String matchWith) {
				return ANTLRAssistBehavior.this.suggest(parseTree, elementNode, matchWith);
			}
			
		};
		this.ruleName = ruleName;
	}
	
	@Override
	protected List<InputSuggestion> getSuggestions(InputStatus inputStatus) {
		return codeAssist.suggest(inputStatus, ruleName);
	}
	
	private Constructor<? extends Lexer> getLexerCtor() {
		if (lexerCtor == null) {
			try {
				lexerCtor = lexerClass.getConstructor(CharStream.class);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
			
		}
		return lexerCtor;
	}

	private Constructor<? extends Parser> getParserCtor() {
		if (parserCtor == null) {
			try {
				parserCtor = parserClass.getConstructor(TokenStream.class);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
			
		}
		return parserCtor;
	}
	
	private Method getRuleMethod() {
		if (ruleMethod == null) {
			try {
				ruleMethod = parserClass.getDeclaredMethod(ruleName);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}
		return ruleMethod;
	}
	
	@Override
	protected List<InputError> getErrors(final String inputContent) {
		final List<InputError> errors = new ArrayList<>();
		
		Lexer lexer;
		try {
			lexer = getLexerCtor().newInstance(new ANTLRInputStream(inputContent));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		lexer.removeErrorListeners();
		lexer.addErrorListener(new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				int charIndex = getCharIndex(inputContent, line-1, charPositionInLine);
				errors.add(new InputError(charIndex, recognizer.getInputStream().index()));
			}
			
		});
		
		Parser parser;
		try {
			parser = getParserCtor().newInstance(new CommonTokenStream(lexer));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		parser.removeErrorListeners();
		parser.addErrorListener(new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				int fromIndex = getCharIndex(inputContent, line-1, charPositionInLine);
				CommonToken token = (CommonToken) offendingSymbol;
				int toIndex;
				if (token != null && token.getType() != Token.EOF)
					toIndex = fromIndex + token.getText().length() - 1;
				else
					toIndex = fromIndex;
				errors.add(new InputError(fromIndex, toIndex));
			}
			
		});
		try {
			getRuleMethod().invoke(parser);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return errors;
	}
	
	@Override
	protected int getAnchor(String content) {
		AssistStream stream = codeAssist.lex(content);
		if (stream.isEof()) {
			return 0;
		} else {
			Token lastToken = stream.getToken(stream.size()-2);
			String contentAfterLastToken = content.substring(lastToken.getStopIndex()+1);
			if (contentAfterLastToken.length() > 0) {
				contentAfterLastToken = StringUtils.trimStart(contentAfterLastToken);
				return content.length() - contentAfterLastToken.length();
			} else {
				return lastToken.getStartIndex();
			}
		}
	}

	protected abstract List<InputSuggestion> suggest(ParseTree parseTree, Node elementNode, String matchWith);
	
}
