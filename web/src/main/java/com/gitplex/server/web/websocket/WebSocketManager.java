package com.gitplex.server.web.websocket;

import javax.annotation.Nullable;

import com.gitplex.server.web.page.CommonPage;

public interface WebSocketManager {
	
	static final String ERROR_MESSAGE = "ErrorMessage";
	
	static final String RENDER_CALLBACK = "RenderCallback";
	
	static final String CONNECT_CALLBACK = "ConnectCallback";
	
	static final String KEEP_ALIVE = "KeepAlive";
	
	void onRegionChange(CommonPage page);
	
	void onDestroySession(String sessionId);
	
	void render(WebSocketRegion region, @Nullable PageKey sourcePageKey);
	
	void start();
	
	void stop();
	
}
