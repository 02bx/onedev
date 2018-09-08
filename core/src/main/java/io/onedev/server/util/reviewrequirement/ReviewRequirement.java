package io.onedev.server.util.reviewrequirement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import io.onedev.server.util.reviewrequirement.ReviewRequirementParser.CountContext;
import io.onedev.server.util.reviewrequirement.ReviewRequirementParser.CriteriaContext;
import io.onedev.server.util.reviewrequirement.ReviewRequirementParser.RequirementContext;

import io.onedev.server.OneDev;
import io.onedev.server.exception.OneException;
import io.onedev.server.manager.TeamManager;
import io.onedev.server.manager.UserManager;
import io.onedev.server.model.Project;
import io.onedev.server.model.Team;
import io.onedev.server.model.User;

public class ReviewRequirement {
	
	private final List<User> users;
	
	private final Map<Team, Integer> teams;
	
	public ReviewRequirement(List<User> users, Map<Team, Integer> teams) {
		this.users = users;
		this.teams = teams;
	}
	
	public static ReviewRequirement parse(Project project, String requirementString) {
		List<User> users = new ArrayList<>();
		Map<Team, Integer> teams = new LinkedHashMap<>();
		
		if (requirementString != null) {
			RequirementContext requirement = parse(requirementString);
			
			for (CriteriaContext criteria: requirement.criteria()) {
				if (criteria.userCriteria() != null) {
					String userName = getBracedValue(criteria.userCriteria().Value());
					User user = OneDev.getInstance(UserManager.class).findByName(userName);
					if (user != null) {
						if (!users.contains(user)) { 
							users.add(user);
						} else {
							throw new OneException("User '" + userName + "' is included multiple times");
						}
					} else {
						throw new OneException("Unable to find user '" + userName + "'");
					}
				} else if (criteria.teamCriteria() != null) {
					String teamName = getBracedValue(criteria.teamCriteria().Value());
					Team team = OneDev.getInstance(TeamManager.class).find(project, teamName);
					if (team!= null) {
						if (!teams.containsKey(team)) {
							CountContext count = criteria.teamCriteria().count();
							if (count != null) {
								if (count.DIGIT() != null)
									teams.put(team, Integer.parseInt(count.DIGIT().getText()));
								else
									teams.put(team, 0);
							} else {
								teams.put(team, 1);
							}
						} else {
							throw new OneException("Team '" + teamName + "' is included multiple times");
						}
					} else {
						throw new OneException("Unable to find team '" + teamName + "'");
					}
				}
			}			
		}
		
		return new ReviewRequirement(users, teams);
	}

	public static RequirementContext parse(String requirementString) {
		ANTLRInputStream is = new ANTLRInputStream(requirementString); 
		ReviewRequirementLexer lexer = new ReviewRequirementLexer(is);
		lexer.removeErrorListeners();
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ReviewRequirementParser parser = new ReviewRequirementParser(tokens);
		parser.removeErrorListeners();
		parser.setErrorHandler(new BailErrorStrategy());
		return parser.requirement();
	}
	
	private static String getBracedValue(TerminalNode terminal) {
		String value = terminal.getText().substring(1);
		return value.substring(0, value.length()-1).trim();
	}
	
	public List<User> getUsers() {
		return users;
	}

	public Map<Team, Integer> getTeams() {
		return teams;
	}
	
	public boolean satisfied(User user) {
		for (User eachUser: users) {
			if (!eachUser.equals(user))
				return false;
		}
		for (Map.Entry<Team, Integer> entry: teams.entrySet()) {
			Team team = entry.getKey();
			int requiredCount = entry.getValue();
			if (requiredCount == 0 || requiredCount > team.getMembers().size())
				requiredCount = team.getMembers().size();

			if (requiredCount > 1 || requiredCount == 1 && !team.getMembers().contains(user))
				return false;
		}
		return true;
	}
	
	@Nullable
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (User user: users)
			builder.append("user(").append(user.getName()).append(") ");
		for (Map.Entry<Team, Integer> entry: teams.entrySet()) {
			builder.append("team(").append(entry.getKey().getName()).append(")");
			if (entry.getValue() == 0)
				builder.append(":all");
			else if (entry.getValue() != 1)
				builder.append(":").append(entry.getValue());
			builder.append(" ");
		}
		if (builder.length() != 0)
			return builder.toString().trim();
		else
			return null;
	}
}
