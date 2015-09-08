package com.pmease.gitplex.web.page.repository.file;

import java.io.Serializable;

import com.pmease.commons.git.BlobIdent;
import com.pmease.gitplex.web.component.repofile.blobview.BlobViewContext.Mode;

class HistoryState implements Serializable {

	private static final long serialVersionUID = 1L;

	public Long requestId;
	
	public Long commentId;
	
	public BlobIdent blobIdent = new BlobIdent();
	
	public Highlight highlight;
	
	public Mode mode;
	
}