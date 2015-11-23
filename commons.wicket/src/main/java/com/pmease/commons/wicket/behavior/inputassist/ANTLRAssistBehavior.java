package com.pmease.commons.wicket.behavior.inputassist;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

import com.pmease.commons.antlr.AntlrUtils;
import com.pmease.commons.antlr.codeassist.InputSuggestion;
import com.pmease.commons.antlr.codeassist.CodeAssist;
import com.pmease.commons.antlr.codeassist.ElementSpec;
import com.pmease.commons.antlr.codeassist.InputStatus;
import com.pmease.commons.antlr.codeassist.Node;
import com.pmease.commons.antlr.codeassist.TokenStream;
import com.pmease.commons.util.StringUtils;

@SuppressWarnings("serial")
public abstract class ANTLRAssistBehavior extends InputAssistBehavior {

	private final Constructor<? extends Lexer> lexerCtor;
	
	private final Constructor<? extends Parser> parserCtor; 
	
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
		try {
			lexerCtor = lexerClass.getConstructor(CharStream.class);
			parserCtor = parserClass.getConstructor(CommonTokenStream.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		
		codeAssist = new CodeAssist(lexerClass, grammarFiles, tokenFile) {

			@Override
			protected List<InputSuggestion> suggest(ElementSpec spec, Node parent, String matchWith,
					TokenStream stream) {
				return ANTLRAssistBehavior.this.suggest(spec, parent, matchWith, stream);
			}
			
		};
		this.ruleName = ruleName;
	}
	
	@Override
	protected List<InputSuggestion> getSuggestions(InputStatus inputStatus) {
		return codeAssist.suggest(inputStatus, ruleName);
	}

	@Override
	protected List<InputError> getErrors(String inputContent) {
		final List<InputError> errors = new ArrayList<>();
		
		Lexer lexer;
		try {
			lexer = lexerCtor.newInstance(new ANTLRInputStream(inputContent));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		lexer.removeErrorListeners();
		lexer.addErrorListener(new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				errors.add(new InputError(charPositionInLine, recognizer.getInputStream().index()+1));
			}
			
		});
		
		Parser parser;
		try {
			parser = parserCtor.newInstance(new CommonTokenStream(lexer));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		parser.removeErrorListeners();
		parser.addErrorListener(new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				CommonToken token = (CommonToken) offendingSymbol;
				int end;
				if (token != null && token.getType() != Token.EOF)
					end = charPositionInLine + token.getText().length();
				else
					end = charPositionInLine + 1;
				errors.add(new InputError(charPositionInLine, end));
			}
			
		});
		
		return errors;
	}

	@Override
	protected int getAnchor(String content) {
		TokenStream stream = codeAssist.lex(content);
		if (stream.isEof()) {
			return content.length();
		} else {
			Token lastToken = stream.getToken(stream.size()-2);
			String contentAfterLastToken = content.substring(lastToken.getStopIndex()+1);
			contentAfterLastToken = StringUtils.trimStart(contentAfterLastToken);
			return content.length() - contentAfterLastToken.length();
		}
	}

	protected abstract List<InputSuggestion> suggest(ElementSpec spec, Node parent, 
			String matchWith, TokenStream stream);
}
