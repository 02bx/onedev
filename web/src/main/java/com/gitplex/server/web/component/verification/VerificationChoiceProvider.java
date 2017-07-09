package com.gitplex.server.web.component.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONWriter;
import org.unbescape.html.HtmlEscape;

import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.VerificationManager;
import com.gitplex.server.web.WebConstants;
import com.gitplex.server.web.component.select2.ChoiceProvider;
import com.gitplex.server.web.component.select2.Response;
import com.gitplex.server.web.component.select2.ResponseFiller;
import com.gitplex.server.web.page.project.ProjectPage;
import com.gitplex.server.web.util.WicketUtils;

public class VerificationChoiceProvider extends ChoiceProvider<String> {

	private static final long serialVersionUID = 1L;
	
	@Override
	public void toJson(String choice, JSONWriter writer) throws JSONException {
		String escapedName = HtmlEscape.escapeHtml5(choice);
		writer.key("id").value(escapedName).key("name").value(escapedName);
	}

	@Override
	public Collection<String> toChoices(Collection<String> ids) {
		return ids;
	}

	@Override
	public void query(String term, int page, Response<String> response) {
		ProjectPage projectPage = (ProjectPage) WicketUtils.getPage();
		Collection<String> verifications = GitPlex.getInstance(VerificationManager.class)
				.getVerificationNames(projectPage.getProject());
		List<String> verificationList = new ArrayList<>(verifications);
		Collections.sort(verificationList);
		new ResponseFiller<String>(response).fill(verificationList, page, WebConstants.PAGE_SIZE);
	}
	
}