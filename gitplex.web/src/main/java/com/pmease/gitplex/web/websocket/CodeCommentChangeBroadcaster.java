package com.pmease.gitplex.web.websocket;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.resource.transaction.spi.TransactionStatus;

import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.loader.Listen;
import com.pmease.commons.wicket.WicketUtils;
import com.pmease.commons.wicket.websocket.PageKey;
import com.pmease.commons.wicket.websocket.WebSocketManager;
import com.pmease.gitplex.core.event.codecomment.CodeCommentEvent;

@Singleton
public class CodeCommentChangeBroadcaster {
	
	private final Dao dao;
	
	private final ExecutorService executorService;
	
	private final WebSocketManager webSocketManager;
	
	@Inject
	public CodeCommentChangeBroadcaster(Dao dao, ExecutorService executorService, 
			WebSocketManager webSocketManager) {
		this.dao = dao;
		this.executorService = executorService;
		this.webSocketManager = webSocketManager;
	}
	
	private void requestToRender(CodeCommentChangedRegion region) {
		// Send web socket message in a thread in order not to blocking UI operations
		PageKey pageKey = WicketUtils.getPageKey();
		executorService.execute(new Runnable() {

			@Override
			public void run() {
				webSocketManager.requestToRender(region, pageKey, null);
			}
			
		});
	}
	
	@Listen
	public void on(CodeCommentEvent event) {
		CodeCommentChangedRegion region = new CodeCommentChangedRegion(event.getComment().getId());
		if (dao.getSession().getTransaction().getStatus() == TransactionStatus.ACTIVE) {
			/*
			 * Make sure that code comment and associated objects are committed before
			 * sending render request; otherwise rendering request may not reflect
			 * expected status as rendering happens in another thread which may get
			 * executed before code comment modification is committed.
			 */
			dao.doAfterCommit(new Runnable() {

				@Override
				public void run() {
					requestToRender(region);
				}
				
			});
		} else {
			requestToRender(region);
		}
	}
		
}