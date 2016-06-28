package com.pmease.gitplex.core.manager;

import com.pmease.commons.hibernate.dao.EntityManager;
import com.pmease.gitplex.core.entity.Notification;
import com.pmease.gitplex.core.listener.PullRequestListener;

public interface NotificationManager extends EntityManager<Notification>, PullRequestListener {

}
