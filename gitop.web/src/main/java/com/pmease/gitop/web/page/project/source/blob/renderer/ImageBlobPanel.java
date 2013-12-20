package com.pmease.gitop.web.page.project.source.blob.renderer;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.pmease.gitop.web.service.FileBlob;

@SuppressWarnings("serial")
public class ImageBlobPanel extends Panel {

	public ImageBlobPanel(String id, IModel<FileBlob> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		FileBlob blob = getBlob();
		add(new FileBlobImage("image", FileBlobImage.newParams(blob)));
	}
	
	private FileBlob getBlob() {
		return (FileBlob) getDefaultModelObject();
	}
}
