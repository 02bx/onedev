package com.pmease.commons.wicket.behavior.tooltip;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.pmease.commons.wicket.behavior.dropdown.DropdownAlignment;
import com.pmease.commons.wicket.behavior.dropdown.DropdownAlignment.INDICATOR_MODE;
import com.pmease.commons.wicket.behavior.dropdown.DropdownBehavior;

public class TooltipBehavior extends Behavior {

	private static final long serialVersionUID = 1L;

	private final IModel<String> contentModel;
	
	private DropdownAlignment alignment = new DropdownAlignment(50, 0, 50, 100).setIndicatorMode(INDICATOR_MODE.SHOW);
	
	public TooltipBehavior(IModel<String> contentModel) {
		this.contentModel = contentModel;
	}
	
	public void bind(Component component) {
		super.bind(component);
		component.setOutputMarkupId(true);
	}

	/**
	 * Specify how the dropdown panel is aligned to the target. 
	 * @param alignment
	 * 			The {@link DropdownAlignment alignment} setting object. 
	 * @return
	 * 			This behavior.
	 */
	public TooltipBehavior setAlignment(DropdownAlignment alignment) {
		this.alignment = alignment;
		if (alignment.getTarget() != null)
			alignment.getTarget().setOutputMarkupId(true);
		return this;
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response) {
		super.renderHead(component, response);
		response.render(CssHeaderItem.forReference(new PackageResourceReference(DropdownBehavior.class, "dropdown.css")));
		response.render(JavaScriptHeaderItem.forReference(new PackageResourceReference(DropdownBehavior.class, "dropdown.js")));
		
		String escapedContent = StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(contentModel.getObject()));
		String script = String.format(
				"setupDropdown('%s', '<div class=\\'tooltip\\' id=\\'%s-dropdown\\'><div class=\\'content\\'>%s</div></div>', 0, '%s', '%s', %s, %s, %s, %s, %d, undefined)", 
				component.getMarkupId(), component.getMarkupId(), escapedContent, alignment.getIndicatorMode().name(), 
				alignment.getTarget()!=null?alignment.getTarget().getMarkupId():component.getMarkupId(), 
				alignment.getTargetX(), alignment.getTargetY(), alignment.getDropdownX(), alignment.getDropdownY(), 
				alignment.getGap());
		response.render(OnDomReadyHeaderItem.forScript(script));
		
		response.render(CssHeaderItem.forReference(new PackageResourceReference(TooltipBehavior.class, "tooltip.css")));
	}

	@Override
	public void detach(Component component) {
		contentModel.detach();
		super.detach(component);
	}

}
