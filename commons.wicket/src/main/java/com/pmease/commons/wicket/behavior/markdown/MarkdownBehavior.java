package com.pmease.commons.wicket.behavior.markdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes.Method;
import static org.apache.wicket.ajax.attributes.CallbackParameter.*;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.crypt.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmease.commons.loader.AppLoader;
import com.pmease.commons.markdown.MarkdownManager;
import com.pmease.commons.wicket.CommonPage;
import com.pmease.commons.wicket.assets.atwho.AtWhoResourceReference;
import com.pmease.commons.wicket.assets.caret.CaretResourceReference;
import com.pmease.commons.wicket.assets.codemirror.HighlightResourceReference;

@SuppressWarnings("serial")
public class MarkdownBehavior extends AbstractDefaultAjaxBehavior {

	protected static final int ATWHO_LIMIT = 5;
	
	private SelectImagePanel imageSelector;
	
	@Override
	protected void respond(AjaxRequestTarget target) {
		IRequestParameters params = RequestCycle.get().getRequest().getPostParameters();
		String type = params.getParameterValue("type").toString();
		
		if (type.equals("markdownPreview")) {
			String markdown = params.getParameterValue("param").toOptionalString();
			String preview;
			if (StringUtils.isNotBlank(markdown)) {
				preview = AppLoader.getInstance(MarkdownManager.class).parseAndProcess(markdown);
			} else { 
				preview = "<i>Nothing to preview.</i>";
			}
			String script = String.format(""
					+ "var $preview=$('#%s~.md-preview');"
					+ "$preview.html('%s');"
					+ "pmease.commons.highlight($preview);", 
					getComponent().getMarkupId(), StringEscapeUtils.escapeEcmaScript(preview));
			target.appendJavaScript(script);
		} else if (type.equals("emojiQuery")){
			List<String> emojiNames = new ArrayList<>();
			String emojiQuery = params.getParameterValue("param").toOptionalString();
			if (StringUtils.isNotBlank(emojiQuery)) {
				emojiQuery = emojiQuery.toLowerCase();
				for (String emojiName: EmojiOnes.getInstance().all().keySet()) {
					if (emojiName.toLowerCase().contains(emojiQuery))
						emojiNames.add(emojiName);
				}
				Collections.sort(emojiNames, new Comparator<String>() {

					@Override
					public int compare(String name1, String name2) {
						return name1.length() - name2.length();
					}
					
				});
			} else {
				emojiNames.add("smile");
				emojiNames.add("worried");
				emojiNames.add("blush");
				emojiNames.add("+1");
				emojiNames.add("-1");
			}

			List<Map<String, String>> emojis = new ArrayList<>();
			for (String emojiName: emojiNames) {
				if (emojis.size() < ATWHO_LIMIT) {
					String emojiCode = EmojiOnes.getInstance().all().get(emojiName);
					CharSequence url = RequestCycle.get().urlFor(new PackageResourceReference(
							EmojiOnes.class, "emoji/" + emojiCode + ".png"), new PageParameters());
					Map<String, String> emoji = new HashMap<>();
					emoji.put("name", emojiName);
					emoji.put("url", url.toString());
					emojis.add(emoji);
				}
			}
			String json;
			try {
				json = AppLoader.getInstance(ObjectMapper.class).writeValueAsString(emojis);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
			String script = String.format("var $input = $('#%s');"
					+ "$input[0].atWhoEmojiRenderCallback(%s);"
					+ "$input[0].cachedEmojis[$input[0].atWhoEmojiQuery] = %s;", 
					getComponent().getMarkupId(), json, json);
			target.appendJavaScript(script);
		} else if (type.equals("loadEmojis")) {
			List<Map<String, String>> emojis = new ArrayList<>();
			for (Map.Entry<String, String> entry: EmojiOnes.getInstance().all().entrySet()) {
				Map<String, String> emoji = new HashMap<>();
				emoji.put("name", entry.getKey());
				emoji.put("url", RequestCycle.get().urlFor(new PackageResourceReference(
						EmojiOnes.class, "emoji/" + entry.getValue() + ".png"), new PageParameters()).toString());
				emojis.add(emoji);
			}

			String json;
			try {
				json = AppLoader.getInstance(ObjectMapper.class).writeValueAsString(emojis);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}

			String script = String.format("pmease.commons.markdown.onEmojisLoaded('%s', %s);", 
					getComponent().getMarkupId(), json);
			target.appendJavaScript(script);
		} else if (type.equals("selectImage")) {
			CommonPage page = (CommonPage) getComponent().getPage();
			imageSelector = new SelectImagePanel(page.getComponents().newChildId(), this);
			imageSelector.setOutputMarkupId(true);
			page.getComponents().add(imageSelector);
			imageSelector.setMarkupId(getComponent().getMarkupId() + "-imageinserter");
			target.add(imageSelector);
		} else if (type.equals("insertImage")) {
			String attachmentName = params.getParameterValue("param").toString();
			String attachmentUrl = getAttachmentSupport().getAttachmentUrl(attachmentName);
			insertImage(target, attachmentUrl);
		} else {
			throw new IllegalStateException("Unknown callback type: " + type);
		}
	}
	
	public void insertImage(AjaxRequestTarget target, String url) {
		String script;
		if (imageSelector != null) {
			CommonPage page = (CommonPage) imageSelector.getPage();
			page.getComponents().remove(imageSelector);
			imageSelector = null;
	
			script = String.format(""
					+ "pmease.commons.markdown.insertImage('%s', '%s');"
					+ "$('#%s-imageinserter').closest('.modal').modal('hide');", 
					getComponent().getMarkupId(), url, getComponent().getMarkupId());
		} else {
			script = String.format("pmease.commons.markdown.insertImage('%s', '%s');",
					getComponent().getMarkupId(), url);
		}
		target.appendJavaScript(script);
	}

	public void cancelInsertImage(AjaxRequestTarget target) {
		if (imageSelector != null) {
			CommonPage page = (CommonPage) imageSelector.getPage();
			page.getComponents().remove(imageSelector);
			imageSelector = null;
			String script = String.format("$('#%s-imageinserter').closest('.modal').modal('hide');", 
					getComponent().getMarkupId());
			target.appendJavaScript(script);
		}
	}
	
	public boolean isWebSafeImage(String fileName) {
		fileName = fileName.toLowerCase();
		return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") 
				|| fileName.endsWith(".gif") || fileName.endsWith(".png");
	}
	
	@Override
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
		super.updateAjaxAttributes(attributes);
		
		attributes.setMethod(Method.POST);
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response) {
		super.renderHead(component, response);
		
		response.render(JavaScriptHeaderItem.forReference(
				new JavaScriptResourceReference(MarkdownBehavior.class, "bootstrap-markdown.js")));
		response.render(CssHeaderItem.forReference(
				new CssResourceReference(MarkdownBehavior.class, "bootstrap-markdown.min.css")));
		response.render(JavaScriptHeaderItem.forReference(CaretResourceReference.INSTANCE));
		response.render(JavaScriptHeaderItem.forReference(AtWhoResourceReference.INSTANCE));
		response.render(JavaScriptHeaderItem.forReference(HighlightResourceReference.INSTANCE));
		
		response.render(JavaScriptHeaderItem.forReference(
				new JavaScriptResourceReference(MarkdownBehavior.class, "markdown.js")));
		response.render(CssHeaderItem.forReference(MarkdownCssResourceReference.INSTANCE));
		
		String encodedAttachmentSupport;
		AttachmentSupport attachmentSupport = getAttachmentSupport();
		if (attachmentSupport != null) {
			encodedAttachmentSupport = Base64.encodeBase64String(SerializationUtils.serialize(attachmentSupport));
			encodedAttachmentSupport = StringUtils.deleteWhitespace(encodedAttachmentSupport);
			encodedAttachmentSupport = StringEscapeUtils.escapeEcmaScript(encodedAttachmentSupport);
			encodedAttachmentSupport = "'" + encodedAttachmentSupport + "'";
		} else {
			encodedAttachmentSupport = "undefined";
		}
		String uploadUrl = RequestCycle.get().getUrlRenderer().renderRelativeUrl(Url.parse("attachment_upload"));
		String script = String.format("pmease.commons.markdown.init('%s', %s, %s, '%s', %s);", 
				component.getMarkupId(true), 
				ATWHO_LIMIT,
				getCallbackFunction(explicit("type"), explicit("param")), 
				uploadUrl, 
				encodedAttachmentSupport);
		response.render(OnDomReadyHeaderItem.forScript(script));
	}

	@Nullable
	public AttachmentSupport getAttachmentSupport() {
		return null;
	}

}
