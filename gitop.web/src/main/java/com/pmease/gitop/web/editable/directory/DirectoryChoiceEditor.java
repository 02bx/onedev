package com.pmease.gitop.web.editable.directory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.pmease.commons.editable.PropertyEditContext;
import com.pmease.commons.git.TreeNode;
import com.pmease.commons.wicket.behavior.dropdown.DropdownBehavior;
import com.pmease.commons.wicket.behavior.dropdown.DropdownPanel;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.BranchManager;
import com.pmease.gitop.model.Branch;
import com.pmease.gitop.web.component.directory.DirectoryChooser;
import com.pmease.gitop.web.page.project.AbstractProjectPage;

@SuppressWarnings("serial")
public class DirectoryChoiceEditor extends Panel {
	
	private final PropertyEditContext editContext;

	private final boolean append;
	
	public DirectoryChoiceEditor(String id, PropertyEditContext editContext, boolean append) {
		super(id);
		this.editContext = editContext;
		this.append = append;
		
		setDefaultModel(new LoadableDetachableModel<Branch>() {

			@Override
			protected Branch load() {
				AbstractProjectPage page = (AbstractProjectPage) getPage();
				return Gitop.getInstance(BranchManager.class).findDefault(page.getProject());
			}
			
		});
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		final TextField<String> input;
		
		add(input = new TextField<String>("input", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				return (String) editContext.getPropertyValue();
			}

			@Override
			public void setObject(String object) {
				editContext.setPropertyValue(object);
			}
			
		}));
		input.setOutputMarkupId(true);
		
		DropdownPanel chooser = new DropdownPanel("chooser") {

			@Override
			protected Component newContent(String id) {
				if (getBranch() != null) {
					return new DirectoryChooser(id, new AbstractReadOnlyModel<Branch>() {
	
						@Override
						public Branch getObject() {
							return getBranch();
						}
						
					}) {
	
						@Override
						protected MarkupContainer newLinkComponent(String id, IModel<TreeNode> node) {
							final String path = StringEscapeUtils.escapeEcmaScript(node.getObject().getPath());
							WebMarkupContainer link = new WebMarkupContainer(id) {

								@Override
								protected void onComponentTag(ComponentTag tag) {
									super.onComponentTag(tag);
									String script = String.format("gitop.selectDirectory('%s', '%s', '%s', %s);", 
											input.getMarkupId(), getMarkupId(), path, append);
									tag.put("onclick", script);
									tag.put("href", "javascript:void(0);");
								}
								
							};
							link.setOutputMarkupId(true);
							return link;
						}
						
					};
				} else {
					return new Fragment(id, "noDefaultBranchFrag", DirectoryChoiceEditor.this);
				}					
			}
		};
		add(chooser);
		add(new WebMarkupContainer("chooserTrigger").add(new DropdownBehavior(chooser)));
	}

	private Branch getBranch() {
		return (Branch) getDefaultModelObject();
	}
	
}