package io.onedev.server.ci;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.constraints.NotEmpty;

import io.onedev.server.ci.jobparam.JobParam;
import io.onedev.server.util.OneContext;
import io.onedev.server.web.editable.annotation.ChoiceProvider;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.page.project.blob.render.renderers.cispec.dependencies.DependencyEditPanel;

@Editable
public class DependencySpec implements Serializable {

	private static final long serialVersionUID = 1L;

	private String job;
	
	private List<JobParam> params = new ArrayList<>();
	
	private String artifacts;

	@Editable(order=100)
	@ChoiceProvider("getJobChoices")
	@NotEmpty
	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	@Editable(order=200)
	public List<JobParam> getParams() {
		return params;
	}

	public void setParams(List<JobParam> params) {
		this.params = params;
	}

	@Editable(order=200)
	public String getArtifacts() {
		return artifacts;
	}

	public void setArtifacts(String artifacts) {
		this.artifacts = artifacts;
	}
	
	@SuppressWarnings("unused")
	private static List<String> getJobChoices() {
		DependencyEditPanel editor = OneContext.get().getComponent().findParent(DependencyEditPanel.class);
		List<String> choices = new ArrayList<>();
		JobSpec belongingJob = editor.getBelongingJob();
		for (JobSpec job: editor.getEditingCISpec().getJobs()) {
			choices.add(job.getName());
		}
		choices.remove(belongingJob.getName());
		return choices;
	}
	
}
