package com.pmease.commons.git.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jgit.lib.FileMode;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.TreeNode;
import com.pmease.commons.util.execution.Commandline;
import com.pmease.commons.util.execution.LineConsumer;

public class ListTreeCommand extends GitCommand<List<TreeNode>> {

	private String revision;
	
	private String path;
	
	private boolean recursive;
	
	public ListTreeCommand(final File repoDir) {
		super(repoDir);
	}

	public ListTreeCommand revision(String revision) {
		this.revision = revision;
		return this;
	}
	
	public ListTreeCommand path(String path) {
		this.path = path;
		return this;
	}
	
	public ListTreeCommand recursive(boolean recursive) {
		this.recursive = recursive;
		return this;
	}
	
	@Override
	public List<TreeNode> call() {
		Preconditions.checkNotNull(revision, "revision has to be specified for browse.");
		
		Commandline cmd = cmd().addArgs("ls-tree", "-l");
		if (recursive)
			cmd.addArgs("-r");
		cmd.addArgs(revision);
		if (path != null)
			cmd.addArgs(path);
		
		final List<TreeNode> treeNodes = new ArrayList<TreeNode>();
		
		cmd.execute(new LineConsumer() {

			@Override
			public void consume(String line) {
				String mode = StringUtils.substringBefore(line, " ");
				line = StringUtils.substringAfter(line, " ");
				line = StringUtils.substringAfter(line, " ");
				String hash = StringUtils.substringBefore(line, " ");
				line = StringUtils.substringAfter(line, " ");
				int size = 0;
				String sizeStr = StringUtils.substringBefore(line.trim(), "\t");
				if (NumberUtils.isNumber(sizeStr))
					size = Integer.parseInt(sizeStr);
				String path = StringUtils.substringAfter(line.trim(), "\t");
				
				treeNodes.add(new TreeNode(repoDir, FileMode.fromBits(Integer.parseInt(mode, 8)), path, revision, hash, size));
			}
			
		}, errorLogger()).checkReturnCode();

		return treeNodes;
	}

}
