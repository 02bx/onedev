package com.gitplex.server.web.component.comment;

import org.apache.wicket.request.cycle.RequestCycle;
import org.jsoup.nodes.Element;

import com.gitplex.server.model.Account;
import com.gitplex.server.util.markdown.HtmlTransformer;
import com.gitplex.server.util.markdown.MentionParser;
import com.gitplex.server.web.page.account.overview.AccountOverviewPage;

public class MentionTransformer extends MentionParser implements HtmlTransformer {
	
	@Override
	public void transform(Element body) {
		parseMentions(body);
	}

	@Override
	protected String toHtml(Account user) {
		if (RequestCycle.get() != null) {
			return String.format("<a href='%s' class='mention'>@%s</a>", 
				RequestCycle.get().urlFor(AccountOverviewPage.class, AccountOverviewPage.paramsOf(user)), user.getName());
		} else {
			return super.toHtml(user);
		}
	}
	
}
