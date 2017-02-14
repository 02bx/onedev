package com.gitplex.server.web.component.depotfile.blobview;

import javax.annotation.Nullable;

import com.gitplex.calla.loader.ExtensionPoint;

@ExtensionPoint
public interface BlobRenderer {

	@Nullable BlobViewPanel render(String panelId, BlobViewContext context);
	
}
