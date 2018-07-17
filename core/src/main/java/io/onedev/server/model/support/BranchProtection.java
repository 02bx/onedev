package io.onedev.server.model.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.base.Preconditions;

import io.onedev.server.model.Group;
import io.onedev.server.model.User;
import io.onedev.server.model.support.ifsubmittedby.Anyone;
import io.onedev.server.model.support.ifsubmittedby.IfSubmittedBy;
import io.onedev.server.model.support.ifsubmittedby.SpecifiedGroup;
import io.onedev.server.model.support.ifsubmittedby.SpecifiedUser;
import io.onedev.server.util.reviewrequirement.ReviewRequirement;
import io.onedev.server.web.editable.annotation.BranchPattern;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.ReviewRequirementSpec;
import io.onedev.server.web.editable.annotation.ConfigurationChoice;
import io.onedev.utils.PathUtils;

@Editable
public class BranchProtection implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean enabled = true;
	
	private String branch;
	
	private IfSubmittedBy submitter = new Anyone();
	
	private boolean noForcedPush = true;
	
	private boolean noDeletion = true;
	
	private boolean noCreation = true;
	
	private String reviewRequirementSpec;
	
	private List<String> configurations = new ArrayList<>();
	
	private boolean buildMerges;
	
	private transient Optional<ReviewRequirement> reviewRequirementOpt;
	
	private List<FileProtection> fileProtections = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Editable(order=100, description="Specify branch to be protected. Wildcard may be used to "
			+ "specify multiple branches")
	@BranchPattern
	@NotEmpty
	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	@Editable(order=150, name="If Submitted By", description="This protection rule will apply "
			+ "only if the change is submitted by specified users here")
	@NotNull(message="may not be empty")
	public IfSubmittedBy getSubmitter() {
		return submitter;
	}

	public void setSubmitter(IfSubmittedBy submitter) {
		this.submitter = submitter;
	}

	@Editable(order=200, description="Check this to not allow forced push")
	public boolean isNoForcedPush() {
		return noForcedPush;
	}

	public void setNoForcedPush(boolean noForcedPush) {
		this.noForcedPush = noForcedPush;
	}

	@Editable(order=300, description="Check this to not allow branch deletion")
	public boolean isNoDeletion() {
		return noDeletion;
	}

	public void setNoDeletion(boolean noDeletion) {
		this.noDeletion = noDeletion;
	}

	@Editable(order=350, description="Check this to not allow branch creation")
	public boolean isNoCreation() {
		return noCreation;
	}

	public void setNoCreation(boolean noCreation) {
		this.noCreation = noCreation;
	}

	@Editable(order=400, name="Required Reviewers", description="Optionally specify required reviewers for changes of "
			+ "specified branch. OneDev assumes that the user submitting the change has completed the review already")
	@ReviewRequirementSpec
	public String getReviewRequirementSpec() {
		return reviewRequirementSpec;
	}

	public void setReviewRequirementSpec(String reviewRequirementSpec) {
		this.reviewRequirementSpec = reviewRequirementSpec;
	}

	@Editable(order=500, name="Required Builds", description="Optionally choose required builds")
	@ConfigurationChoice
	public List<String> getConfigurations() {
		return configurations;
	}

	public void setConfigurations(List<String> configurations) {
		this.configurations = configurations;
	}

	@Editable(order=600, name="Build Merged Commits", description="If checked, builds of merged commits "
			+ "(instead of head commits) are required when pull request is involved")
	public boolean isBuildMerges() {
		return buildMerges;
	}

	public void setBuildMerges(boolean buildMerges) {
		this.buildMerges = buildMerges;
	}

	@Editable(order=700, description="Optionally specify additional users to review particular paths. For each changed file, "
			+ "the first matched file protection setting will be used")
	@NotNull(message="may not be empty")
	public List<FileProtection> getFileProtections() {
		return fileProtections;
	}

	public void setFileProtections(List<FileProtection> fileProtections) {
		this.fileProtections = fileProtections;
	}
	
	@Nullable
	public FileProtection getFileProtection(String file) {
		for (FileProtection protection: fileProtections) {
			if (PathUtils.matchChildAware(protection.getPath(), file))
				return protection;
		}
		return null;
	}
	
	@Nullable
	public ReviewRequirement getReviewRequirement() {
		if (reviewRequirementOpt == null) {
			if (reviewRequirementSpec != null)
				reviewRequirementOpt = Optional.of(new ReviewRequirement(reviewRequirementSpec));
			else
				reviewRequirementOpt = Optional.empty();
		}
		return reviewRequirementOpt.orElse(null);
	}
	
	public void onRenameGroup(String oldName, String newName) {
		if (getSubmitter() instanceof SpecifiedGroup) {
			SpecifiedGroup specifiedGroup = (SpecifiedGroup) getSubmitter();
			if (specifiedGroup.getGroupName().equals(oldName))
				specifiedGroup.setGroupName(newName);
		}
		
		ReviewRequirement reviewRequirement = getReviewRequirement();
		if (reviewRequirement != null) {
			for (Group group: reviewRequirement.getGroups().keySet()) {
				if (group.getName().equals(oldName))
					group.setName(newName);
			}
			setReviewRequirementSpec(reviewRequirement.toSpec());
		}
		
		for (FileProtection fileProtection: getFileProtections()) {
			reviewRequirement = fileProtection.getReviewRequirement();
			for (Group group: reviewRequirement.getGroups().keySet()) {
				if (group.getName().equals(oldName))
					group.setName(newName);
			}
			String spec = reviewRequirement.toSpec();
			Preconditions.checkState(spec != null);
			fileProtection.setReviewRequirementSpec(spec);
		}
	}
	
	public boolean onDeleteGroup(String groupName) {
		if (getSubmitter() instanceof SpecifiedGroup) {
			SpecifiedGroup specifiedGroup = (SpecifiedGroup) getSubmitter();
			if (specifiedGroup.getGroupName().equals(groupName))
				return true;
		}
		
		ReviewRequirement reviewRequirement = getReviewRequirement();
		if (reviewRequirement != null) {
			for (Group group: reviewRequirement.getGroups().keySet()) {
				if (group.getName().equals(groupName))
					return true;
			}
		}
		
		for (Iterator<FileProtection> it = getFileProtections().iterator(); it.hasNext();) {
			FileProtection protection = it.next();
			reviewRequirement = protection.getReviewRequirement();
			for (Group group: reviewRequirement.getGroups().keySet()) {
				if (group.getName().equals(groupName)) {
					it.remove();
					break;
				}
			}
		}
		
		return false;
	}
	
	public void onRenameConfiguration(String oldName, String newName) {
		int index = getConfigurations().indexOf(oldName);
		if (index != -1)
			getConfigurations().set(index, newName);
	}
	
	public void onDeleteConfiguration(String configurationName) {
		getConfigurations().remove(configurationName);
	}
	
	public void onRenameUser(String oldName, String newName) {
		if (getSubmitter() instanceof SpecifiedUser) {
			SpecifiedUser specifiedUser = (SpecifiedUser) getSubmitter();
			if (specifiedUser.getUserName().equals(oldName))
				specifiedUser.setUserName(newName);
		}
		
		ReviewRequirement reviewRequirement = getReviewRequirement();
		if (reviewRequirement != null) {
			for (User user: reviewRequirement.getUsers()) {
				if (user.getName().equals(oldName))
					user.setName(newName);
			}
			setReviewRequirementSpec(reviewRequirement.toSpec());
		}
		
		for (FileProtection fileProtection: getFileProtections()) {
			reviewRequirement = fileProtection.getReviewRequirement();
			for (User user: reviewRequirement.getUsers()) {
				if (user.getName().equals(oldName))
					user.setName(newName);
			}
			String spec = reviewRequirement.toSpec();
			Preconditions.checkState(spec != null);
			fileProtection.setReviewRequirementSpec(spec);
		}	
	}
	
	public boolean onDeleteUser(String userName) {
		if (getSubmitter() instanceof SpecifiedUser) {
			SpecifiedUser specifiedUser = (SpecifiedUser) getSubmitter();
			if (specifiedUser.getUserName().equals(userName))
				return true;
		}
		
		ReviewRequirement reviewRequirement = getReviewRequirement();
		if (reviewRequirement != null) {
			for (User user: reviewRequirement.getUsers()) {
				if (user.getName().equals(userName))
					return true;
			}
		}
		
		for (Iterator<FileProtection> it = getFileProtections().iterator(); it.hasNext();) {
			FileProtection protection = it.next();
			reviewRequirement = protection.getReviewRequirement();
			for (User user: reviewRequirement.getUsers()) {
				if (user.getName().equals(userName)) {
					it.remove();
					break;
				}
			}
		}
		return false;
	}
	
	public boolean onBranchDeleted(String branchName) {
		return branchName.equals(getBranch());
	}

}
