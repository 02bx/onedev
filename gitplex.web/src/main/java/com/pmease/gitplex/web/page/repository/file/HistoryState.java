package com.pmease.gitplex.web.page.repository.file;

import java.io.Serializable;

import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.lang.extractors.TokenPosition;

class HistoryState implements Serializable {

	private static final long serialVersionUID = 1L;

	public Long requestId;
	
	public Long commentId;
	
	public BlobIdent file = new BlobIdent();
	
	public TokenPosition tokenPos;
	
	public boolean blame;
	
}