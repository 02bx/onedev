/**
 * Copyright (C) 2010 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.onedev.server.persistence;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Singleton;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.Transaction;

import com.google.inject.Inject;

import io.onedev.commons.utils.ExceptionUtils;
import io.onedev.server.persistence.annotation.Transactional;

@Singleton
public class DefaultTransactionManager implements TransactionManager {

	private final SessionManager sessionManager;
	
	private final ExecutorService executorService;

	@Inject
	public DefaultTransactionManager(SessionManager sessionManager, ExecutorService executorService) {
		this.sessionManager = sessionManager;
		this.executorService = executorService;
	}
	
	@Override
	public <T> T call(Callable<T> callable) {
		return sessionManager.call(new Callable<T>() {

			@Override
			public T call() throws Exception {
				if (getTransaction().isActive()) {
					return callable.call();
				} else {
					Transaction tx = sessionManager.getSession().beginTransaction();
					try {
						T result = callable.call();
						sessionManager.getSession().flush();
						tx.commit();
						return result;
					} catch (Throwable t) {
						tx.rollback();
						throw ExceptionUtils.unchecked(t);
					}
				}
			}
			
		});
	}

	@Override
	public void run(Runnable runnable) {
		call(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				runnable.run();
				return null;
			}
			
		});
	}

	@Override
	public void runAsync(Runnable runnable) {
		executorService.execute(new Runnable() {

			@Override
			public void run() {
				DefaultTransactionManager.this.run(runnable);
			}
			
		});
	}

	@Override
	public void runAfterCommit(Runnable runnable) {
		if (getTransaction().isActive()) {
			getTransaction().registerSynchronization(new Synchronization() {
				
				@Override
				public void beforeCompletion() {
				}
				
				@Override
				public void afterCompletion(int status) {
					if (status == Status.STATUS_COMMITTED)
						runnable.run();
				}
				
			});
		} else {
			runnable.run();
		}
	}
	
	@Transactional
	public void runAsyncAfterCommit(Runnable runnable) {
		runAfterCommit(new Runnable() {

			@Override
			public void run() {
				executorService.execute(runnable);
			}
			
		});
	}

	@Override
	public Transaction getTransaction() {
		return sessionManager.getSession().getTransaction();
	}

}
