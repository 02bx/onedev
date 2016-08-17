package com.pmease.commons.wicket.websocket;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.ws.api.IWebSocketConnection;
import org.apache.wicket.protocol.ws.api.registry.IWebSocketConnectionRegistry;
import org.apache.wicket.protocol.ws.api.registry.SimpleWebSocketConnectionRegistry;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pmease.commons.hibernate.Sessional;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.wicket.CommonPage;

@Singleton
public class DefaultWebSocketManager implements WebSocketManager {

	private static final Logger logger = LoggerFactory.getLogger(DefaultWebSocketManager.class);
	
	private final Application application;
	
	private final Dao dao;
	
	private final ExecutorService executorService;

	private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
	
	private final WebSocketPolicy webSocketPolicy;
	
	private final Map<PageKey, Collection<WebSocketRegion>> regions = new ConcurrentHashMap<>();
	
	private final IWebSocketConnectionRegistry connectionRegistry = new SimpleWebSocketConnectionRegistry();

	@Inject
	public DefaultWebSocketManager(Application application, Dao dao,
			ExecutorService executorService, WebSocketPolicy webSocketPolicy) {
		this.application = application;
		this.dao = dao;
		this.executorService = executorService;
		this.webSocketPolicy = webSocketPolicy;
	}
	
	@Override
	public void onRegionChange(CommonPage page) {
		regions.put(new PageKey(page), page.getWebSocketRegions());
	}
	
	@Override
	public void onDestroySession(String sessionId) {
		for (Iterator<Map.Entry<PageKey, Collection<WebSocketRegion>>> it = regions.entrySet().iterator(); it.hasNext();) {
			if (it.next().getKey().getSessionId().equals(sessionId))
				it.remove();
		}
	}

	@Sessional
	@Override
	public void renderAsync(WebSocketRegion region, @Nullable PageKey sourcePageKey) {
		if (dao.getSession().getTransaction().getStatus() == TransactionStatus.ACTIVE) {
			dao.doAsyncAfterCommit(new Runnable() {

				@Override
				public void run() {
					render(region, sourcePageKey);
				}
				
			});
		} else {
			executorService.execute(new Runnable() {
	
				@Override
				public void run() {
					render(region, sourcePageKey);
				}
				
			});
		}
	}
	
	@Override
	public void render(WebSocketRegion region, @Nullable PageKey sourcePageKey) {
		try {
			for (IWebSocketConnection connection: connectionRegistry.getConnections(application)) {
				synchronized (connection) {
					PageKey pageKey = ((WebSocketConnection) connection).getPageKey();
					if (connection.isOpen() 
							&& (sourcePageKey == null || !sourcePageKey.equals(pageKey)) 
							&& containsRegion(connection, region)) {
						connection.sendMessage(RENDER_CALLBACK);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error sending websocket message", e);
		}
	}
	
	private boolean containsRegion(IWebSocketConnection connection, WebSocketRegion region) {
		PageKey pageKey = ((WebSocketConnection)connection).getPageKey();
		Collection<WebSocketRegion> connectionRegions = regions.get(pageKey);
		if (connectionRegions != null) {
			for (WebSocketRegion connectionRegion: connectionRegions) {
				if (connectionRegion.contains(region)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onConnect(WebSocketConnection connection) {
		/* 
		 * re-render interesting parts upon websocket connecting after a page is opened, 
		 * this is necessary in case some web socket render request is sent between the 
		 * gap of opening a page and a websocket connection is established. For instance
		 * when someone creates a pull request, the server will re-render integration 
		 * preview section of the page after preview is calculated and this may happen 
		 * before the web socket connection is established. Requiring the page to 
		 * re-render the integration preview section after connecting will make it 
		 * displaying correctly    
		 */
		synchronized (connection) {
			try {
				connection.sendMessage(INITIAL_RENDER_CALLBACK);
			} catch (IOException e) {
			}
		}
	}
	
	@Override
	public void start() {
		scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {
				for (IWebSocketConnection connection: new SimpleWebSocketConnectionRegistry().getConnections(application)) {
					synchronized (connection) {
						if (connection.isOpen()) {
							try {
								connection.sendMessage(WebSocketManager.KEEP_ALIVE);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}
					}
				}
			}
			
		}, 0, webSocketPolicy.getIdleTimeout()/2, TimeUnit.MILLISECONDS);
	}

	@Override
	public void stop() {
		scheduledExecutorService.shutdown();
	}

}
