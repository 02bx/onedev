package com.pmease.commons.antlr.codeassist.parse;

import java.util.List;

import org.junit.Test;

import com.pmease.commons.antlr.codeassist.AssistStream;
import com.pmease.commons.antlr.codeassist.CodeAssist;
import com.pmease.commons.antlr.codeassist.InputSuggestion;
import com.pmease.commons.antlr.codeassist.Node;
import com.pmease.commons.antlr.codeassist.ParseTree;
import com.pmease.commons.antlr.codeassist.RuleSpec;
import com.pmease.commons.antlr.codeassist.test.CodeAssistTest4Lexer;

public class EarleyParserTest {

	@Test
	public void test() {
		CodeAssist assist = new CodeAssist(CodeAssistTest4Lexer.class) {

			private static final long serialVersionUID = 1L;

			@Override
			protected List<InputSuggestion> suggest(ParseTree parseTree, Node elementNode, String matchWith) {
				return null;
			}
			
		};
		AssistStream stream = assist.lex("cd");
		RuleSpec rule = assist.getRule("a");
		System.out.println(new EarleyParser(rule, stream).getMatches().size());
	}

}
