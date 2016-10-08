package com.pmease.gitplex.web.page.depot.setting.commitmessagetransform;

import java.io.Serializable;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.commons.wicket.editable.PropertyDescriptor;
import com.pmease.commons.wicket.editable.reflection.ReflectionPropertyEditor;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.core.entity.support.CommitMessageTransformSetting;
import com.pmease.gitplex.core.manager.DepotManager;
import com.pmease.gitplex.web.page.depot.setting.DepotSettingPage;

@SuppressWarnings("serial")
public class CommitMessageTransformPage extends DepotSettingPage {

	public CommitMessageTransformPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		Form<?> form = new Form<Void>("commitMessageTransformSetting") {

			@Override
			protected void onSubmit() {
				super.onSubmit();
				GitPlex.getInstance(DepotManager.class).save(getDepot());
				Session.get().success("Commit message transform setting is updated");
			}
			
		};
		form.add(new ReflectionPropertyEditor("editor", new PropertyDescriptor(Depot.class, "commitMessageTransformSetting"), new IModel<Serializable>() {

			@Override
			public void detach() {
			}

			@Override
			public Serializable getObject() {
				return getDepot().getCommitMessageTransformSetting();
			}

			@Override
			public void setObject(Serializable object) {
				getDepot().setCommitMessageTransformSetting((CommitMessageTransformSetting) object);
			}

		}));

		add(form);
	}
	
}
