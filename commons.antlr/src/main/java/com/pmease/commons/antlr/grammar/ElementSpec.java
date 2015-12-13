package com.pmease.commons.antlr.grammar;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.Token;

import com.pmease.commons.antlr.codeassist.MandatoryScan;

public abstract class ElementSpec implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public enum Multiplicity{ONE, ZERO_OR_ONE, ZERO_OR_MORE, ONE_OR_MORE};
	
	private final String label;
	
	private final Multiplicity multiplicity;
	
	public ElementSpec(String label, Multiplicity multiplicity) {
		this.label = label;
		this.multiplicity = multiplicity;
	}

	public String getLabel() {
		return label;
	}

	public Multiplicity getMultiplicity() {
		return multiplicity;
	}
	
	public boolean isOptional() {
		return multiplicity == Multiplicity.ZERO_OR_MORE || multiplicity == Multiplicity.ZERO_OR_ONE;
	}

	public abstract Set<String> getLeadingLiterals(Set<String> checkedRules);
	
	protected abstract boolean matchesEmptyOnce(Set<String> checkedRules);
	
	public boolean matchesEmpty(Set<String> checkedRules) {
		if (getMultiplicity() == Multiplicity.ZERO_OR_MORE || getMultiplicity() == Multiplicity.ZERO_OR_ONE)
			return true;
		else
			return matchesEmptyOnce(checkedRules);
	}
	
	public abstract MandatoryScan scanMandatories(Set<String> checkedRules);
	
	public final String toString() {
		if (multiplicity == Multiplicity.ONE)
			return toStringOnce();
		else if (multiplicity == Multiplicity.ONE_OR_MORE)
			return toStringOnce() + "+";
		else if (multiplicity == Multiplicity.ZERO_OR_MORE)
			return toStringOnce() + "*";
		else
			return toStringOnce() + "?";
	}
	
	public abstract int getEndOfMatch(List<Token> tokens);
	
	protected abstract String toStringOnce();
}
