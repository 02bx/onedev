package com.turbodev.server.util.diff;

import java.io.Serializable;
import java.util.List;

import com.turbodev.jsyntax.TextToken;

public class LineDiff implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int compareLine;
	
	private final List<DiffBlock<TextToken>> tokenDiffs;
	
	public LineDiff(int compareLine, List<DiffBlock<TextToken>> tokenDiffs) {
		this.compareLine = compareLine;
		this.tokenDiffs = tokenDiffs;
	}

	public int getCompareLine() {
		return compareLine;
	}

	public List<DiffBlock<TextToken>> getTokenDiffs() {
		return tokenDiffs;
	}
	
}
