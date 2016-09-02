package com.pmease.gitplex.web.component.depotfile.fileedit;

import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;

import com.pmease.commons.wicket.assets.codemirror.CodeMirrorResourceReference;
import com.pmease.commons.wicket.assets.diffmatchpatch.DiffMatchPatchResourceReference;
import com.pmease.gitplex.web.page.base.BaseDependentResourceReference;

public class FileEditResourceReference extends BaseDependentResourceReference {

	private static final long serialVersionUID = 1L;

	public FileEditResourceReference() {
		super(FileEditResourceReference.class, "file-edit.js");
	}

	@Override
	public List<HeaderItem> getDependencies() {
		List<HeaderItem> dependencies = super.getDependencies();
		dependencies.add(JavaScriptHeaderItem.forReference(new CodeMirrorResourceReference()));
		dependencies.add(JavaScriptHeaderItem.forReference(new DiffMatchPatchResourceReference()));
		dependencies.add(CssHeaderItem.forReference(new CssResourceReference(FileEditResourceReference.class, "file-edit.css")));
		return dependencies;
	}

}
