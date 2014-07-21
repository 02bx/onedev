package com.pmease.gitplex.web.page.repository.source.blob.renderer;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.pmease.gitplex.web.service.FileBlob;

class RawBlobRenderer implements BlobRenderer {

	@Override
	public Panel render(String componentId, IModel<FileBlob> blob) {
		return new RawBlobPanel(componentId, blob);
	}

}
