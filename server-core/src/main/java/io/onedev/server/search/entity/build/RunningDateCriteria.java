package io.onedev.server.search.entity.build;

import java.util.Date;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Build;
import io.onedev.server.model.User;
import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.search.entity.EntityQuery;
import io.onedev.server.util.query.BuildQueryConstants;

public class RunningDateCriteria extends EntityCriteria<Build> {

	private static final long serialVersionUID = 1L;

	private final int operator;
	
	private final Date date;
	
	private final String value;
	
	public RunningDateCriteria(String value, int operator) {
		this.operator = operator;
		date = EntityQuery.getDateValue(value);
		this.value = value;
	}

	@Override
	public Predicate getPredicate(Root<Build> root, CriteriaBuilder builder, User user) {
		Path<Date> attribute = root.get(BuildQueryConstants.ATTR_RUNNING_DATE);
		if (operator == BuildQueryLexer.IsBefore)
			return builder.lessThan(attribute, date);
		else
			return builder.greaterThan(attribute, date);
	}

	@Override
	public boolean matches(Build build, User user) {
		if (operator == BuildQueryLexer.IsBefore)
			return build.getRunningDate().before(date);
		else
			return build.getRunningDate().after(date);
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public String toString() {
		return BuildQuery.quote(BuildQueryConstants.FIELD_RUNNING_DATE) + " " 
				+ BuildQuery.getRuleName(operator) + " " + BuildQuery.quote(value);
	}

}
