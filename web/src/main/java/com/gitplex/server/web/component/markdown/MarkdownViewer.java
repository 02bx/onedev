package com.gitplex.server.web.component.markdown;

import static org.apache.wicket.ajax.attributes.CallbackParameter.explicit;

import javax.annotation.Nullable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hibernate.StaleStateException;

import com.gitplex.launcher.loader.AppLoader;
import com.gitplex.server.manager.MarkdownManager;
import com.gitplex.server.util.StringUtils;
import com.gitplex.server.web.behavior.AbstractPostAjaxBehavior;

import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;

@SuppressWarnings("serial")
public class MarkdownViewer extends GenericPanel<String> {

	private static final String TASK_CHECKED = "taskchecked";
	
	private final ContentVersionSupport contentVersionSupport;
	
	private long lastContentVersion;
	
	private AbstractPostAjaxBehavior behavior;
	
	public MarkdownViewer(String id, IModel<String> model, @Nullable ContentVersionSupport contentVersionSupport) {
		super(id, model);
		this.contentVersionSupport = contentVersionSupport;
		if (contentVersionSupport != null)
			lastContentVersion = contentVersionSupport.getVersion();
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();

		NotificationPanel feedback = new NotificationPanel("feedback", this);
		feedback.setOutputMarkupPlaceholderTag(true);
		add(feedback);
		
		add(new Label("content", new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				String markdown = MarkdownViewer.this.getModelObject();
				if (markdown != null) {
					return AppLoader.getInstance(MarkdownManager.class).render(markdown, getBaseUrl(), true);
				} else {
					return null;
				}
			}
			
		}).setEscapeModelStrings(false));
		
		add(behavior = new AbstractPostAjaxBehavior() {
			
			@Override
			protected void respond(AjaxRequestTarget target) {
				IRequestParameters params = RequestCycle.get().getRequest().getPostParameters();
				int taskPosition = params.getParameterValue(SourcePositionTrackExtension.DATA_START_ATTRIBUTE).toInt();
				boolean taskChecked = params.getParameterValue(TASK_CHECKED).toBoolean();
				String markdown = getComponent().getDefaultModelObjectAsString();
				String beforeTask = markdown.substring(0, taskPosition);
				String fromTask = markdown.substring(taskPosition);
				String beforeBracket = StringUtils.substringBefore(fromTask, "[");
				String afterBracket = StringUtils.substringAfter(fromTask, "]");
				String taskStatus = taskChecked?"x":" ";
				markdown = beforeTask + beforeBracket + "[" + taskStatus + "]" + afterBracket;

				try {
					if (contentVersionSupport.getVersion() != lastContentVersion)
						throw new StaleStateException("");
					setDefaultModelObject(markdown);
				} catch (StaleStateException e) {
					warn("Some one changed the content you are editing. The content has now been reloaded, "
							+ "please try again.");
				}
				target.add(MarkdownViewer.this);
				lastContentVersion = contentVersionSupport.getVersion();
			}
			
		});
		
		setOutputMarkupId(true);
	}
	
	protected String getBaseUrl() {
		return null; 
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forReference(new MarkdownResourceReference()));
		
		CharSequence callback = behavior.getCallbackFunction(
				explicit(SourcePositionTrackExtension.DATA_START_ATTRIBUTE), 
				explicit(TASK_CHECKED));
		
		String script = String.format("gitplex.server.markdown.onViewerDomReady('%s', %s, '%s');", 
				getMarkupId(), 
				contentVersionSupport!=null?callback:"undefined", 
				SourcePositionTrackExtension.DATA_START_ATTRIBUTE);
		response.render(OnDomReadyHeaderItem.forScript(script));
	}

}
