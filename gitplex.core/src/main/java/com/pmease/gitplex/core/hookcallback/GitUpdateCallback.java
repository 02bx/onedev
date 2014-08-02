package com.pmease.gitplex.core.hookcallback;

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
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.util.StringUtils;
import com.pmease.gitplex.core.gatekeeper.GateKeeper;
import com.pmease.gitplex.core.gatekeeper.checkresult.Approved;
import com.pmease.gitplex.core.gatekeeper.checkresult.CheckResult;
import com.pmease.gitplex.core.gatekeeper.checkresult.Disapproved;
import com.pmease.gitplex.core.manager.BranchManager;
import com.pmease.gitplex.core.manager.UserManager;
import com.pmease.gitplex.core.model.Branch;
import com.pmease.gitplex.core.model.Repository;
import com.pmease.gitplex.core.model.User;
import com.pmease.gitplex.core.permission.ObjectPermission;

@SuppressWarnings("serial")
@Singleton
public class GitUpdateCallback extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(GitUpdateCallback.class);

	public static final String PATH = "/git-update-callback";

	private final Dao dao;
	
	private final BranchManager branchManager;

	private final UserManager userManager;
	
	@Inject
	public GitUpdateCallback(Dao dao, BranchManager branchManager, UserManager userManager) {
		this.dao = dao;
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
	
	private void checkRef(User user, Repository repository, String refName, Output output) {
		GateKeeper gateKeeper = repository.getGateKeeper();
		CheckResult checkResult = gateKeeper.checkRef(user, repository, refName);

		if (!(checkResult instanceof Approved)) {
			List<String> messages = new ArrayList<>();
			for (String each: checkResult.getReasons())
				messages.add(each);
			error(output, messages.toArray(new String[messages.size()]));
		}
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
        
        Repository repository = dao.load(Repository.class, Long.valueOf(fields.get(0)));
        
        SecurityUtils.getSubject().runAs(User.asPrincipal(Long.valueOf(fields.get(1))));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(request.getInputStream(), baos);
		
        Output output = new Output(response.getOutputStream());
        
        fields = StringUtils.splitAndTrim(new String(baos.toByteArray()), " ");
        String refName = fields.get(0);
        String oldCommitHash = fields.get(1);
        String newCommitHash = fields.get(2);
        
		logger.debug("Executing update hook against reference {}...", refName);

		User user = userManager.getCurrent();
		Preconditions.checkNotNull(user);

		if (refName.startsWith(Repository.REFS_GITPLEX)) {
			if (!user.asSubject().isPermitted(ObjectPermission.ofRepositoryAdmin(repository)))
				error(output, "Only repository administrators can update gitplex refs.");
		} else {
			String branchName = Branch.parseName(refName);
			if (branchName != null) { // push branch ref 
				if (oldCommitHash.equals(Commit.ZERO_HASH)) { // create new branch
					checkRef(user, repository, refName, output);
				} else {
					Branch branch = branchManager.findBy(repository, branchName);
					Preconditions.checkNotNull(branch);

					if (newCommitHash.equals(Commit.ZERO_HASH)) { // deleting a branch ref
						GateKeeper gateKeeper = repository.getGateKeeper();
						CheckResult checkResult = gateKeeper.checkBranch(user, branch);
						
						if (!(checkResult instanceof Approved)) {
							List<String> messages = new ArrayList<>();
							for (String each: checkResult.getReasons())
								messages.add(each);
							error(output, messages.toArray(new String[messages.size()]));
						}
					} else {
						GateKeeper gateKeeper = repository.getGateKeeper();
						CheckResult checkResult = gateKeeper.checkCommit(user, branch, newCommitHash);
				
						if (!(checkResult instanceof Approved)) {
							List<String> messages = new ArrayList<>();
							for (String each: checkResult.getReasons())
								messages.add(each);
							if (!newCommitHash.equals(Commit.ZERO_HASH) && !(checkResult instanceof Disapproved)) {
								messages.add("");
								messages.add("----------------------------------------------------");
								messages.add("You may submit a pull request instead.");
							}
							error(output, messages.toArray(new String[messages.size()]));
						}
					}
				}
			} else { // push non-branch refs
				checkRef(user, repository, refName, output);
			}
		}
	}	
}
