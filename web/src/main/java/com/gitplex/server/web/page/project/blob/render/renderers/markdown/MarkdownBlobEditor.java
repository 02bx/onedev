package com.gitplex.server.web.page.project.blob.render.renderers.markdown;

import java.nio.charset.Charset;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.Model;

import com.gitplex.server.util.ContentDetector;
import com.gitplex.server.web.component.markdown.MarkdownEditor;
import com.gitplex.server.web.page.project.blob.render.BlobRenderContext;
import com.gitplex.server.web.page.project.blob.render.BlobRenderContext.Mode;
import com.gitplex.utils.StringUtils;

@SuppressWarnings("serial")
abstract class MarkdownBlobEditor extends FormComponentPanel<byte[]> {

	private final BlobRenderContext context;
	
	private final String charset;

	private MarkdownEditor input;
	
	public MarkdownBlobEditor(String id, BlobRenderContext context, byte[] initialContent) {
		super(id, Model.of(initialContent));

		this.context = context;
		
		Charset detectedCharset = ContentDetector.detectCharset(getModelObject());
		charset = (detectedCharset!=null?detectedCharset:Charset.defaultCharset()).name();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(input = new MarkdownEditor("input", Model.of(new String(getModelObject(), Charset.forName(charset))), 
				false, context) {

			@Override
			protected String getAutosaveKey() {
				return MarkdownBlobEditor.this.getAutosaveKey();
			}

		});
		if (context.getMode() != Mode.EDIT) {
			input.add(AttributeAppender.append("class", "no-autofocus"));
		}
		input.setOutputMarkupId(true);
	}

	@Override
	public void convertInput() {
		String content = input.getConvertedInput();
		if (content != null) {
			/*
			 * Textarea always uses CRLF as line ending, and below we change back to original EOL
			 */
			String initialContent = input.getModelObject();
			if (initialContent == null || !initialContent.contains("\r\n"))
				content = StringUtils.replace(content, "\r\n", "\n");
			setConvertedInput(content.getBytes(Charset.forName(charset)));
		} else {
			setConvertedInput(new byte[0]);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		if (context.getMode() == Mode.EDIT) {
			String script = String.format("$('#%s textarea').focus();", input.getMarkupId());
			response.render(OnDomReadyHeaderItem.forScript(script));
		}
	}

	protected abstract String getAutosaveKey();
	
}
