package com.pmease.gitop.web.editable.team;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import com.pmease.gitop.web.component.team.TeamMultiChoice;
import com.pmease.gitop.web.page.repository.RepositoryBasePage;

@SuppressWarnings("serial")
public class TeamMultiChoiceEditor extends PropertyEditor<List<Long>> {
	
	private TeamMultiChoice input;
	
	public TeamMultiChoiceEditor(String id, PropertyDescriptor propertyDescriptor, IModel<List<Long>> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
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

    	List<Team> teames = new ArrayList<>();
		if (getModelObject() != null) {
			Dao dao = Gitop.getInstance(Dao.class);
			for (Long teamId: getModelObject()) 
				teames.add(dao.load(Team.class, teamId));
		} 
		
		input = new TeamMultiChoice("input", new Model((Serializable)teames), teamProvider);
        input.setConvertEmptyInputStringToNull(true);
        
        add(input);
	}

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		return null;
	}

	@Override
	protected List<Long> convertInputToValue() throws ConversionException {
		List<Long> teamIds = new ArrayList<>();
		Collection<Team> teames = input.getConvertedInput();
		if (teames != null) {
			for (Team team: teames)
				teamIds.add(team.getId());
		}
		return teamIds;
	}

}
