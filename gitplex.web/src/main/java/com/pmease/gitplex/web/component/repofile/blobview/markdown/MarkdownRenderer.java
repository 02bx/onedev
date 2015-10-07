package com.pmease.gitplex.web.component.repofile.blobview.markdown;

import com.pmease.commons.git.Blob;
import com.pmease.gitplex.web.component.repofile.blobview.BlobRenderer;
import com.pmease.gitplex.web.component.repofile.blobview.BlobViewContext;
import com.pmease.gitplex.web.component.repofile.blobview.BlobViewPanel;
import com.pmease.gitplex.web.component.repofile.blobview.BlobViewContext.Mode;
import com.pmease.gitplex.web.component.repofile.blobview.source.SourceViewPanel;

public class MarkdownRenderer implements BlobRenderer {

	@Override
	public BlobViewPanel render(String panelId, BlobViewContext context, String clientState) {
		Blob blob = context.getRepository().getBlob(context.getBlobIdent());
		if (context.getBlobIdent().isFile() 
				&& blob.getText() != null 
				&& context.getBlobIdent().path.endsWith(".md")) { 
			if (context.getMark() != null || context.getMode() == Mode.BLAME 
					|| context.getComment() != null && context.getComment().getBlobIdent().equals(context.getBlobIdent()))
				return new SourceViewPanel(panelId, context, clientState);
			else
				return new MarkdownFilePanel(panelId, context);
		} else {
			return null;
		}
	}

}
