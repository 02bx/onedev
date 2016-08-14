package com.pmease.gitplex.core.manager.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.pmease.commons.hibernate.Sessional;
import com.pmease.commons.hibernate.Transactional;
import com.pmease.commons.hibernate.dao.AbstractEntityManager;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.hibernate.dao.EntityCriteria;
import com.pmease.commons.loader.ListenerRegistry;
import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.PullRequest;
import com.pmease.gitplex.core.entity.PullRequestReview;
import com.pmease.gitplex.core.entity.PullRequestUpdate;
import com.pmease.gitplex.core.event.pullrequest.PullRequestApproved;
import com.pmease.gitplex.core.event.pullrequest.PullRequestChangeEvent;
import com.pmease.gitplex.core.event.pullrequest.PullRequestDisapproved;
import com.pmease.gitplex.core.event.pullrequest.PullRequestReviewDeleted;
import com.pmease.gitplex.core.manager.AccountManager;
import com.pmease.gitplex.core.manager.PullRequestManager;
import com.pmease.gitplex.core.manager.PullRequestReviewManager;

@Singleton
public class DefaultPullRequestReviewManager extends AbstractEntityManager<PullRequestReview> implements PullRequestReviewManager {

	private final AccountManager accountManager;

	private final PullRequestManager pullRequestManager;
	
	private final ListenerRegistry listenerRegistry;
	
	@Inject
	public DefaultPullRequestReviewManager(Dao dao, AccountManager accountManager, PullRequestManager pullRequestManager, 
			ListenerRegistry listenerRegistry) {
		super(dao);
		
		this.accountManager = accountManager;
		this.pullRequestManager = pullRequestManager;
		this.listenerRegistry = listenerRegistry;
	}

	@Sessional
	@Override
	public PullRequestReview find(Account user, PullRequestUpdate update) {
		return find(EntityCriteria.of(PullRequestReview.class)
				.add(Restrictions.eq("user", user)) 
				.add(Restrictions.eq("update", update)));
	}

	@Transactional
	@Override
	public void review(PullRequest request, PullRequestReview.Result result, String note) {
		PullRequestReview review = new PullRequestReview();
		review.setUpdate(request.getLatestUpdate());
		review.setResult(result);
		review.setUser(accountManager.getCurrent());
		review.setDate(new Date());
		save(review);	
		
		PullRequestChangeEvent event;
		if (result == PullRequestReview.Result.APPROVE) {
			event = new PullRequestApproved(review, note);
		} else {
			event = new PullRequestDisapproved(review, note);
		}
		listenerRegistry.post(event);
		request.setLastEvent(event);
		pullRequestManager.save(request);
	}

	@Transactional
	@Override
	public void delete(PullRequestReview entity) {
		super.delete(entity);
		PullRequestReviewDeleted event = new PullRequestReviewDeleted(entity, accountManager.getCurrent(), null); 
		listenerRegistry.post(event);
		event.getRequest().setLastEvent(event);
		pullRequestManager.save(event.getRequest());
	}

	@Sessional
	@Override
	public List<PullRequestReview> findAll(PullRequest request) {
		EntityCriteria<PullRequestReview> criteria = EntityCriteria.of(PullRequestReview.class);
		criteria.createCriteria("update").add(Restrictions.eq("request", request));
		criteria.addOrder(Order.asc("date"));
		return findAll(criteria);
	}

	@Transactional
	@Override
	public void deleteAll(Account user, PullRequest request) {
		for (Iterator<PullRequestReview> it = request.getReviews().iterator(); it.hasNext();) {
			PullRequestReview review = it.next();
			if (review.getUser().equals(user)) {
				delete(review);
				it.remove();
			}
		}
	}

}
