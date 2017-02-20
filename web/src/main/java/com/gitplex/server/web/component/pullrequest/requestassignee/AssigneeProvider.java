package com.gitplex.server.web.component.pullrequest.requestassignee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.Lists;
import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.AccountManager;
import com.gitplex.server.model.Account;
import com.gitplex.server.model.Depot;
import com.gitplex.server.persistence.dao.Dao;
import com.gitplex.server.security.ObjectPermission;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.security.privilege.DepotPrivilege;
import com.gitplex.server.web.WebConstants;
import com.gitplex.server.web.component.select2.ChoiceProvider;
import com.gitplex.server.web.component.select2.Response;
import com.gitplex.server.web.util.avatar.AvatarManager;

public class AssigneeProvider extends ChoiceProvider<Assignee> {

	private static final long serialVersionUID = 1L;

	private final IModel<Depot> depotModel;
	
	public AssigneeProvider(IModel<Depot> depotModel) {
		this.depotModel = depotModel;
	}
	
	@Override
	public void query(String term, int page, Response<Assignee> response) {
		Depot depot = depotModel.getObject();
		List<Assignee> assignees = new ArrayList<>();
		for (Account user: SecurityUtils.findUsersCan(depot, DepotPrivilege.WRITE)) {
			if (user.matches(term)) {
				assignees.add(new Assignee(user, null));
			}
		}
		assignees.sort((assignee1, assignee2) 
				-> assignee1.getUser().getDisplayName().compareTo(assignee2.getUser().getDisplayName()));
		if (StringUtils.isBlank(term)) {
			if (!depot.getAccount().isOrganization())
				assignees.add(0, new Assignee(depot.getAccount(), "Repository Owner"));
			ObjectPermission writePermission = ObjectPermission.ofDepotWrite(depotModel.getObject());
			Account currentUser = GitPlex.getInstance(AccountManager.class).getCurrent();
			if (currentUser != null && currentUser.asSubject().isPermitted(writePermission))
				assignees.add(0, new Assignee(currentUser, "Me"));
		}

		int first = page * WebConstants.DEFAULT_PAGE_SIZE;
		int last = first + WebConstants.DEFAULT_PAGE_SIZE;
		if (last > assignees.size()) {
			response.addAll(assignees.subList(first, assignees.size()));
		} else {
			response.addAll(assignees.subList(first, last));
			response.setHasMore(last < assignees.size());
		}
	}

	@Override
	public void toJson(Assignee choice, JSONWriter writer) throws JSONException {
		writer.key("id").value(choice.getUser().getId())
			.key("name").value(StringEscapeUtils.escapeHtml4(choice.getUser().getName()));
		if (choice.getUser().getFullName() != null)
			writer.key("fullName").value(StringEscapeUtils.escapeHtml4(choice.getUser().getFullName()));
		writer.key("email").value(StringEscapeUtils.escapeHtml4(choice.getUser().getEmail()));
		String avatarUrl =  GitPlex.getInstance(AvatarManager.class).getAvatarUrl(choice.getUser());
		writer.key("avatar").value(avatarUrl);
		if (choice.getAlias() != null)
			writer.key("alias").value(StringEscapeUtils.escapeHtml4(choice.getAlias()));
	}

	@Override
	public Collection<Assignee> toChoices(Collection<String> ids) {
		List<Assignee> assignees = Lists.newArrayList();
		Dao dao = GitPlex.getInstance(Dao.class);
		for (String each : ids) {
			Long id = Long.valueOf(each);
			assignees.add(new Assignee(dao.load(Account.class, id), null));
		}

		return assignees;
	}

	@Override
	public void detach() {
		depotModel.detach();
		
		super.detach();
	}

}