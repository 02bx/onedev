package com.pmease.gitplex.web.component.depotfile.blobview.source;

import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;

import com.pmease.commons.wicket.assets.codemirror.CodeMirrorResourceReference;
import com.pmease.commons.wicket.assets.cookies.CookiesResourceReference;
import com.pmease.commons.wicket.assets.hover.HoverResourceReference;
import com.pmease.commons.wicket.assets.jqueryui.JQueryUIResourceReference;
import com.pmease.commons.wicket.assets.selectionpopover.SelectionPopoverResourceReference;
import com.pmease.gitplex.web.page.base.BaseDependentCssResourceReference;
import com.pmease.gitplex.web.page.base.BaseDependentResourceReference;

public class SourceViewResourceReference extends BaseDependentResourceReference {

	private static final long serialVersionUID = 1L;

	public SourceViewResourceReference() {
		super(SourceViewResourceReference.class, "source-view.js");
	}

	@Override
	public List<HeaderItem> getDependencies() {
		List<HeaderItem> dependencies = super.getDependencies();
		dependencies.add(JavaScriptHeaderItem.forReference(new JQueryUIResourceReference()));
		dependencies.add(JavaScriptHeaderItem.forReference(new SelectionPopoverResourceReference()));
		dependencies.add(JavaScriptHeaderItem.forReference(new HoverResourceReference()));
		
		dependencies.add(JavaScriptHeaderItem.forReference(new CookiesResourceReference()));
		dependencies.add(JavaScriptHeaderItem.forReference(new CodeMirrorResourceReference()));
		dependencies.add(CssHeaderItem.forReference(new BaseDependentCssResourceReference(SourceViewResourceReference.class, "source-view.css")));
		return dependencies;
	}

}
