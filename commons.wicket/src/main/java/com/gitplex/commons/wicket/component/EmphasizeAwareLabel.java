package com.gitplex.commons.wicket.component;

import javax.annotation.Nullable;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.LoadableDetachableModel;

import com.gitplex.commons.util.Range;
import com.gitplex.commons.util.StringUtils;

@SuppressWarnings("serial")
public class EmphasizeAwareLabel extends Label {

	public EmphasizeAwareLabel(String id, @Nullable String label, @Nullable Range emphasize) {
		super(id, new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				if (label != null) {
					if (emphasize != null) {
						String prefix = label.substring(0, emphasize.getFrom());
						String middle = label.substring(emphasize.getFrom(), emphasize.getTo());
						String suffix = label.substring(emphasize.getTo());
						return StringUtils.escapeHtml(prefix) 
								+ "<b>" 
								+ StringUtils.escapeHtml(middle) 
								+ "</b>" 
								+ StringUtils.escapeHtml(suffix);
					} else {
						return StringUtils.escapeHtml(label);
					}
				} else {
					return "";
				}
			}
			
		});
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		setEscapeModelStrings(false);
	}

}
