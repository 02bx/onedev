package com.pmease.gitplex.web.editable.teamchoice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import com.pmease.commons.wicket.editable.ErrorContext;
import com.pmease.commons.wicket.editable.PathSegment;
import com.pmease.commons.wicket.editable.PropertyDescriptor;
import com.pmease.commons.wicket.editable.PropertyEditor;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.Team;
import com.pmease.gitplex.core.manager.TeamManager;
import com.pmease.gitplex.web.component.teamchoice.TeamChoiceProvider;
import com.pmease.gitplex.web.component.teamchoice.TeamMultiChoice;
import com.pmease.gitplex.web.page.account.AccountPage;

@SuppressWarnings("serial")
public class TeamMultiChoiceEditor extends PropertyEditor<Collection<String>> {
	
	private TeamMultiChoice input;
	
	public TeamMultiChoiceEditor(String id, PropertyDescriptor propertyDescriptor, IModel<Collection<String>> propertyModel) {
		super(id, propertyDescriptor, propertyModel);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		IModel<Account> organizationModel = new AbstractReadOnlyModel<Account>() {

			@Override
			public Account getObject() {
				return ((AccountPage)getPage()).getAccount();
			}
			
		};
    	List<Team> teams = new ArrayList<>();
		if (getModelObject() != null) {
			TeamManager teamManager = GitPlex.getInstance(TeamManager.class);
			for (String teamName: getModelObject()) {
				Team team = teamManager.find(organizationModel.getObject(), teamName);
				if (team != null)
					teams.add(team);
			}
		} 
		
		input = new TeamMultiChoice("input", new Model((Serializable)teams), new TeamChoiceProvider(organizationModel));
        input.setConvertEmptyInputStringToNull(true);
        
        add(input);
	}

	@Override
	public ErrorContext getErrorContext(PathSegment pathSegment) {
		return null;
	}

	@Override
	protected List<String> convertInputToValue() throws ConversionException {
		List<String> teamNames = new ArrayList<>();
		Collection<Team> teams = input.getConvertedInput();
		if (teams != null) {
			for (Team team: teams)
				teamNames.add(team.getName());
		} 
		return teamNames;
	}

}
