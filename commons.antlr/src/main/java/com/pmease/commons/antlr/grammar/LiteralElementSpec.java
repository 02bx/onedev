package com.pmease.commons.antlr.grammar;

import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pmease.commons.antlr.codeassist.MandatoryScan;

public class LiteralElementSpec extends TokenElementSpec {

	private static final long serialVersionUID = 1L;
	
	private final String literal;
	
	public LiteralElementSpec(String label, Multiplicity multiplicity, 
			int tokenType, String literal) {
		super(label, multiplicity, tokenType);
		
		this.literal = literal;
	}

	public String getLiteral() {
		return literal;
	}

	@Override
	public MandatoryScan scanMandatories() {
		return new MandatoryScan(Lists.newArrayList(literal), false);
	}

	@Override
	protected String toStringOnce() {
		return "'" + literal + "'";
	}

	@Override
	public Set<String> getLeadingChoices() {
		return Sets.newHashSet(literal);
	}

	@Override
	protected boolean isAllowEmptyOnce() {
		return false;
	}
	
}
