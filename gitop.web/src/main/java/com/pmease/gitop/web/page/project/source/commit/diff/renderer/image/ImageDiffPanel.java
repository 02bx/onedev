package com.pmease.gitop.web.page.project.source.commit.diff.renderer.image;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.pmease.gitop.model.Project;
import com.pmease.gitop.web.page.project.source.commit.diff.patch.FileHeader;
import com.pmease.gitop.web.page.project.source.commit.diff.renderer.BlobDiffPanel;

@SuppressWarnings("serial")
public class ImageDiffPanel extends BlobDiffPanel {

	public ImageDiffPanel(String id, int index, 
			IModel<FileHeader> fileModel,
			IModel<Project> projectModel, 
			IModel<String> sinceModel,
			IModel<String> untilModel) {
		
		super(id, index, fileModel, projectModel, sinceModel, untilModel);
	}

	@Override
	protected Component createActionsBar(String id) {
		return new WebMarkupContainer(id).setVisibilityAllowed(false);
	}

	@Override
	protected Component createDiffContent(String id) {
		return new ImageBlobDiffPanel(id, getFileModel(), projectModel, sinceModel, untilModel);
	}
}
