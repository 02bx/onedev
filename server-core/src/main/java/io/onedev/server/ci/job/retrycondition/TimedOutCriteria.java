package io.onedev.server.ci.job.retrycondition;

import java.util.function.Predicate;

import io.onedev.server.model.Build;

public class TimedOutCriteria implements Predicate<Build> {

	@Override
	public boolean test(Build build) {
		return build.getStatus() == Build.Status.TIMED_OUT;
	}

}
