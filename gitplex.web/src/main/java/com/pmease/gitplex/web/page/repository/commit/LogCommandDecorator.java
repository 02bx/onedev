package com.pmease.gitplex.web.page.repository.commit;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.pmease.commons.git.command.LogCommand;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.AfterContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.AuthorContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.BeforeContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.BranchContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.CommitterContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.IdContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.MessageContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.PathContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.RevisionContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.RevisionExclusionContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.RevisionRangeContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.SingleRevisionContext;
import com.pmease.gitplex.web.page.repository.commit.CommitQueryParser.TagContext;

public class LogCommandDecorator extends CommitQueryBaseListener {

	private LogCommand logCommand;
	
	private ParseTreeProperty<String> value = new ParseTreeProperty<>();

	public LogCommandDecorator(LogCommand logCommand) {
		this.logCommand = logCommand;
	}

	@Override
	public void exitSingleRevision(SingleRevisionContext ctx) {
		logCommand.revisions().add(value.get(ctx.revision()));
	}

	@Override
	public void exitRevisionExclusion(RevisionExclusionContext ctx) {
		logCommand.revisions().add("^" + value.get(ctx.revision()));
	}

	@Override
	public void exitRevisionRange(RevisionRangeContext ctx) {
		logCommand.revisions().add(value.get(ctx.revision(0)) + ctx.Range().getText() + value.get(ctx.revision(1)));
	}

	@Override
	public void exitRevision(RevisionContext ctx) {
		if (ctx.branch() != null)
			value.put(ctx, value.get(ctx.branch()));
		else if (ctx.tag() != null)
			value.put(ctx, value.get(ctx.tag()));
		else
			value.put(ctx, value.get(ctx.id()));
	}
	
	private String getValue(TerminalNode terminal) {
		String value = terminal.getText().substring(1);
		return value.substring(0, value.length()-1);
	}

	@Override
	public void enterBranch(BranchContext ctx) {
		value.put(ctx, getValue(ctx.Value()));
	}

	@Override
	public void exitTag(TagContext ctx) {
		value.put(ctx, getValue(ctx.Value()));
	}

	@Override
	public void exitId(IdContext ctx) {
		value.put(ctx, getValue(ctx.Value()));
	}

	@Override
	public void exitAfter(AfterContext ctx) {
		logCommand.after(getValue(ctx.Value()));
	}

	@Override
	public void exitBefore(BeforeContext ctx) {
		logCommand.before(getValue(ctx.Value()));
	}
	
	@Override
	public void exitCommitter(CommitterContext ctx) {
		logCommand.committers().add(getValue(ctx.Value()));
	}
	
	@Override
	public void exitAuthor(AuthorContext ctx) {
		logCommand.authors().add(getValue(ctx.Value()));
	}
	
	@Override
	public void exitPath(PathContext ctx) {
		logCommand.paths().add(getValue(ctx.Value()));
	}
	
	@Override
	public void exitMessage(MessageContext ctx) {
		logCommand.messages().add(getValue(ctx.Value()));
	}
	
}
