package com.pmease.gitplex.web.component.diff.blob.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.time.Duration;

import com.pmease.commons.git.BlobChange;
import com.pmease.commons.git.BlobIdent;
import com.pmease.commons.lang.diff.AroundContext;
import com.pmease.commons.lang.diff.DiffBlock;
import com.pmease.commons.lang.diff.DiffMatchPatch.Operation;
import com.pmease.commons.lang.diff.DiffUtils;
import com.pmease.commons.lang.diff.LineModification;
import com.pmease.commons.lang.diff.TokenDiffBlock;
import com.pmease.commons.lang.tokenizers.CmToken;
import com.pmease.commons.util.StringUtils;
import com.pmease.commons.wicket.behavior.DirtyIgnoreBehavior;
import com.pmease.commons.wicket.component.feedback.FeedbackPanel;
import com.pmease.gitplex.core.comment.InlineComment;
import com.pmease.gitplex.core.comment.InlineCommentSupport;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.web.Constants;
import com.pmease.gitplex.web.component.comment.CommentInput;
import com.pmease.gitplex.web.component.comment.CommentRemoved;
import com.pmease.gitplex.web.component.diff.diffstat.DiffStatBar;
import com.pmease.gitplex.web.component.diff.difftitle.BlobDiffTitle;
import com.pmease.gitplex.web.component.diff.revision.DiffMode;
import com.pmease.gitplex.web.page.repository.file.RepoFilePage;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;

@SuppressWarnings("serial")
public class TextDiffPanel extends Panel {

	private final IModel<Repository> repoModel;
	
	private final BlobChange change;
	
	private final Map<Integer, Integer> contextSizes = new HashMap<>();
	
	private final DiffMode diffMode;
	
	private final InlineCommentSupport commentSupport;
	
	private final IModel<List<DiffComment>> commentsModel = new LoadableDetachableModel<List<DiffComment>>() {

		@Override
		protected List<DiffComment> load() {
			List<DiffComment> comments = new ArrayList<>();
			for (Map.Entry<Integer, List<InlineComment>> entry: 
					commentSupport.getComments(change.getOldBlobIdent()).entrySet()) {
				for (InlineComment inline: entry.getValue()) {
					DiffComment comment = new DiffComment();
					comment.inline = inline;
					comment.oldLineNo = entry.getKey();
					comment.newLineNo = -1;
				}
			}
			for (Map.Entry<Integer, List<InlineComment>> entry: 
					commentSupport.getComments(change.getNewBlobIdent()).entrySet()) {
				for (InlineComment inline: entry.getValue()) {
					DiffComment comment = new DiffComment();
					comment.inline = inline;
					comment.oldLineNo = -1;
					comment.newLineNo = entry.getKey();
				}
			}
			Collections.sort(comments, new Comparator<DiffComment>() {

				@Override
				public int compare(DiffComment comment1, DiffComment comment2) {
					return comment1.inline.getDate().compareTo(comment2.inline.getDate());
				}
				
			});
			return comments;
		}
		
	};
	
	private AbstractDefaultAjaxBehavior addCommentBehavior;
	
	private RepeatingView newCommentForms;
	
	private RepeatingView commentRows;
	
	public TextDiffPanel(String id, IModel<Repository> repoModel, BlobChange change, DiffMode diffMode, 
			@Nullable InlineCommentSupport commentSupport) {
		super(id);
		
		this.repoModel = repoModel;
		this.change = change;
		this.diffMode = diffMode;
		this.commentSupport = commentSupport;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		add(new DiffStatBar("diffStat", change.getAdditions(), change.getDeletions(), true));
		add(new BlobDiffTitle("title", change));
		
		PageParameters params = RepoFilePage.paramsOf(repoModel.getObject(), change.getBlobIdent());
		add(new BookmarkablePageLink<Void>("viewFile", RepoFilePage.class, params));
		
		add(new Label("diffLines", renderDiffs()).setEscapeModelStrings(false));
		
		add(new AbstractDefaultAjaxBehavior() {
			
			@Override
			protected void respond(AjaxRequestTarget target) {
				IRequestParameters params = RequestCycle.get().getRequest().getQueryParameters();
				int index = params.getParameterValue("index").toInt();
				Integer lastContextSize = contextSizes.get(index);
				if (lastContextSize == null)
					lastContextSize = Constants.DIFF_DEFAULT_CONTEXT_SIZE;
				int contextSize = lastContextSize + Constants.DIFF_EXPAND_CONTEXT_SIZE;
				contextSizes.put(index, contextSize);
				
				StringBuilder builder = new StringBuilder();
				appendEquals(builder, index, lastContextSize, contextSize);
				
				String expanded = StringUtils.replace(builder.toString(), "\"", "\\\"");
				expanded = StringUtils.replace(expanded, "\n", "");
				String script = String.format("$('#%s .expander%d').replaceWith(\"%s\");", 
						getMarkupId(), index, expanded);
				target.appendJavaScript(script);
			}

			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				super.renderHead(component, response);
				
				String script = String.format("$('#%s')[0].expander = %s;", 
						getMarkupId(), getCallbackFunction(CallbackParameter.explicit("index")));
				response.render(OnDomReadyHeaderItem.forScript(script));
			}
			
		});
		
		add(addCommentBehavior = new AbstractDefaultAjaxBehavior() {
			
			@Override
			protected void respond(AjaxRequestTarget target) {
				IRequestParameters params = RequestCycle.get().getRequest().getQueryParameters();
				final int oldLineNo = params.getParameterValue("oldLineNo").toInt();
				final int newLineNo = params.getParameterValue("newLineNo").toInt();
				
				final Form<?> newCommentForm = new Form<Void>(newCommentForms.newChildId());
				newCommentForm.setOutputMarkupId(true);
				
				final CommentInput input;
				newCommentForm.add(input = new CommentInput("input", Model.of("")));
				input.setRequired(true);
				newCommentForm.add(new FeedbackPanel("feedback", input).hideAfter(Duration.seconds(5)));
				
				newCommentForm.add(new AjaxLink<Void>("cancel") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						newCommentForm.remove();
						String script = String.format("$('#%s').closest('.line').remove();", newCommentForm.getMarkupId());
						target.appendJavaScript(script);
					}
					
				}.add(new DirtyIgnoreBehavior()));
				
				newCommentForm.add(new AjaxSubmitLink("save") {

					@Override
					protected void onError(AjaxRequestTarget target, Form<?> form) {
						super.onError(target, form);
						target.add(form);
					}

					@Override
					protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
						super.onSubmit(target, form);
						
						BlobIdent commentAt;
						BlobIdent compareWith;
						int lineNo;
						AroundContext commentContext; 
						
						if (newLineNo == -1) {
							commentAt = change.getOldBlobIdent();
							compareWith = change.getNewBlobIdent();
							lineNo = oldLineNo;
						} else {
							commentAt = change.getNewBlobIdent();
							compareWith = change.getOldBlobIdent();
							lineNo = newLineNo;
						}
						commentContext = DiffUtils.around(change.getDiffBlocks(), oldLineNo, newLineNo, 
								Constants.LINE_COMMENT_CONTEXT_SIZE); 
						
						commentSupport.addComment(commentAt, compareWith, commentContext, 
								lineNo, input.getModelObject());
						
 						Component commentsRow = newCommentsRow(comments.newChildId(), index);
						comments.add(commentsRow);
						target.add(commentsRow);
						
						String prependScript = String.format("$('#comments-placeholder').append('<table id=\"%s\"></table>')", 
								commentsRow.getMarkupId());
						target.prependJavaScript(prependScript);
						target.appendJavaScript(String.format("gitplex.textdiff.afterAddComment(%d);", index));
					}

				}.add(new DirtyIgnoreBehavior()));
				
				newCommentForms.add(newCommentForm);
				
				String script = String.format("gitplex.textdiff.beforeAddComment('%s', '%s', %d, %d);", 
						getMarkupId(), newCommentForm.getMarkupId(), oldLineNo, newLineNo);
				target.prependJavaScript(script);
				
				target.add(newCommentForm);
			}
			
			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				super.renderHead(component, response);
				CharSequence addCommentCallback = addCommentBehavior.getCallbackFunction(
						CallbackParameter.explicit("oldLineNo"), CallbackParameter.explicit("newLineNo"));
				String script = String.format("document.getElementById('%s').addComment=%s;", 
						getMarkupId(), addCommentCallback);
				response.render(OnDomReadyHeaderItem.forScript(script));
			}

		});
		
		add(newCommentForms = new RepeatingView("newComments"));
		
		setOutputMarkupId(true);
	}
	
	private void appendEquals(StringBuilder builder, int index, int lastContextSize, int contextSize) {
		DiffBlock block = change.getDiffBlocks().get(index);
		if (index == 0) {
			int start = block.getLines().size()-contextSize;
			if (start < 0)
				start=0;
			else if (start > 0)
				appendExpander(builder, index, start);
			for (int j=start; j<block.getLines().size()-lastContextSize; j++) 
				appendEqual(builder, block, j, lastContextSize);
		} else if (index == change.getDiffBlocks().size()-1) {
			int end = block.getLines().size();
			int skipped = 0;
			if (end > contextSize) {
				skipped = end-contextSize;
				end = contextSize;
			}
			for (int j=lastContextSize; j<end; j++)
				appendEqual(builder, block, j, lastContextSize);
			if (skipped != 0)
				appendExpander(builder, index, skipped);
		} else if (2*contextSize < block.getLines().size()) {
			for (int j=lastContextSize; j<contextSize; j++)
				appendEqual(builder, block, j, lastContextSize);
			appendExpander(builder, index, block.getLines().size() - 2*contextSize);
			for (int j=block.getLines().size()-contextSize; j<block.getLines().size()-lastContextSize; j++)
				appendEqual(builder, block, j, lastContextSize);
		} else {
			for (int j=lastContextSize; j<block.getLines().size()-lastContextSize; j++)
				appendEqual(builder, block, j, lastContextSize);
		}
	}
	
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.render(JavaScriptHeaderItem.forReference(
				new JavaScriptResourceReference(TextDiffPanel.class, "text-diff.js")));
		response.render(CssHeaderItem.forReference(
				new WebjarsCssResourceReference("codemirror/current/theme/eclipse.css")));
		response.render(CssHeaderItem.forReference(
				new CssResourceReference(TextDiffPanel.class, "text-diff.css")));
		
		CharSequence addCommentCallback = addCommentBehavior.getCallbackFunction(
				CallbackParameter.explicit("oldLineNo"), CallbackParameter.explicit("newLineNo"));
		String script = String.format("document.getElementById('%s').addComment=%s;", 
				getMarkupId(), addCommentCallback);
		response.render(OnDomReadyHeaderItem.forScript(script));
	}

	private String renderDiffs() {
		int contextSize = Constants.DIFF_DEFAULT_CONTEXT_SIZE;
		StringBuilder builder = new StringBuilder();
		builder.append("<colgroup><col width='40'></col>");
		if (diffMode == DiffMode.UNIFIED)
			builder.append("<col width='40'></col>");
		else
			builder.append("</col><col></col><col width=40></col>");
		builder.append("<col></col></colgroup>");
		for (int i=0; i<change.getDiffBlocks().size(); i++) {
			DiffBlock block = change.getDiffBlocks().get(i);
			if (block.getOperation() == Operation.EQUAL) {
				appendEquals(builder, i, 0, contextSize);
			} else if (block.getOperation() == Operation.DELETE) {
				if (i+1<change.getDiffBlocks().size()) {
					DiffBlock nextBlock = change.getDiffBlocks().get(i+1);
					if (nextBlock.getOperation() == Operation.INSERT) {
						LinkedHashMap<Integer, LineModification> lineChanges = DiffUtils.calcLineChange(block, nextBlock);
						int prevDeleteLineIndex = 0;
						int prevInsertLineIndex = 0;
						for (Map.Entry<Integer, LineModification> entry: lineChanges.entrySet()) {
							int deleteLineIndex = entry.getKey();
							LineModification lineChange = entry.getValue();
							int insertLineIndex = lineChange.getCompareLine();
							
							appendDeletesAndInserts(builder, block, nextBlock, prevDeleteLineIndex, deleteLineIndex, 
									prevInsertLineIndex, insertLineIndex);
							
							appendModification(builder, block, nextBlock, deleteLineIndex, insertLineIndex, lineChange.getTokenDiffs()); 
							
							prevDeleteLineIndex = deleteLineIndex+1;
							prevInsertLineIndex = insertLineIndex+1;
						}
						appendDeletesAndInserts(builder, block, nextBlock, prevDeleteLineIndex, block.getLines().size(), 
								prevInsertLineIndex, nextBlock.getLines().size());
						i++;
					} else {
						for (int j=0; j<block.getLines().size(); j++) 
							appendDelete(builder, block, j);
					}
				} else {
					for (int j=0; j<block.getLines().size(); j++) 
						appendDelete(builder, block, j);
				}
			} else {
				for (int j=0; j<block.getLines().size(); j++) 
					appendInsert(builder, block, j);
			}
		}
		return builder.toString();
	}

	private void appendDeletesAndInserts(StringBuilder builder, DiffBlock deleteBlock, DiffBlock insertBlock, 
			int fromDeleteLineIndex, int toDeleteLineIndex, int fromInsertLineIndex, int toInsertLineIndex) {
		if (diffMode == DiffMode.UNIFIED) {
			for (int i=fromDeleteLineIndex; i<toDeleteLineIndex; i++)
				appendDelete(builder, deleteBlock, i);
			for (int i=fromInsertLineIndex; i<toInsertLineIndex; i++)
				appendInsert(builder, insertBlock, i);
		} else {
			int deleteSize = toDeleteLineIndex - fromDeleteLineIndex;
			int insertSize = toInsertLineIndex - fromInsertLineIndex;
			if (deleteSize < insertSize) {
				for (int i=fromDeleteLineIndex; i<toDeleteLineIndex; i++) 
					appendSideBySide(builder, deleteBlock, insertBlock, i, i-fromDeleteLineIndex+fromInsertLineIndex);
				for (int i=fromInsertLineIndex+deleteSize; i<toInsertLineIndex; i++)
					appendInsert(builder, insertBlock, i);
			} else {
				for (int i=fromInsertLineIndex; i<toInsertLineIndex; i++) 
					appendSideBySide(builder, deleteBlock, insertBlock, i-fromInsertLineIndex+fromDeleteLineIndex, i);
				for (int i=fromDeleteLineIndex+insertSize; i<toDeleteLineIndex; i++)
					appendDelete(builder, deleteBlock, i);
			}
		}
	}
	
	private void appendAddComment(StringBuilder builder, int oldLineNo, int newLineNo) {
		builder.append("<span class='add-comment'>");
		String script = String.format("document.getElementById('%s').addComment(%d, %d);", 
				getMarkupId(), oldLineNo, newLineNo);
		builder.append("<a href=\"javascript:").append(script).append("\">").append("<i class='fa fa-plus'></i></a></span>");
	}
	
	private void appendEqual(StringBuilder builder, DiffBlock block, int lineIndex, int lastContextSize) {
		if (lastContextSize != 0)
			builder.append("<tr class='line expanded'>");
		else
			builder.append("<tr class='line'>");

		int oldLineNo = block.getOldStart() + lineIndex;
		int newLineNo = block.getNewStart() + lineIndex;
		StringBuilder contentBuilder = new StringBuilder();
		contentBuilder.append("<td class='content old").append(oldLineNo).append(" new").append(newLineNo).append("'>");
		if (lastContextSize == 0)
			appendAddComment(contentBuilder, oldLineNo, newLineNo);
		contentBuilder.append("<span class='operation'>&nbsp;</span>");
		for (CmToken token: block.getLines().get(lineIndex))
			contentBuilder.append(token.toHtml(Operation.EQUAL));
		contentBuilder.append("</td>");
		
		if (diffMode == DiffMode.UNIFIED) {
			builder.append("<td class='number'>").append(oldLineNo+1).append("</td>");
			builder.append("<td class='number'>").append(newLineNo+1).append("</td>");
			builder.append(contentBuilder);
		} else {
			builder.append("<td class='number'>").append(oldLineNo+1).append("</td>");
			builder.append(contentBuilder);
			builder.append("<td class='number'>").append(newLineNo+1).append("</td>");
			builder.append(contentBuilder);
		}
		builder.append("</tr>");
	}
	
	private void appendInsert(StringBuilder builder, DiffBlock block, int lineIndex) {
		builder.append("<tr class='line'>");

		int newLineNo = block.getNewStart() + lineIndex;
		StringBuilder contentBuilder = new StringBuilder();
		contentBuilder.append("<td class='content new new").append(newLineNo).append("'>");
		appendAddComment(contentBuilder, -1, newLineNo);
		contentBuilder.append("<span class='operation'>+</span>");
		List<CmToken> tokens = block.getLines().get(lineIndex);
		for (int i=0; i<tokens.size(); i++) 
			contentBuilder.append(tokens.get(i).toHtml(Operation.EQUAL));
		contentBuilder.append("</td>");
		
		if (diffMode == DiffMode.UNIFIED) {
			builder.append("<td class='number new'>&nbsp;</td>");
			builder.append("<td class='number new'>").append(newLineNo+1).append("</td>");
			builder.append(contentBuilder);
		} else {
			builder.append("<td class='number'>&nbsp;</td><td class='content'>&nbsp;</td>");
			builder.append("<td class='number new'>").append(newLineNo+1).append("</td>");
			builder.append(contentBuilder);
		}
		builder.append("</tr>");
	}
	
	private void appendDelete(StringBuilder builder, DiffBlock block, int lineIndex) {
		builder.append("<tr class='line'>");
		
		int oldLineNo = block.getOldStart() + lineIndex;
		StringBuilder contentBuilder = new StringBuilder();
		contentBuilder.append("<td class='content old old").append(oldLineNo).append("'>");
		appendAddComment(contentBuilder, oldLineNo, -1);
		contentBuilder.append("<span class='operation'>-</span>");
		List<CmToken> tokens = block.getLines().get(lineIndex);
		for (int i=0; i<tokens.size(); i++) 
			contentBuilder.append(tokens.get(i).toHtml(Operation.EQUAL));
		contentBuilder.append("</td>");
		
		if (diffMode == DiffMode.UNIFIED) {
			builder.append("<td class='number old'>").append(oldLineNo+1).append("</td>");
			builder.append("<td class='number old'>&nbsp;</td>");
			builder.append(contentBuilder);
		} else {
			builder.append("<td class='number old'>").append(oldLineNo+1).append("</td>");
			builder.append(contentBuilder);
			builder.append("<td class='number'>&nbsp;</td><td class='content'>&nbsp;</td>");
		}
		builder.append("</tr>");
	}
	
	private void appendSideBySide(StringBuilder builder, DiffBlock deleteBlock, DiffBlock insertBlock, 
			int deleteLineIndex, int insertLineIndex) {
		builder.append("<tr class='line'>");

		int oldLineNo = deleteBlock.getOldStart()+deleteLineIndex;
		builder.append("<td class='number old'>").append(oldLineNo+1).append("</td>");
		builder.append("<td class='content old old").append(oldLineNo).append("'>");
		appendAddComment(builder, oldLineNo, -1);
		builder.append("<span class='operation'>-</span>");
		for (CmToken token: deleteBlock.getLines().get(deleteLineIndex))
			builder.append(token.toHtml(Operation.EQUAL));
		builder.append("</td>");
		
		int newLineNo = insertBlock.getNewStart()+insertLineIndex;
		builder.append("<td class='number new'>").append(newLineNo+1).append("</td>");
		builder.append("<td class='content new new").append(newLineNo).append("'>");
		appendAddComment(builder, -1, newLineNo);
		builder.append("<span class='operation'>+</span>");
		for (CmToken token: insertBlock.getLines().get(insertLineIndex))
			builder.append(token.toHtml(Operation.EQUAL));
		builder.append("</td>");
		
		builder.append("</tr>");
	}

	private void appendModification(StringBuilder builder, DiffBlock deleteBlock, DiffBlock insertBlock, 
			int deleteLineIndex, int insertLineIndex, List<TokenDiffBlock> tokenDiffs) {
		builder.append("<tr class='line'>");

		int oldLineNo = deleteBlock.getOldStart() + deleteLineIndex;
		int newLineNo = insertBlock.getNewStart() + insertLineIndex;
		if (diffMode == DiffMode.UNIFIED) {
			builder.append("<td class='number old new'>").append(oldLineNo+1).append("</td>");
			builder.append("<td class='number old new'>").append(newLineNo+1).append("</td>");
			builder.append("<td class='content old new old").append(oldLineNo).append(" new").append(newLineNo).append("'>");
			appendAddComment(builder, oldLineNo, newLineNo);
			builder.append("<span class='operation'>*</span>");
			for (TokenDiffBlock tokenBlock: tokenDiffs) { 
				for (CmToken token: tokenBlock.getTokens()) 
					builder.append(token.toHtml(tokenBlock.getOperation()));
			}
			builder.append("</td>");
		} else {
			builder.append("<td class='number old'>").append(oldLineNo+1).append("</td>");
			builder.append("<td class='content old old").append(oldLineNo).append("'>");
			appendAddComment(builder, oldLineNo, -1);
			builder.append("<span class='operation'>-</span>");
			for (TokenDiffBlock tokenBlock: tokenDiffs) { 
				for (CmToken token: tokenBlock.getTokens()) {
					if (tokenBlock.getOperation() != Operation.INSERT) 
						builder.append(token.toHtml(tokenBlock.getOperation()));
				}
			}
			builder.append("</td>");
			
			builder.append("<td class='number new'>").append(newLineNo+1).append("</td>");
			builder.append("<td class='content new new").append(newLineNo).append("'>");
			appendAddComment(builder, -1, newLineNo);
			builder.append("<span class='operation'>+</span>");
			for (TokenDiffBlock tokenBlock: tokenDiffs) { 
				for (CmToken token: tokenBlock.getTokens()) {
					if (tokenBlock.getOperation() != Operation.DELETE) 
						builder.append(token.toHtml(tokenBlock.getOperation()));
				}
			}
			builder.append("</td>");
		}
		
		builder.append("</tr>");
	}
	
	private void appendExpander(StringBuilder builder, int blockIndex, int skippedLines) {
		builder.append("<tr class='expander expander").append(blockIndex).append("'>");
		
		String script = String.format("javascript: $('#%s')[0].expander(%d);", getMarkupId(), blockIndex);
		if (diffMode == DiffMode.UNIFIED) {
			builder.append("<td colspan='2' class='expander'><a title='Show more lines' href=\"")
					.append(script).append("\"><i class='fa fa-sort'></i></a></td>");
			builder.append("<td class='skipped'><i class='fa fa-ellipsis-h'></i> skipped ")
					.append(skippedLines).append(" lines <i class='fa fa-ellipsis-h'></i></td>");
		} else {
			builder.append("<td class='expander'><a title='Show more lines' href=\"").append(script)
					.append("\"><i class='fa fa-sort'></i></a></td>");
			builder.append("<td class='skipped' colspan='3'><i class='fa fa-ellipsis-h'></i> skipped ")
					.append(skippedLines).append(" lines <i class='fa fa-ellipsis-h'></i></td>");
		}
		builder.append("</tr>");
	}
	
	private Component newCommentRows() {
		commentRows = new RepeatingView("commentLines");
		
		if (commentSupport != null) {
			for (int index: commentSupport.getComments(blobIdent).keySet()) 
				commentRows.add(newCommentsRow(commentsView.newChildId(), index));
			for (int index: commentSupportsModel.getObject().keySet()) 
				commentRows.add(newCommentsRow(commentsView.newChildId(), index));
		}
		
		return commentRows;
	}
	
	private Component newCommentRow(String id, final int oldLineNo, final int newLineNo) {
		WebMarkupContainer row = new WebMarkupContainer(commentLines.newChildId()) {

			@Override
			public void onEvent(IEvent<?> event) {
				super.onEvent(event);
				
				if (event.getPayload() instanceof CommentRemoved) {
					CommentRemoved commentRemoved = (CommentRemoved) event.getPayload();
					commentRows.remove(this);
					String script = String.format("$('#%s').closest('tr').remove();", 
							TextDiffPanel.this.getMarkupId(), oldLineNo, newLineNo);
					commentRemoved.getTarget().appendJavaScript(script);
				} 
			}
			
		};
		row.setOutputMarkupId(true);
		row.setMarkupId("comment-diffline-" + index);
		
		row.add(new ListView<InlineComment>("comments", new LoadableDetachableModel<List<InlineComment>>() {

			@Override
			protected List<InlineComment> load() {
				return commentsModel.getObject().get(index); 
			}
			
		}) {

			@Override
			protected void populateItem(ListItem<InlineComment> item) {
				item.add(new UserLink("avatar", new UserModel(item.getModelObject().getUser()), AvatarMode.AVATAR));
				item.add(new CommentPanel("comment", item.getModel()).setOutputMarkupId(true));
				if (item.getModelObject().equals(commentSupport.getConcernedComment()))
					item.add(AttributeAppender.append("class", " concerned"));
			}
			
		});
		
		return row;
	}
	
	@Override
	protected void onBeforeRender() {
		replace(newCommentLines());
		
		super.onBeforeRender();
	}
	
	@Override
	protected void onDetach() {
		repoModel.detach();
		commentsModel.detach();
		super.onDetach();
	}

}
