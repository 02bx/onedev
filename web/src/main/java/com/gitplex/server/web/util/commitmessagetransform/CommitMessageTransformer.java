package com.gitplex.server.web.util.commitmessagetransform;

import com.gitplex.launcher.loader.ExtensionPoint;
import com.gitplex.server.entity.Depot;

@ExtensionPoint
public interface CommitMessageTransformer {
	
	String transform(Depot depot, String commitMessage);
	
}
