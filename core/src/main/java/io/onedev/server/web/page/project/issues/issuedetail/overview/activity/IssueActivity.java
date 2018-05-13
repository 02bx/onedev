package io.onedev.server.web.page.project.issues.issuedetail.overview.activity;

import java.io.Serializable;
import java.util.Date;

import javax.annotation.Nullable;

import org.apache.wicket.markup.html.panel.Panel;

public interface IssueActivity extends Serializable {
	
	Date getDate();
	
	Panel render(String panelId);
	
	@Nullable
	String getAnchor();
}
