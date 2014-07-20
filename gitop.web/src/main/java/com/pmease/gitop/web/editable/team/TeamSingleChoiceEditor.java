package com.pmease.gitop.web.editable.team;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.hibernate.criterion.Restrictions;

import com.pmease.commons.editable.PropertyDescriptor;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.commons.wicket.editable.ErrorContext;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.commons.wicket.editable.PropertyEditor;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.model.Team;
import com.pmease.gitop.web.component.team.TeamChoiceProvider;
import com.pmease.gitop.web.component.team.TeamSingleChoice;
import com.pmease.gitop.web.page.repository.RepositoryBasePage;

@SuppressWarnings("serial")
public class TeamSingleChoiceEditor extends PropertyEditor<Long> {
	
	private TeamSingleChoice input;
	
	public TeamSingleChoiceEditor(String id, PropertyDescriptor propertyDescriptor, IModel<Long> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
    	
    	TeamChoiceProvider teamProvider = new TeamChoiceProvider(new LoadableDetachableModel<EntityCriteria<Team>>() {

			@Override
			protected EntityCriteria<Team> load() {
				EntityCriteria<Team> criteria = EntityCriteria.of(Team.class);
				RepositoryBasePage page = (RepositoryBasePage) getPage();
				criteria.add(Restrictions.eq("owner", page.getRepository().getOwner()));
				return criteria;
			}
    		
    	});

    	Team team;
		if (getModelObject() != null)
			team =  Gitop.getInstance(Dao.class).load(Team.class, getModelObject()); 
		else
			team = null;
    	input = new TeamSingleChoice("input", Model.of(team), teamProvider);
        input.setConvertEmptyInputStringToNull(true);
        
        add(input);
	}

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		return null;
	}

	@Override
	protected Long convertInputToValue() throws ConversionException {
		Team team = input.getConvertedInput();
		if (team != null)
			return team.getId();
		else
			return null;
	}

}
