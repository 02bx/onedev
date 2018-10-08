package io.onedev.server.web.websocket;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.onedev.launcher.loader.Listen;
import io.onedev.server.event.issue.IssueDeleted;
import io.onedev.server.event.issue.IssueEvent;
import io.onedev.server.model.Issue;
import io.onedev.server.model.support.issue.IssueBoard;
import io.onedev.server.web.util.WicketUtils;

@Singleton
public class IssueEventBroadcaster {
	
	private final WebSocketManager webSocketManager;
	
	@Inject
	public IssueEventBroadcaster(WebSocketManager webSocketManager) {
		this.webSocketManager = webSocketManager;
	}

	@Listen
	public void on(IssueEvent event) {
		if (!(event instanceof IssueDeleted))
			webSocketManager.notifyObservableChange(Issue.getWebSocketObservable(event.getIssue().getId()), WicketUtils.getPageKey());
		if (event.affectsBoards())
			webSocketManager.notifyObservableChange(IssueBoard.getWebSocketObservable(event.getIssue().getProject().getId()), null);
	}

}