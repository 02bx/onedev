/*
 * Copyright PMEase (c) 2005-2008,
 * Date: Feb 24, 2008
 * Time: 4:29:05 PM
 * All rights reserved.
 * 
 * Revision: $Id: TransactionInterceptor.java 1209 2008-07-28 00:16:18Z robin $
 */
package com.pmease.commons.hibernate;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import com.google.inject.Inject;

class TransactionInterceptor implements MethodInterceptor {

	@Inject
	private UnitOfWork unitOfWork;
	
	public Object invoke(MethodInvocation mi) throws Throwable {
		unitOfWork.begin();
		try {
			Session session = unitOfWork.getSession();
			if (session.getTransaction().getStatus() == TransactionStatus.ACTIVE) {
				return mi.proceed();
			} else {
				Transaction tx = session.beginTransaction();
				FlushMode previousMode = session.getFlushMode();
				session.setFlushMode(FlushMode.COMMIT);
				try {
					Object result = mi.proceed();
					tx.commit();
					return result;
				} catch (Throwable t) {
					try {
						tx.rollback();
					} catch (Throwable t2) {
					}
					throw t;
				} finally {
					session.setFlushMode(previousMode);
				}
			}
			
		} finally {
			unitOfWork.end();
		}
	}

}
