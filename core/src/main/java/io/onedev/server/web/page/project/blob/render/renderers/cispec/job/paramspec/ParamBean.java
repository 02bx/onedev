package io.onedev.server.web.page.project.blob.render.renderers.cispec.job.paramspec;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.web.editable.annotation.Editable;

@Editable
public class ParamBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private InputSpec param;

	@Editable(name="Type", order=100)
	@NotNull(message="may not be empty")
	public InputSpec getParam() {
		return param;
	}

	public void setParam(InputSpec param) {
		this.param = param;
	}

}
