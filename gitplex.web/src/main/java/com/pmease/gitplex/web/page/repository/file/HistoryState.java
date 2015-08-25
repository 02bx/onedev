package com.pmease.gitplex.web.page.repository.file;

import java.io.Serializable;

import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.lang.extractors.TokenPosition;

public class HistoryState implements Serializable {

	private static final long serialVersionUID = 1L;

	public Long requestId;
	
	public Long commentId;
	
	public BlobIdent file = new BlobIdent();
	
	public TokenPosition tokenPos;
	
	public boolean blame;
	
	public HistoryState() {
	}

	public HistoryState(HistoryState state) {
		file = new BlobIdent(state.file);
		tokenPos = state.tokenPos;
		blame = state.blame;
		requestId = state.requestId;
		commentId = state.commentId;
	}
	
}