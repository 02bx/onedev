package com.gitplex.server.web.page.base;

import static org.apache.wicket.ajax.attributes.CallbackParameter.explicit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes.Method;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.time.Duration;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.unbescape.javascript.JavaScriptEscape;

import com.gitplex.launcher.loader.AppLoader;
import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.UserManager;
import com.gitplex.server.model.User;
import com.gitplex.server.web.assets.Assets;
import com.gitplex.server.web.behavior.AbstractPostAjaxBehavior;
import com.gitplex.server.web.page.init.ServerInitPage;
import com.gitplex.server.web.page.security.LoginPage;
import com.gitplex.server.web.websocket.PageDataChanged;
import com.gitplex.server.web.websocket.WebSocketManager;
import com.gitplex.server.web.websocket.WebSocketRegion;

@SuppressWarnings("serial")
public abstract class BasePage extends WebPage {

	public static final String PARAM_AUTOSAVE_KEY_TO_CLEAR = "autosaveKeyToClear";
	
	private FeedbackPanel sessionFeedback;
	
	private RepeatingView rootComponents;
	
	public BasePage() {
		checkReady();
	}

	public BasePage(PageParameters params) {
		super(params);
		checkReady();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		if (!isPermitted())
			unauthorized();
		
		add(new Label("pageTitle", getPageTitle()) {

			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);
				
				/* 
				 * Render page resources in first child to make sure every other resources appears after it, 
				 * including onDomReady and onWindowLoad call
				 */
				response.render(JavaScriptHeaderItem.forReference(new BaseResourceReference()));
				
				String autosaveKeyToClear = getPageParameters().get(PARAM_AUTOSAVE_KEY_TO_CLEAR).toString();
				if (autosaveKeyToClear != null)
					autosaveKeyToClear = "'" + JavaScriptEscape.escapeJavaScript(autosaveKeyToClear) + "'";
				else
					autosaveKeyToClear = "undefined";
				response.render(OnDomReadyHeaderItem.forScript(String.format("gitplex.server.onDomReady(%s);", 
						autosaveKeyToClear)));
				response.render(OnLoadHeaderItem.forScript("gitplex.server.onWindowLoad();"));
			}
			
		});

		add(new WebMarkupContainer("pageRefresh") {

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("content", getPageRefreshInterval());
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(getPageRefreshInterval() != 0);
			}

		});
		
		add(new WebMarkupContainer("robots") {
			
			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("content", getRobotsMeta());
			}
			
		});
		
		add(new WebMarkupContainer("favicon") {

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				CharSequence url = urlFor(
						new PackageResourceReference(Assets.class, "favicon.ico"), 
						new PageParameters()); 
				tag.put("href", url);
			}

		});
		add(new WebMarkupContainer("appleTouchIcon") {

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				CharSequence url = urlFor(
						new PackageResourceReference(Assets.class, "image/apple-touch-icon.png"), 
						new PageParameters()); 
				tag.put("href", url);
			}

		});
		add(new WebMarkupContainer("appleTouchIconPrecomposed") {

			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				CharSequence url = urlFor(
						new PackageResourceReference(Assets.class, "image/apple-touch-icon.png"), 
						new PageParameters()); 
				tag.put("href", url);
			}
			
		});
				
		add(new AbstractPostAjaxBehavior() {
			
			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
				super.updateAjaxAttributes(attributes);
				attributes.setMethod(Method.POST);
			}

			@Override
			protected void respond(AjaxRequestTarget target) {
				IRequestParameters params = RequestCycle.get().getRequest().getPostParameters();
				String encodedData = params.getParameterValue("data").toString();
				
				byte[] bytes = Base64.decodeBase64(encodedData.getBytes());
				Serializable data = (Serializable) SerializationUtils.deserialize(bytes);
				onPopState(target, data);
				target.appendJavaScript("gitplex.server.viewState.getFromHistoryAndSetToView();");
			}
			
			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				super.renderHead(component, response);

				String script = String.format("gitplex.server.history.init(%s);", 
						getCallbackFunction(explicit("data"))); 
				response.render(OnDomReadyHeaderItem.forScript(script));
			}

		});
		
		sessionFeedback = new SessionFeedbackPanel("sessionFeedback");
		add(sessionFeedback);			
		sessionFeedback.setOutputMarkupId(true);
		
		int sessionTimeout = AppLoader.getInstance(ServletContextHandler.class)
				.getSessionHandler().getSessionManager().getMaxInactiveInterval();
		add(new WebMarkupContainer("keepSessionAlive")
				.add(new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(sessionTimeout*500L))));
		
		add(rootComponents = new RepeatingView("rootComponents"));
		
		add(new WebSocketBehavior() {

			@Override
			protected void onMessage(WebSocketRequestHandler handler, TextMessage message) {
				super.onMessage(handler, message);
				
				if (message.getText().equals(WebSocketManager.RENDER_CALLBACK)) {
					send(getPage(), Broadcast.BREADTH, new PageDataChanged(handler, false));
				} else if (message.getText().equals(WebSocketManager.CONNECT_CALLBACK)) {
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
					send(getPage(), Broadcast.BREADTH, new PageDataChanged(handler, true));
				} 
		 
			}
			
		});
					
	}
	
	public FeedbackPanel getSessionFeedback() {
		return sessionFeedback;
	}
	
	@Override
	protected void onBeforeRender() {
		rootComponents.removeAll();
		super.onBeforeRender();
	}

	public void pushState(IPartialPageRequestHandler partialPageRequestHandler, String url, Serializable data) {
		String encodedData = new String(Base64.encodeBase64(SerializationUtils.serialize(data)));
		String script = String.format("gitplex.server.history.pushState('%s', '%s');", url, encodedData);
		partialPageRequestHandler.prependJavaScript(script);
	}
	
	protected void onPopState(AjaxRequestTarget target, Serializable data) {
	}
	
	public RepeatingView getRootComponents() {
		return rootComponents;
	}

	@Override
	protected void onAfterRender() {
		if (getWebSocketRegions() != null)
			AppLoader.getInstance(WebSocketManager.class).onRegionChange(this);
		super.onAfterRender();
	}

	public Collection<WebSocketRegion> getWebSocketRegions() {
		return new ArrayList<>();
	}

	private void checkReady() {
		if (!GitPlex.getInstance().isReady() && getClass() != ServerInitPage.class)
			throw new RestartResponseAtInterceptPageException(ServerInitPage.class);
	}

	protected boolean isPermitted() {
		return true;
	}
	
	protected String getRobotsMeta() {
		return "";
	}
	
	@Nullable
	protected final User getLoginUser() {
		return GitPlex.getInstance(UserManager.class).getCurrent();
	}
	
	public void unauthorized() {
		if (getLoginUser() != null) 
			throw new UnauthorizedException("You are not allowed to perform this operation");
		else 
			throw new RestartResponseAtInterceptPageException(LoginPage.class);
	}
	
	protected final String getPageTitle() {
		return "GitPlex - Git Management and Code Review";
	};

	protected int getPageRefreshInterval() {
		return 0;
	}
	
}
