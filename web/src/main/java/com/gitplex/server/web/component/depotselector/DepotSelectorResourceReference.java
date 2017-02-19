package com.gitplex.server.web.component.depotselector;

import java.util.List;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.CssResourceReference;

import com.gitplex.server.web.assets.hotkeys.HotkeysResourceReference;
import com.gitplex.server.web.assets.scrollintoview.ScrollIntoViewResourceReference;
import com.gitplex.server.web.page.base.BaseDependentResourceReference;

public class DepotSelectorResourceReference extends BaseDependentResourceReference {

	private static final long serialVersionUID = 1L;

	public DepotSelectorResourceReference() {
		super(DepotSelectorResourceReference.class, "depot-selector.js");
	}

	@Override
	public List<HeaderItem> getDependencies() {
		List<HeaderItem> dependencies = super.getDependencies();
		dependencies.add(JavaScriptHeaderItem.forReference(new HotkeysResourceReference()));
		dependencies.add(JavaScriptHeaderItem.forReference(new ScrollIntoViewResourceReference()));
		dependencies.add(CssHeaderItem.forReference(new CssResourceReference(DepotSelectorResourceReference.class, "depot-selector.css")));
		return dependencies;
	}

}
