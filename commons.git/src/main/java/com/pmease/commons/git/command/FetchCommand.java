package com.pmease.commons.git.command;

import java.io.File;

import com.google.common.base.Preconditions;
import com.pmease.commons.util.execution.Commandline;

public class FetchCommand extends GitCommand<Void> {

    private String from;
    
    private String refspec;
    
	public FetchCommand(final File repoDir) {
		super(repoDir);
	}

	public FetchCommand from(String from) {
	    this.from = from;
	    return this;
	}
	
	public FetchCommand refspec(String refspec) {
		this.refspec = refspec;
		return this;
	}
	
	@Override
	public Void call() {
	    Preconditions.checkNotNull(from, "from param has to be specified.");
	    Preconditions.checkNotNull(refspec, "refspec param has to be specified.");
	    
		Commandline cmd = cmd().addArgs("fetch");
		cmd.addArgs(from, refspec);
		
		cmd.execute(debugLogger, errorLogger).checkReturnCode();
		
		return null;
	}

}
