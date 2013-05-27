package com.pmease.commons.wicket.asset;

import org.apache.wicket.ajax.WicketAjaxJQueryResourceReference;
import org.apache.wicket.ajax.WicketEventJQueryResourceReference;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

/**
 * A stake holder class for asset positioning.
 * @author robin
 *
 */
@SuppressWarnings("serial")
public class Asset extends WebMarkupContainer {

	public Asset(String id) {
		super(id);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(WicketEventJQueryResourceReference.get()));
		response.render(JavaScriptHeaderItem.forReference(WicketAjaxJQueryResourceReference.get()));
		response.render(CssHeaderItem.forReference(new CssResourceReference(Asset.class, "bootstrap/css/bootstrap.min.css")));
		response.render(CssHeaderItem.forReference(new CssResourceReference(Asset.class, "bootstrap/css/bootstrap-responsive.min.css")));
		response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(Asset.class, "bootstrap/js/bootstrap.min.js")));
		
		response.render(CssReferenceHeaderItem.forReference(new CssResourceReference(Asset.class, "css/common.css")));
		response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(Asset.class, "js/common.js")));
		response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(Asset.class, "js/jquery.align.js")));
	}

}
