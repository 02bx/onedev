package com.pmease.commons.git.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.FileMode;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.FileChange;
import com.pmease.commons.git.FileChange.Action;
import com.pmease.commons.git.FileChangeWithDiffs;
import com.pmease.commons.util.diff.DiffUtils;
import com.pmease.commons.util.execution.Commandline;
import com.pmease.commons.util.execution.LineConsumer;

public class DiffCommand extends GitCommand<List<FileChangeWithDiffs>> {

	private String revisions;
	
	private String path;
	
	private int contextLines;
	
	public DiffCommand(File repoDir) {
		super(repoDir);
	}
	
	public DiffCommand revisions(String revisions) {
		this.revisions = revisions;
		return this;
	}
	
	public DiffCommand path(String path) {
		this.path = path;
		return this;
	}
	
	public DiffCommand contextLines(int contextLines) {
		this.contextLines = contextLines;
		return this;
	}

	@Override
	public List<FileChangeWithDiffs> call() {
		Preconditions.checkNotNull(revisions, "revisions has to be specified.");
		
		Commandline cmd = cmd();
		cmd.addArgs("diff", revisions, "--full-index", "--no-color", "--find-renames", 
				"--find-copies", "--src-prefix=#gitplex_old/", "--dst-prefix=#gitplex_new/");
		if (contextLines != 0)
			cmd.addArgs("--unified=" + contextLines);
		if (path != null)
			cmd.addArgs("--", path);
		
		final List<FileChangeWithDiffs> fileChanges = new ArrayList<FileChangeWithDiffs>();
		final ChangeBuilder changeBuilder = new ChangeBuilder();
		
		cmd.execute(new LineConsumer() {

			@Override
			public void consume(String line) {
				if (line.startsWith("diff --git")) {
					if (changeBuilder.newPath != null) 
						fileChanges.add(changeBuilder.buildFileChange());

					changeBuilder.action = FileChange.Action.MODIFY;
					changeBuilder.binary = false;
					changeBuilder.mode = FileMode.MISSING;
					changeBuilder.oldCommit = null;
					changeBuilder.newCommit = null;
					changeBuilder.diffLines.clear();
					
					line = line.substring("diff --git #gitplex_old/".length());
					
					changeBuilder.oldPath = StringUtils.substringBefore(line, " #gitplex_new/");
					changeBuilder.newPath = StringUtils.substringAfter(line, " #gitplex_new/");
				} else if (line.startsWith("deleted file mode ")) {
					changeBuilder.action = FileChange.Action.DELETE;
					changeBuilder.mode = FileMode.fromBits(Integer.parseInt(line.substring("deleted file mode ".length()), 8));
				} else if (line.startsWith("new file mode ")) {
					changeBuilder.action = FileChange.Action.ADD;
					changeBuilder.mode = FileMode.fromBits(Integer.parseInt(line.substring("new file mode ".length()), 8));
				} else if (line.startsWith("Binary files")) {
					changeBuilder.binary = true;
				} else if (line.startsWith("rename from ") || line.startsWith("rename to ")) {
					changeBuilder.action = FileChange.Action.RENAME;
				} else if (line.startsWith("copy from ") || line.startsWith("copy to ")) {
					changeBuilder.action = FileChange.Action.COPY;
				} else if (line.startsWith("index ")) {
					line = line.substring("index ".length());
					changeBuilder.oldCommit = StringUtils.substringBefore(line, "..");
					changeBuilder.newCommit = StringUtils.substringAfter(line, "..");
					if (changeBuilder.newCommit.indexOf(' ') != -1) {
						changeBuilder.newCommit = StringUtils.substringBefore(changeBuilder.newCommit, " ");
						changeBuilder.mode = FileMode.fromBits(Integer.parseInt(StringUtils.substringAfterLast(line, " "), 8));
					}
				} else if (line.startsWith("@@") || line.startsWith("+") || line.startsWith("-") 
						|| line.startsWith(" ") || line.startsWith("\\")) {
					changeBuilder.diffLines.add(line);
				}
					
			}
			
		}, new LineConsumer() {

			@Override
			public void consume(String line) {
				if (line.startsWith("warning: "))
					warn(line.substring("warning: ".length()));
				else if (line.startsWith("The file will have its original line endings"))
					warn(line);
				else if (line.startsWith("The file will have its original line endings in your working directory"))
					warn(line);
				else
					error(line);
			}
			
		}).checkReturnCode();

		if (changeBuilder.newPath != null)
			fileChanges.add(changeBuilder.buildFileChange());
		
		return fileChanges;
	}

	private static class ChangeBuilder {
		private Action action;
		
		private String oldPath;
		
		private String newPath;
		
		private boolean binary;
		
		private FileMode mode;
		
		private String oldCommit;
		
		private String newCommit;
		
		private List<String> diffLines = new ArrayList<>();
		
		private FileChangeWithDiffs buildFileChange() {
			return new FileChangeWithDiffs(action, oldPath, newPath, mode, binary, 
					oldCommit, newCommit, DiffUtils.parseUnifiedDiff(diffLines));
		}
	}
}
