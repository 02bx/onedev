package com.pmease.gitplex.web.component.pullrequest.requestreviewer;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.web.Constants;
import com.pmease.gitplex.web.component.accountchoice.AbstractAccountChoiceProvider;
import com.vaynberg.wicket.select2.Response;

public class ReviewerProvider extends AbstractAccountChoiceProvider {

	private static final long serialVersionUID = 1L;

	private final IModel<PullRequest> requestModel;
	
	public ReviewerProvider(IModel<PullRequest> requestModel) {
		this.requestModel = requestModel;
	}
	
	@Override
	public void query(String term, int page, Response<Account> response) {
		List<Account> reviewers = requestModel.getObject().getPotentialReviewers();

		for (Iterator<Account> it = reviewers.iterator(); it.hasNext();) {
			Account user = it.next();
			if (!user.matches(term))
				it.remove();
		}
		
		Collections.sort(reviewers, new Comparator<Account>() {

			@Override
			public int compare(Account user1, Account user2) {
				return user1.getDisplayName().compareTo(user2.getDisplayName());
			}
			
		});

		int first = page * Constants.DEFAULT_PAGE_SIZE;
		int last = first + Constants.DEFAULT_PAGE_SIZE;
		if (last > reviewers.size()) {
			response.addAll(reviewers.subList(first, reviewers.size()));
		} else {
			response.addAll(reviewers.subList(first, last));
			response.setHasMore(last < reviewers.size());
		}
	}

	@Override
	public void detach() {
		requestModel.detach();
		
		super.detach();
	}

}