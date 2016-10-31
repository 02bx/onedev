package com.gitplex.web.component;

import java.util.Collection;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.gitplex.core.GitPlex;
import com.gitplex.core.entity.Depot;
import com.gitplex.web.component.depotselector.DepotSelector;
import com.gitplex.commons.hibernate.dao.Dao;
import com.gitplex.commons.wicket.component.DropdownLink;

@SuppressWarnings("serial")
public abstract class DepotPicker extends DropdownLink {

	private final IModel<Collection<Depot>> depotsModel; 
	
	private Long currentDepotId;
	
	public DepotPicker(String id, IModel<Collection<Depot>> depotsModel, Long currentDepotId) {
		super(id);
	
		this.depotsModel = depotsModel;
		this.currentDepotId = currentDepotId;
	}

	@Override
	protected Component newContent(String id) {
		return new DepotSelector(id, depotsModel, currentDepotId) {

			@Override
			protected void onSelect(AjaxRequestTarget target, Depot depot) {
				close();
				target.add(DepotPicker.this);
				DepotPicker.this.onSelect(target, depot);
			}

		};
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		setEscapeModelStrings(false);
	}

	@Override
	public IModel<?> getBody() {
		Depot currentDepot = GitPlex.getInstance(Dao.class).load(Depot.class, currentDepotId);
		return Model.of(String.format("<i class='fa fa-ext fa-repo'></i> <span>%s</span> <i class='fa fa-caret-down'></i>", currentDepot.getFQN()));
	}

	@Override
	protected void onDetach() {
		depotsModel.detach();
		super.onDetach();
	}

	protected abstract void onSelect(AjaxRequestTarget target, Depot depot);
}
