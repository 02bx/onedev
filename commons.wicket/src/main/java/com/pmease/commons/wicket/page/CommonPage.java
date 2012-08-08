package com.pmease.commons.wicket.page;

import org.apache.wicket.bootstrap.Bootstrap;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.pmease.commons.wicket.asset.Asset;

public class CommonPage extends WebPage {

	private static final long serialVersionUID = 1L;

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		Bootstrap.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(new PackageResourceReference(Asset.class, "javascript/common.js")));
		response.render(CssReferenceHeaderItem.forReference(new PackageResourceReference(Asset.class, "css/common.css")));
	}

}
