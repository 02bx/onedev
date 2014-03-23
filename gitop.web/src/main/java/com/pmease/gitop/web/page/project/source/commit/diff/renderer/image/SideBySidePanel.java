package com.pmease.gitop.web.page.project.source.commit.diff.renderer.image;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;

import com.google.common.base.Strings;
import com.pmease.gitop.model.Repository;
import com.pmease.gitop.web.page.project.source.blob.renderer.FileBlobImage;
import com.pmease.gitop.web.page.project.source.commit.diff.patch.FileHeader;

@SuppressWarnings("serial")
public class SideBySidePanel extends AbstractImageDiffPanel {
	
	public SideBySidePanel(String id, IModel<FileHeader> model,
			IModel<Repository> projectModel, IModel<String> sinceModel,
			IModel<String> untilModel) {
		super(id, model, projectModel, sinceModel, untilModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		FileHeader file = getFile();
		String until = getUntil();
		String since = getSince();
		
		if (file.getChangeType() == ChangeType.ADD || Strings.isNullOrEmpty(since)) {
			add(new WebMarkupContainer("old").setVisibilityAllowed(false));
		} else {
			add(new FileBlobImage("old", 
					getProject(), 
					since,
					file.getOldPath()));
		}
		
		if (file.getChangeType() == ChangeType.DELETE) {
			add(new WebMarkupContainer("new").setVisibilityAllowed(false));
		} else {
			add(new FileBlobImage("new",
					getProject(),
					until,
					file.getNewPath()));
		}
	}
}
