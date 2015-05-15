package com.pmease.commons.wicket.component.datatable;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.pmease.commons.editable.EditableUtils;
import com.pmease.commons.editable.DefaultPropertyDescriptor;
import com.pmease.commons.hibernate.AbstractEntity;
import com.pmease.commons.wicket.editable.PropertyContext;

public class EntityColumn<T extends AbstractEntity> implements IStyledColumn<T, String> {
	
	private static final long serialVersionUID = 1L;

	private final Class<T> entityType;
	
	private final String propertyName;
	
	private final boolean sortable;
	
	private String cssClass;
	
	public EntityColumn(Class<T> entityType, String propertyName, boolean sortable) {
		this.entityType = entityType;
		this.propertyName = propertyName;
		this.sortable = sortable;
	}

	@Override
	public Component getHeader(String componentId) {
		return new Label(componentId, 
				EditableUtils.getName(new DefaultPropertyDescriptor(entityType, propertyName).getPropertyGetter()));
	}
	
	@Override
	public String getSortProperty() {
		if (sortable)
			return propertyName;
		else
			return null;
	}

	@Override
	public boolean isSortable() {
		return sortable;
	}
	
	@Override
	public void detach() {
	}

	@Override
	public void populateItem(Item<ICellPopulator<T>> cellItem,
			String componentId, IModel<T> rowModel) {
		cellItem.add(PropertyContext.viewBean(componentId, rowModel.getObject(), propertyName));
	}

	@Override
	public String getCssClass() {
		return cssClass;
	}

	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

}
