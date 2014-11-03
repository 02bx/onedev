package com.pmease.commons.git.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pmease.commons.git.Commit;
import com.pmease.commons.git.GitUtils;
import com.pmease.commons.util.execution.Commandline;
import com.pmease.commons.util.execution.LineConsumer;

public class LogCommand extends GitCommand<List<Commit>> {

	private static final Logger logger = LoggerFactory.getLogger(LogCommand.class); 
	
    private String fromRev;
    
    private String toRev;
    
    private String path;
    
    private int maxCount;
    
    private int skip;
    
    private boolean firstParent;

    public LogCommand(File repoDir) {
        super(repoDir);
    }

    public LogCommand fromRev(String fromRev) {
        this.fromRev = fromRev;
        return this;
    }

    public LogCommand toRev(String toRev) {
        this.toRev = toRev;
        return this;
    }
    
    public LogCommand path(String path) {
    	this.path = path;
    	return this;
    }
    
    public LogCommand maxCount(int maxCount) {
    	this.maxCount = maxCount;
    	return this;
    }

    public LogCommand skip(int skip) {
    	this.skip = skip;
    	return this;
    }
    
	public LogCommand firstParent(boolean firstParent) {
		this.firstParent = firstParent;
		return this;
	}

	@Override
    public List<Commit> call() {
        Commandline cmd = cmd();
        cmd.addArgs("log",
                        "--format=*** commit_begin ***%n%B%n*** commit_message_end ***%n%N"
                        + "*** commit_note_end ***%nhash:%H%nauthor:%aN%nauthorEmail:%aE%n"
                        + "committerEmail:%cE%ncommitter:%cN%nparents:%P%ncommitterDate:%cd %n"
                        + "authorDate:%ad %n",
                        "--date=raw");
        if (fromRev != null) {
        	if (toRev != null)
        		cmd.addArgs(fromRev + ".." + toRev);
        	else
        		cmd.addArgs(fromRev + "..");
        } else if (toRev != null) {
        	cmd.addArgs(toRev);
        }
        
        if (maxCount != 0)
        	cmd.addArgs("-" + maxCount);
        if (skip != 0)
        	cmd.addArgs("--skip=" + skip);
        
        if (firstParent)
        	cmd.addArgs("--first-parent");
        
        cmd.addArgs("--");
        if (path != null)
        	cmd.addArgs(path);

        final List<Commit> commits = new ArrayList<>();
        
        final Commit.Builder commitBuilder = Commit.builder();
        
        final AtomicBoolean commitMessageBlock = new AtomicBoolean();
        final AtomicBoolean commitNoteBlock = new AtomicBoolean();
        
        cmd.execute(new LineConsumer() {

            @Override
            public void consume(String line) {
            	if (line.equals("*** commit_begin ***")) {
            		if (commitBuilder.hash!= null) {
	            		commits.add(commitBuilder.build());
	            		commitBuilder.parentHashes.clear();
	            		commitBuilder.messageSummary = null;
	            		commitBuilder.messageBody = null;
	            		commitBuilder.note = null;
            		}
            		commitMessageBlock.set(true);
            	} else if (line.equals("*** commit_message_end ***")) {
            		commitMessageBlock.set(false);
            		commitNoteBlock.set(true);
            	} else if (line.equals("*** commit_note_end ***")) {
            		commitNoteBlock.set(false);
            	} else if (commitMessageBlock.get()) {
            		if (commitBuilder.messageSummary == null)
            			commitBuilder.messageSummary = line;
            		else if (commitBuilder.messageBody == null)
            			commitBuilder.messageBody = line;
            		else 
            			commitBuilder.messageBody += "\n" + line;
            	} else if (commitNoteBlock.get()) {
            		if (commitBuilder.note == null)
            			commitBuilder.note = line;
            		else
            			commitBuilder.note += "\n" + line;
            	} else if (line.startsWith("hash:")) {
                	commitBuilder.hash = line.substring("hash:".length());
            	} else if (line.startsWith("author:")) {
                	commitBuilder.authorName = line.substring("author:".length());
            	} else if (line.startsWith("committer:")) {
                	commitBuilder.committerName = line.substring("committer:".length());
            	} else if (line.startsWith("authorDate:")) {
                	commitBuilder.authorDate = GitUtils.parseRawDate(line.substring("authorDate:".length()).trim());
            	} else if (line.startsWith("committerDate:")) {
                	commitBuilder.committerDate = GitUtils.parseRawDate(line.substring("committerDate:".length()).trim());
            	} else if (line.startsWith("authorEmail:")) {
                	commitBuilder.authorEmail = line.substring("authorEmail:".length());
            	} else if (line.startsWith("committerEmail:")) {
                	commitBuilder.committerEmail = line.substring("committerEmail:".length());
            	} else if (line.startsWith("parents:")) {
                	for (String each: StringUtils.split(line.substring("parents:".length()), " "))
                		commitBuilder.parentHashes.add(each);
                }
            }
            
        }, new LineConsumer() {

			@Override
			public void consume(String line) {
				logger.error(line);
			}
        	
        }).checkReturnCode();

        if (commitBuilder.hash != null)
        	commits.add(commitBuilder.build());

        return commits;
    }
    
}
