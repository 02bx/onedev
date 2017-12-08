package com.gitplex.server.git;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.util.ThreadContext;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitplex.launcher.loader.ListenerRegistry;
import com.gitplex.server.event.RefUpdated;
import com.gitplex.server.manager.ProjectManager;
import com.gitplex.server.model.Project;
import com.gitplex.server.model.User;
import com.gitplex.server.persistence.UnitOfWork;
import com.gitplex.utils.StringUtils;
import com.google.common.base.Preconditions;

@SuppressWarnings("serial")
@Singleton
public class GitPostReceiveCallback extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(GitPostReceiveCallback.class);
	
    public static final String PATH = "/git-postreceive-callback";
    
    private final ProjectManager projectManager;

    private final ListenerRegistry listenerRegistry;
    
    private final UnitOfWork unitOfWork;
    
    @Inject
    public GitPostReceiveCallback(ProjectManager projectManager, UnitOfWork unitOfWork, ListenerRegistry listenerRegistry) {
    	this.projectManager = projectManager;
    	this.unitOfWork = unitOfWork;
        this.listenerRegistry = listenerRegistry;
    }

    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null) clientIp = request.getRemoteAddr();

        if (!InetAddress.getByName(clientIp).isLoopbackAddress()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Git hook callbacks can only be accessed from localhost.");
            return;
        }

        List<String> fields = StringUtils.splitAndTrim(request.getPathInfo(), "/");
        Preconditions.checkState(fields.size() == 2);
        
        Long projectId = Long.valueOf(fields.get(0));
        Long userId = Long.valueOf(fields.get(1));

        String refUpdateInfo = null;
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
        	String paramName = paramNames.nextElement();
        	if (paramName.contains(" ")) {
        		refUpdateInfo = paramName;
        	} 
        }
        Preconditions.checkState(refUpdateInfo != null, "Git ref update information is not available");
        
        /*
         * If multiple refs are updated, the hook stdin will put each ref update info into
         * a separate line, however the line breaks is omitted when forward the hook stdin
         * to curl via "@-", below logic is used to parse these info correctly even 
         * without line breaks.  
         */
        refUpdateInfo = StringUtils.reverse(StringUtils.remove(refUpdateInfo, '\n'));
        
        fields.clear();
        fields.addAll(StringUtils.splitAndTrim(refUpdateInfo, " "));
        
        unitOfWork.doAsync(new Runnable() {

			@Override
			public void run() {
				ThreadContext.bind(User.asSubject(userId));
		        try {
		            Project project = projectManager.load(projectId);
		            
			        int pos = 0;
			        while (true) {
			        	String refName = StringUtils.reverse(fields.get(pos));
			        	pos++;
			        	ObjectId newObjectId = ObjectId.fromString(StringUtils.reverse(fields.get(pos)));
			        	pos++;
			        	String field = fields.get(pos);
			        	ObjectId oldObjectId = ObjectId.fromString(StringUtils.reverse(field.substring(0, 40)));
			        	
			        	if (!newObjectId.equals(ObjectId.zeroId())) {
			        		project.cacheObjectId(refName, newObjectId);
			        	} else {
			        		newObjectId = ObjectId.zeroId();
			        		project.cacheObjectId(refName, null);
			        	}
			        	
			        	String branch = GitUtils.ref2branch(refName);
			        	if (branch != null && project.getDefaultBranch() == null) {
			        		RefUpdate refUpdate = GitUtils.getRefUpdate(project.getRepository(), "HEAD");
			        		GitUtils.linkRef(refUpdate, refName);
			        	}
			        	
			        	listenerRegistry.post(new RefUpdated(project, refName, oldObjectId, newObjectId));
			    		
			        	field = field.substring(40);
			        	if (field.length() == 0)
			        		break;
			        	else
			        		fields.set(pos, field);
			        }
		        } catch (Exception e) {
		        	logger.error("Error executing post-receive callback", e);
				} finally {
		        	ThreadContext.unbindSubject();
		        }
			}
        	
        });
	}

}
