package com.pmease.gitop.web.assets;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;

import com.pmease.commons.wicket.asset.JQueryHeaderItem;
import com.pmease.commons.wicket.asset.bootstrap.BootstrapHeaderItem;

public class BaseResourceBehavior extends Behavior {
	private static final long serialVersionUID = 1L;

//	static final ResourceReference MODERNIZR_JS = new JavaScriptResourceReference(BaseResourceBehavior.class, "js/vendor/modernizr-2.6.2.js");
//	static final ResourceReference PAGE_JS = new JavaScriptResourceReference(BaseResourceBehavior.class, "js/page.js");
//	
//	static final ResourceReference FONT_AWESOME_CSS = new CssResourceReference(BaseResourceBehavior.class, "css/font-awesome.css");
//	static final ResourceReference FONT_AWESOME_MIN_CSS = new CssResourceReference(BaseResourceBehavior.class, "css/font-awesome.min.css");
//	static final ResourceReference BASE_CSS = new CssResourceReference(BaseResourceBehavior.class, "css/base.css");
//	static final ResourceReference PAGE_CSS = new CssResourceReference(BaseResourceBehavior.class, "css/page.css");
	
	@Override
	public void renderHead(Component component, IHeaderResponse response) {
		super.renderHead(component, response);
		
		response.render(JavaScriptReferenceHeaderItem.forReference(AssetLocator.MODERNIZR_JS));
		
		// render jquery
		response.render(JQueryHeaderItem.get());

		// render bootstrap
		response.render(BootstrapHeaderItem.get());
		
		response.render(JavaScriptReferenceHeaderItem.forReference(AssetLocator.PAGE_JS));
		
		// render font-awesome
		if (Application.get().getConfigurationType() == RuntimeConfigurationType.DEPLOYMENT) {
			response.render(CssReferenceHeaderItem.forReference(AssetLocator.FONT_AWESOME_MIN_CSS));
		} else {
			response.render(CssReferenceHeaderItem.forReference(AssetLocator.FONT_AWESOME_CSS));
		}
		
		response.render(CssReferenceHeaderItem.forReference(AssetLocator.BASE_CSS));
		response.render(CssReferenceHeaderItem.forReference(AssetLocator.PAGE_CSS));
	}
}
