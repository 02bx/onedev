package com.pmease.commons.antlr.codeassist;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;

import com.google.common.base.Preconditions;

public class AssistStream {
	
	private final List<Token> tokens;
	
	private int index;

	/**
	 * Construct assist stream with a list of tokens.
	 * 
	 * @param tokens
	 * 			tokens to be used to construct the assist stream
	 */
	public AssistStream(List<Token> tokens) {
		this.tokens = tokens;
	}

	public List<Token> getTokens() {
		return tokens;
	}

	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		Preconditions.checkArgument(index>=0 && index<=size());
		this.index = index;
	}
	
	public void increaseIndex() {
		Preconditions.checkState(!isEof());
		index++;
	}

	public int indexOf(Token token) {
		return tokens.indexOf(token);
	}

	public int size() {
		return tokens.size();
	}
	
	public Token getCurrentToken() {
		return getToken(index);
	}
	
	public Token getNextToken() {
		return getToken(index+1);
	}
	
	public Token getToken(int index) {
		Preconditions.checkArgument(index>=0 && index<size());
		return tokens.get(index);
	}
	
	public int getTokenIndex(Token token) {
		return tokens.indexOf(token);
	}
	
	public Token getPreviousToken() {
		return getToken(index-1);
	}

	public boolean isEof() {
		return index == size();
	}
	
	public static Token SOF = new Token() {

		@Override
		public String getText() {
			return "SOF";
		}

		@Override
		public int getType() {
			return -1;
		}

		@Override
		public int getLine() {
			return -1;
		}

		@Override
		public int getCharPositionInLine() {
			return -1;
		}

		@Override
		public int getChannel() {
			return 0;
		}

		@Override
		public int getTokenIndex() {
			return -1;
		}

		@Override
		public int getStartIndex() {
			return -1;
		}

		@Override
		public int getStopIndex() {
			return -1;
		}

		@Override
		public TokenSource getTokenSource() {
			return null;
		}

		@Override
		public CharStream getInputStream() {
			return null;
		}
		
	};
	
	public static Token EOF = new Token() {

		@Override
		public String getText() {
			return "EOF";
		}

		@Override
		public int getType() {
			return -1;
		}

		@Override
		public int getLine() {
			return -1;
		}

		@Override
		public int getCharPositionInLine() {
			return -1;
		}

		@Override
		public int getChannel() {
			return 0;
		}

		@Override
		public int getTokenIndex() {
			return -1;
		}

		@Override
		public int getStartIndex() {
			return -1;
		}

		@Override
		public int getStopIndex() {
			return -1;
		}

		@Override
		public TokenSource getTokenSource() {
			return null;
		}

		@Override
		public CharStream getInputStream() {
			return null;
		}
		
	};	
}
 