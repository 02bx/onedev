package com.pmease.commons.wicket.editable.list.polymorphic;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.convert.ConversionException;

import com.google.common.base.Preconditions;
import com.pmease.commons.editable.EditableUtils;
import com.pmease.commons.editable.PropertyDescriptor;
import com.pmease.commons.editable.annotation.Horizontal;
import com.pmease.commons.editable.annotation.Vertical;
import com.pmease.commons.loader.AppLoader;
import com.pmease.commons.loader.ImplementationRegistry;
import com.pmease.commons.util.ClassUtils;
import com.pmease.commons.wicket.behavior.sortable.SortBehavior;
import com.pmease.commons.wicket.behavior.sortable.SortPosition;
import com.pmease.commons.wicket.editable.BeanContext;
import com.pmease.commons.wicket.editable.BeanEditor;
import com.pmease.commons.wicket.editable.ErrorContext;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.commons.wicket.editable.PropertyEditor;

@SuppressWarnings("serial")
public class PolymorphicListPropertyEditor extends PropertyEditor<List<Serializable>> {

	private final List<Class<?>> implementations;
	
	private final boolean horizontal;
	
	public PolymorphicListPropertyEditor(String id, PropertyDescriptor propertyDescriptor, IModel<List<Serializable>> model) {
		super(id, propertyDescriptor, model);
		
		implementations = new ArrayList<>();
		Class<?> baseClass = EditableUtils.getElementClass(propertyDescriptor.getPropertyGetter().getGenericReturnType());
		Preconditions.checkNotNull(baseClass);
		
		ImplementationRegistry registry = AppLoader.getInstance(ImplementationRegistry.class);
		implementations.addAll(registry.getImplementations(baseClass));

		Preconditions.checkArgument(
				!implementations.isEmpty(), 
				"Can not find implementations for '" + baseClass + "'.");
		
		EditableUtils.sortAnnotatedElements(implementations);

		Method propertyGetter = propertyDescriptor.getPropertyGetter();
		if (propertyGetter.getAnnotation(Horizontal.class) != null)
			horizontal = true;
		else if (propertyGetter.getAnnotation(Vertical.class) != null)
			horizontal = false;
		else 
			horizontal = true;
	}

	@SuppressWarnings("unchecked")
	private List<Serializable> newList() {
		if (ClassUtils.isConcrete(getPropertyDescriptor().getPropertyClass())) {
			try {
				return (List<Serializable>) getPropertyDescriptor().getPropertyClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		} else {
			return new ArrayList<Serializable>();
		}
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		if (getPropertyDescriptor().isPropertyRequired()) {
			add(new WebMarkupContainer("enable").setVisible(false));
		} else {
			add(new CheckBox("enable", new IModel<Boolean>() {
				
				@Override
				public void detach() {
					
				}
	
				@Override
				public Boolean getObject() {
					return PolymorphicListPropertyEditor.this.get("listEditor").isVisible();
				}
	
				@Override
				public void setObject(Boolean object) {
					PolymorphicListPropertyEditor.this.get("listEditor").setVisible(object);
				}
				
			}).add(new AjaxFormComponentUpdatingBehavior("change") {
				
				@Override
				protected void onUpdate(AjaxRequestTarget target) {
					target.add(PolymorphicListPropertyEditor.this.get("listEditor"));
				}
				
			}));
			
		}
		
		List<Serializable> list = getModelObject();
		if (list == null && getPropertyDescriptor().isPropertyRequired())
			list = newList(); 
		add(newListEditor(list));
	}

	private Component newListEditor(List<Serializable> list) {
		final WebMarkupContainer table = new WebMarkupContainer("listEditor");
		if (horizontal)
			table.add(AttributeAppender.append("class", " horizontal"));
		else
			table.add(AttributeAppender.append("class", " vertical"));

		table.setOutputMarkupId(true);
		table.setOutputMarkupPlaceholderTag(true);

		final RepeatingView rows = new RepeatingView("elements");
		table.add(rows);
		
		if (list != null) {
			for (Serializable element: list) {
				addRow(rows, element);
			}
		} else {
			table.setVisible(false);
		}
		
		WebMarkupContainer noElements = new WebMarkupContainer("noElements") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!rows.iterator().hasNext());
			}
			
		};
		
		table.add(noElements);
		
		WebMarkupContainer newRow = new WebMarkupContainer("addRow");
		newRow.add(AttributeModifier.replace("colspan", new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				if (rows.iterator().hasNext()) {
					if (horizontal)
						return "3";
					else
						return "2";
				} else {
					return "1";
				}
			}
			
		}));
		newRow.add(new AjaxButton("addElement") {

			@Override
			public void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);

				Serializable newElement;
				try {
					newElement = (Serializable) implementations.get(0).newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				addRow(rows, newElement);
				target.add(PolymorphicListPropertyEditor.this.get("listEditor"));
			}
			
		}.setDefaultFormProcessing(false));
		
		table.add(newRow);
		
		table.add(new SortBehavior() {

			@Override
			protected void onSort(AjaxRequestTarget target, SortPosition from, SortPosition to) {
				/*
				List<Component> children = new ArrayList<>();
				for (Component child: rows)
					children.add(child);

				Component fromChild = children.remove(from.getItemIndex());
				children.add(to.getItemIndex(), fromChild);
				
				rows.removeAll();
				for (Component child: children)
					rows.add(child);
				*/

				// Do not use code above as removing components outside of a container and add again 
				// can cause the fenced feedback panel not functioning properly
				if (from.getItemIndex() < to.getItemIndex()) {
					for (int i=0; i<to.getItemIndex()-from.getItemIndex(); i++) 
						rows.swap(from.getItemIndex()+i, from.getItemIndex()+i+1);
				} else {
					for (int i=0; i<from.getItemIndex()-to.getItemIndex(); i++) 
						rows.swap(from.getItemIndex()-i, from.getItemIndex()-i-1);
				}
			}
			
		}.sortable("tbody").handle(".handle").helperClass("sort-helper"));

		return table;		
	}

	private void addRow(final RepeatingView rows, Serializable element) {
		final Fragment row;
		if (horizontal)
			row = new Fragment(rows.newChildId(), "horizontalFrag", this);
		else 
			row = new Fragment(rows.newChildId(), "verticalFrag", this);
		
		rows.add(row);
		
		List<String> implementationNames = new ArrayList<String>();
		for (Class<?> each: implementations)
			implementationNames.add(EditableUtils.getName(each));
				
		row.add(new DropDownChoice<String>("elementTypeSelector", new IModel<String>() {
			
			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				Component elementEditor = row.get("elementEditor");
				if (elementEditor.isVisible()) {
					return EditableUtils.getName(((BeanEditor<?>) elementEditor).getBeanDescriptor().getBeanClass());
				} else {
					return null;
				}
			}

			@Override
			public void setObject(String object) {
				for (Class<?> each: implementations) {
					if (EditableUtils.getName(each).equals(object)) {
						Serializable element;
						try {
							element = (Serializable) each.newInstance();
						} catch (InstantiationException | IllegalAccessException e) {
							throw new RuntimeException(e);
						}
						row.replace(newElementEditor(element));
						return;
					}
				}
				throw new IllegalStateException("Unable to find implementation named " + object);
			}
			
			
		}, implementationNames).setNullValid(false).add(new AjaxFormComponentUpdatingBehavior("change"){

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(row.get("elementEditor"));
			}
			
		}));

		Component elementEditor = newElementEditor(element);
		row.add(elementEditor);
		
		row.add(new FencedFeedbackPanel("feedback", elementEditor));		

		row.add(new AjaxButton("deleteElement") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				rows.remove(row);
				target.add(PolymorphicListPropertyEditor.this.get("listEditor"));
			}

		}.setDefaultFormProcessing(false));
	}

	private Component newElementEditor(Serializable element) {
		Component elementEditor;
		if (element != null) {
			elementEditor = BeanContext.editBean("elementEditor", element);
		} else {
			elementEditor = new WebMarkupContainer("elementEditor").setVisible(false);
		}
		elementEditor.setOutputMarkupId(true);
		elementEditor.setOutputMarkupPlaceholderTag(true);
		return elementEditor;
	}

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		int index = ((PathSegment.Element) pathSegment).getIndex();

		RepeatingView rows = (RepeatingView) get("listEditor").get("elements");
		
		int currentIndex = 0;
		Iterator<Component> it = rows.iterator();
		Component row = it.next();
		while (currentIndex++ < index) {
			row = it.next();
		}
		
		return (ErrorContext) row.get("elementEditor");
	}

	@Override
	protected List<Serializable> convertInputToValue() throws ConversionException {
		if (get("listEditor").isVisible()) {
			List<Serializable> newList = newList();
			RepeatingView rows = (RepeatingView) get("listEditor").get("elements");
			for (Component row: rows) {
				@SuppressWarnings("unchecked")
				BeanEditor<Serializable> elementEditor = (BeanEditor<Serializable>) row.get("elementEditor");
				newList.add(elementEditor.getConvertedInput());
			}
			return newList;
		} else {
			return null;
		}
	}
	
}
