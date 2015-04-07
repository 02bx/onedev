package com.pmease.commons.wicket.assets;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.pmease.commons.util.StringUtils;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;

public class CodeMirrorResourceReference extends WebjarsJavaScriptResourceReference {

	private static final long serialVersionUID = 1L;

	public static final CodeMirrorResourceReference INSTANCE = new CodeMirrorResourceReference();
	
	private CodeMirrorResourceReference() {
		super("codemirror/current/mode/meta.js");
	}

	@Override
	public Iterable<? extends HeaderItem> getDependencies() {
		String modeBase = StringUtils.substringBeforeLast(RequestCycle.get().urlFor(this, new PageParameters()).toString(), "/");
		return Iterables.concat(super.getDependencies(), ImmutableList.<HeaderItem>of(
					JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("codemirror/current/lib/codemirror.js")),
					JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("codemirror/current/addon/mode/loadmode.js")),
					JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("codemirror/current/addon/edit/matchbrackets.js")),
					JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("codemirror/current/addon/fold/foldcode.js")),
					JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("codemirror/current/addon/fold/foldgutter.js")),
					JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("codemirror/current/addon/fold/brace-fold.js")),
					JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("codemirror/current/addon/fold/xml-fold.js")),
					JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("codemirror/current/addon/fold/markdown-fold.js")),
					JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference("codemirror/current/addon/fold/comment-fold.js")),
					CssHeaderItem.forReference(new WebjarsCssResourceReference("codemirror/current/lib/codemirror.css")),
					CssHeaderItem.forReference(new WebjarsCssResourceReference("codemirror/current/addon/fold/foldgutter.css")),
					CssHeaderItem.forReference(new WebjarsCssResourceReference("codemirror/current/theme/eclipse.css")),
					OnDomReadyHeaderItem.forScript("CodeMirror.modeURL = '" + modeBase + "/%N/%N.js';")
				));
	}

}
