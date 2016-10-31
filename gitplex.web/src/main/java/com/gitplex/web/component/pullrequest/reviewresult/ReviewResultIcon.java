package com.gitplex.web.component.pullrequest.reviewresult;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;

import com.gitplex.core.entity.PullRequestReview;
import com.gitplex.commons.wicket.behavior.dropdown.DropdownHover;
import com.gitplex.commons.wicket.component.floating.AlignPlacement;

@SuppressWarnings("serial")
public class ReviewResultIcon extends WebComponent {

	private final IModel<PullRequestReview> model;
	
	public ReviewResultIcon(String id, IModel<PullRequestReview> model) {
		super(id);
		this.model = model;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		PullRequestReview review = model.getObject();
		String css;
		if (review.getUpdate().equals(review.getUpdate().getRequest().getLatestUpdate())) { 
			if (review.getResult() == PullRequestReview.Result.APPROVE) {
				css = " review-result approved current fa fa-check-circle";
			} else {
				css = " review-result disapproved current fa fa-times-circle";
			}
		} else if (review.getResult() == PullRequestReview.Result.APPROVE) {
			css = " review-result approved previous fa fa-check-circle";
		} else {
			css = " review-result disapproved previous fa fa-times-circle";
		}
		add(AttributeAppender.append("class", css));
		
		add(new DropdownHover(new AlignPlacement(100, 0, 0, 100, 0)) {
			
			@Override
			protected Component newContent(String id) {
				return new ReviewResultTooltip(id, model);
			}
		});
	}

	@Override
	protected void onDetach() {
		model.detach();
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new ReviewResultResourceReference()));
	}

}
