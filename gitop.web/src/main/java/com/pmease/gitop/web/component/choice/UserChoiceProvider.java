package com.pmease.gitop.web.component.choice;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.common.collect.Lists;
import com.pmease.gitop.core.Gitop;
import com.pmease.gitop.core.manager.UserManager;
import com.pmease.gitop.model.User;
import com.pmease.gitop.web.service.AvatarManager;
import com.vaynberg.wicket.select2.ChoiceProvider;
import com.vaynberg.wicket.select2.Response;

public class UserChoiceProvider extends ChoiceProvider<User> {

	private static final long serialVersionUID = 1L;
	
	private static final int PAGE_SIZE = 25;

	@Override
	public void query(String term, int page, Response<User> response) {
		UserManager um = Gitop.getInstance(UserManager.class);
		int first = page * PAGE_SIZE;
		Criterion criterion = Restrictions.or(
				Restrictions.ilike("name", term, MatchMode.START),
				Restrictions.ilike("displayName", term, MatchMode.START));
		List<User> users = um.query(new Criterion[] {criterion}, new Order[]{Order.asc("name")}, first, PAGE_SIZE + 1);

		if (users.size() <= PAGE_SIZE) {
			response.addAll(users);
		} else {
			response.addAll(users.subList(0, PAGE_SIZE));
			response.setHasMore(true);
		}
	}

	@Override
	public void toJson(User choice, JSONWriter writer) throws JSONException {
		writer.key("id").value(choice.getId()).key("name").value(StringEscapeUtils.escapeHtml4(choice.getName()));
		if (choice.getFullName() != null)
			writer.key("displayName").value(StringEscapeUtils.escapeHtml4(choice.getFullName()));
		writer.key("email").value(StringEscapeUtils.escapeHtml4(choice.getEmailAddress()));
		writer.key("avatar").value(Gitop.getInstance(AvatarManager.class).getAvatarUrl(choice));
	}

	@Override
	public Collection<User> toChoices(Collection<String> ids) {
		List<User> users = Lists.newArrayList();
		UserManager um = Gitop.getInstance(UserManager.class);
		for (String each : ids) {
			Long id = Long.valueOf(each);
			users.add(um.load(id));
		}

		return users;
	}

}