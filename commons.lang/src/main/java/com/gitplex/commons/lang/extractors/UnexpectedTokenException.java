package com.gitplex.commons.lang.extractors;


@SuppressWarnings("serial")
public class UnexpectedTokenException extends ExtractException {

	public UnexpectedTokenException(ExtractToken token) {
		super("Unexpected token: " + token);
	}

}
