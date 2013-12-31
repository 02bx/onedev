package com.pmease.gitop.core.hookcallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.pmease.commons.git.Commit;
import com.pmease.commons.util.StringUtils;
import com.pmease.gitop.core.manager.BranchManager;
import com.pmease.gitop.core.manager.ProjectManager;
import com.pmease.gitop.core.manager.UserManager;
import com.pmease.gitop.model.Branch;
import com.pmease.gitop.model.Project;
import com.pmease.gitop.model.PullRequest;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.gatekeeper.GateKeeper;
import com.pmease.gitop.model.gatekeeper.checkresult.Accepted;
import com.pmease.gitop.model.gatekeeper.checkresult.CheckResult;
import com.pmease.gitop.model.gatekeeper.checkresult.Rejected;
import com.pmease.gitop.model.permission.ObjectPermission;

@SuppressWarnings("serial")
@Singleton
public class GitUpdateCallback extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(GitUpdateCallback.class);

	public static final String PATH = "/git-update-callback";

	private final ProjectManager projectManager;
	
	private final BranchManager branchManager;

	private final UserManager userManager;
	
	@Inject
	public GitUpdateCallback(ProjectManager projectManager, BranchManager branchManager, UserManager userManager) {
		this.projectManager = projectManager;
		this.branchManager = branchManager;
		this.userManager = userManager;
	}
	
	private void error(Output output, String... messages) {
		output.markError();
		output.writeLine();
		output.writeLine("*******************************************************");
		output.writeLine("*");
		for (String message: messages)
			output.writeLine("*  " + message);
		output.writeLine("*");
		output.writeLine("*******************************************************");
		output.writeLine();
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
        
        Project project = projectManager.load(Long.valueOf(fields.get(0)));
        
        SecurityUtils.getSubject().runAs(User.asPrincipal(Long.valueOf(fields.get(1))));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(request.getInputStream(), baos);
		
        Output output = new Output(response.getOutputStream());
        
        fields = StringUtils.splitAndTrim(new String(baos.toByteArray()), " ");
        String refName = fields.get(0);
        String oldCommitHash = fields.get(1);
        String newCommitHash = fields.get(2);
        
		if (refName.startsWith(Project.REFS_GITOP)) {
			if (!SecurityUtils.getSubject().isPermitted(ObjectPermission.ofProjectAdmin(project)))
				error(output, "Only project administrators can update gitop refs.");
		} else {
			String branchName = Branch.getName(refName);
			if (branchName != null) {
				if (!oldCommitHash.equals(Commit.ZERO_HASH)) {
					Branch branch = branchManager.findBy(project, branchName);
					Preconditions.checkNotNull(branch);

					logger.info("Executing pre-receive hook against branch {}...", branchName);
					
					User user = userManager.getCurrent();
					Preconditions.checkNotNull(user);
			
					GateKeeper gateKeeper = project.getGateKeeper();
					CheckResult checkResult;
					if (newCommitHash.equals(Commit.ZERO_HASH))
						checkResult = gateKeeper.checkFile(user, branch, null);
					else
						checkResult = gateKeeper.checkCommit(user, branch, newCommitHash);
			
					if (!(checkResult instanceof Accepted)) {
						List<String> messages = new ArrayList<>();
						for (String each: checkResult.getReasons())
							messages.add(each);
						if (!newCommitHash.equals(Commit.ZERO_HASH) && !(checkResult instanceof Rejected)) {
							messages.add("");
							messages.add("----------------------------------------------------");
							messages.add("You may submit a pull request instead.");
						}
						error(output, messages.toArray(new String[messages.size()]));
					} else {
						for (PullRequest each: branch.getIngoingRequests()) {
							if (each.isOpen()) {
								error(output, "There are unclosed pull requests targeting this branch.", 
										"Please close them before continue.");
								return;
							}
						}
						for (PullRequest each: branch.getOutgoingRequests()) {
							if (each.isOpen()) {
								error(output, "There are unclosed pull requests originating from this branch.", 
										"Please close them before continue.");
								break;
							}
						}
					}
				}
			}
		}
	}	
}
