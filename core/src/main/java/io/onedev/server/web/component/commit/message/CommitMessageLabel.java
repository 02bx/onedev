package io.onedev.server.web.component.commit.message;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.unbescape.html.HtmlEscape;

import io.onedev.server.OneDev;
import io.onedev.server.model.Project;
import io.onedev.server.web.util.commitmessagetransform.CommitMessageTransformer;

@SuppressWarnings("serial")
public class CommitMessageLabel extends Label {

	public CommitMessageLabel(String id, IModel<Project> projectModel, IModel<String> messageModel) {
		super(id, new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				String message = HtmlEscape.escapeHtml5(messageModel.getObject());
				for (CommitMessageTransformer transformer: OneDev.getExtensions(CommitMessageTransformer.class)) {
					message = transformer.transform(projectModel.getObject(), message);
				}
				return message;
			}

			@Override
			protected void onDetach() {
				projectModel.detach();
				messageModel.detach();
				super.onDetach();
			}
			
		});
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		setEscapeModelStrings(false);
	}

}
