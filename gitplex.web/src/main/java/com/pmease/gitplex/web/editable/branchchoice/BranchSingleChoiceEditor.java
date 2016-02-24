package com.pmease.gitplex.web.editable.branchchoice;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.convert.ConversionException;

import com.pmease.commons.wicket.editable.ErrorContext;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.commons.wicket.editable.PropertyDescriptor;
import com.pmease.commons.wicket.editable.PropertyEditor;
import com.pmease.gitplex.core.entity.Depot;
import com.pmease.gitplex.web.component.branchchoice.BranchChoiceProvider;
import com.pmease.gitplex.web.component.branchchoice.BranchSingleChoice;
import com.pmease.gitplex.web.page.depot.DepotPage;

@SuppressWarnings("serial")
public class BranchSingleChoiceEditor extends PropertyEditor<String> {
	
	private BranchSingleChoice input;
	
	public BranchSingleChoiceEditor(String id, PropertyDescriptor propertyDescriptor, IModel<String> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
    	
    	BranchChoiceProvider branchProvider = new BranchChoiceProvider(new LoadableDetachableModel<Depot>() {

			@Override
			protected Depot load() {
				DepotPage page = (DepotPage) getPage();
				return page.getDepot();
			}
    		
    	});

    	input = new BranchSingleChoice("input", getModel(), branchProvider, !getPropertyDescriptor().isPropertyRequired());
        
        add(input);
	}

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		return null;
	}

	@Override
	protected String convertInputToValue() throws ConversionException {
		return input.getConvertedInput();
	}

}
