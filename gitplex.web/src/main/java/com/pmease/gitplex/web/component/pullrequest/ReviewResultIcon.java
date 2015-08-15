package com.pmease.gitplex.web.component.pullrequest;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;

import com.pmease.commons.wicket.behavior.TooltipBehavior;
import com.pmease.gitplex.core.model.Review;

@SuppressWarnings("serial")
public class ReviewResultIcon extends WebComponent {

	public ReviewResultIcon(String id, IModel<Review> reviewModel) {
		super(id, reviewModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		Review review = (Review) getDefaultModelObject();
		String css;
		String tooltip;
		if (review.getUpdate().equals(review.getUpdate().getRequest().getLatestUpdate())) { 
			if (review.getResult() == Review.Result.APPROVE) {
				css = " review-result approved current fa fa-thumbs-o-up";
				tooltip = "Approved";
			} else {
				css = " review-result disapproved current fa fa-thumbs-o-down";
				tooltip = "Disapproved";
			}
		} else if (review.getResult() == Review.Result.APPROVE) {
			css = " review-result approved previous fa fa-thumbs-o-up";
			tooltip = "Approved on previous updates";
		} else {
			css = " review-result disapproved previous fa fa-thumbs-o-down";
			tooltip = "Disapproved on previous updates";
		}
		add(AttributeAppender.append("class", css));
		add(new TooltipBehavior(Model.of(tooltip)));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(ReviewResultIcon.class, "review-result.css")));
	}

}
