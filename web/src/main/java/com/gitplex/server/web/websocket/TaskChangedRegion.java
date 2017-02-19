package com.gitplex.server.web.websocket;

import com.gitplex.server.web.websocket.WebSocketRegion;

public class TaskChangedRegion implements WebSocketRegion {
	
	public final Long userId;

	public TaskChangedRegion(Long userId) {
		this.userId = userId;
	}

	@Override
	public boolean contains(WebSocketRegion region) {
		if (region instanceof TaskChangedRegion) {
			TaskChangedRegion pullRequestChangedRegion = (TaskChangedRegion) region;
			return userId.equals(pullRequestChangedRegion.userId);
		} else {
			return false;
		}
	}		
	
}