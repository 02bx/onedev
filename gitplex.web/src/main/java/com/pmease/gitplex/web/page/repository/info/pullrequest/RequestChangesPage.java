package com.pmease.gitplex.web.page.repository.info.pullrequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.pmease.gitplex.core.model.IntegrationInfo;
import com.pmease.gitplex.core.model.PullRequest;
import com.pmease.gitplex.core.model.PullRequest.Status;
import com.pmease.gitplex.web.page.repository.info.code.commit.diff.DiffViewPanel;

@SuppressWarnings("serial")
public class RequestChangesPage extends RequestDetailPage {

	private Panel diffView;
	
	private DropDownChoice<String> baseCommitSelector;
	
	private DropDownChoice<String> headCommitSelector;
	
	// base commit represents comparison base
	private String baseCommit;
	
	// head commit represents comparison head
	private String headCommit;
	
	// map commit name to comit hash
	private IModel<LinkedHashMap<String, String>> commitsModel = 
			new LoadableDetachableModel<LinkedHashMap<String, String>>() {

		@Override
		protected LinkedHashMap<String, String> load() {
			LinkedHashMap<String, String> choices = new LinkedHashMap<>();
			
			PullRequest request = getPullRequest();
			IntegrationInfo mergeInfo = request.getIntegrationInfo();
			if (request.isOpen() 
					&& mergeInfo.getIntegrationHead() != null 
					&& !mergeInfo.getIntegrationHead().equals(mergeInfo.getRequestHead())) { 
				choices.put(request.getIntegrationInfo().getIntegrationHead(), "Integration Preview");
			}
			
			for (int i=request.getSortedUpdates().size()-1; i>=0; i--)
				choices.put(request.getSortedUpdates().get(i).getHeadCommit(), "Head of Update #" + i);
			
			String baseCommit = request.getBaseUpdate().getHeadCommit();
			if (request.getTarget().getHeadCommit().equals(baseCommit)) {
				choices.put(request.getTarget().getHeadCommit(), "Target Branch Head");
			} else {
				String displayName = choices.get(request.getTarget().getHeadCommit());
				if (displayName != null) {
					choices.put(request.getTarget().getHeadCommit(), displayName + " (current head of target branch)");
				} else {
					choices.put(request.getTarget().getHeadCommit(), "Target Branch Head (current)");
				}
				choices.put(baseCommit, "Target Branch Head (original)");
			}
			
			return choices;
		}
		
	};

	public RequestChangesPage(PageParameters params) {
		super(params);
		
		baseCommit = params.get("base").toString();
		headCommit = params.get("head").toString();
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		IModel<List<String>> commitChoices = new LoadableDetachableModel<List<String>>() {

			@Override
			protected List<String> load() {
				return new ArrayList<String>(commitsModel.getObject().keySet());
			}
			
		};
		
		IChoiceRenderer<String> choiceRenderer = new IChoiceRenderer<String>() {

			@Override
			public Object getDisplayValue(String object) {
				return commitsModel.getObject().get(object);
			}

			@Override
			public String getIdValue(String object, int index) {
				return object;
			}
			
		};
		
		baseCommitSelector = new DropDownChoice<String>("baseCommitChoice", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				if (!commitsModel.getObject().containsKey(baseCommit)) {
					if (getPullRequest().getStatus() == Status.INTEGRATED)
						baseCommit = getPullRequest().getBaseUpdate().getHeadCommit();
					else
						baseCommit = getPullRequest().getTarget().getHeadCommit();
				}
				return baseCommit;
			}

			@Override
			public void setObject(String object) {
				baseCommit = object;
			}
			
		}, commitChoices, choiceRenderer);
		
		baseCommitSelector.add(new OnChangeAjaxBehavior() {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(diffView);
			}
		});
		add(baseCommitSelector);
		
		headCommitSelector = new DropDownChoice<String>("headCommitChoice", new IModel<String>() {

			@Override
			public void detach() {
			}

			@Override
			public String getObject() {
				if (!commitsModel.getObject().containsKey(headCommit)) {
					headCommit = getPullRequest().getLatestUpdate().getHeadCommit();
				}
				return headCommit;
			}
			
			@Override
			public void setObject(String object) {
				headCommit = object;
			}

		}, commitChoices, choiceRenderer);
		
		headCommitSelector.add(new OnChangeAjaxBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(diffView);
			}
			
		});
		add(headCommitSelector);
		
		add(diffView = new DiffViewPanel("diffView", new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				return baseCommitSelector.getDefaultModelObjectAsString();
			}
			
		}, new LoadableDetachableModel<String>() {

			@Override
			protected String load() {
				return headCommitSelector.getDefaultModelObjectAsString();
			}
			
		}));
		
		diffView.setOutputMarkupId(true);
	}
	
	@Override
	public void onDetach() {
		commitsModel.detach();
		
		super.onDetach();
	}

	public static PageParameters params4(PullRequest request, String baseCommit, String headCommit) {
		PageParameters params = RequestDetailPage.params4(request);
		
		params.set("base", baseCommit);
		params.set("head", headCommit);
		
		return params;
	}
	
}
