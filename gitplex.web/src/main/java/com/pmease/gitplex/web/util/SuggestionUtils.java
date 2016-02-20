package com.pmease.gitplex.web.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Ref;

import com.pmease.commons.antlr.codeassist.InputSuggestion;
import com.pmease.commons.git.GitUtils;
import com.pmease.commons.util.Range;
import com.pmease.commons.util.pattern.WildcardApplied;
import com.pmease.commons.util.pattern.WildcardUtils;
import com.pmease.gitplex.core.GitPlex;
import com.pmease.gitplex.core.manager.AuxiliaryManager;
import com.pmease.gitplex.core.model.Depot;

public class SuggestionUtils {
	
	public static List<InputSuggestion> suggestBranch(Depot depot, String matchWith, int count) {
		String lowerCaseMatchWith = matchWith.toLowerCase();
		int numSuggestions = 0;
		List<InputSuggestion> suggestions = new ArrayList<>();
		for (Ref ref: depot.getBranchRefs()) {
			String branch = GitUtils.ref2branch(ref.getName());
			int index = branch.toLowerCase().indexOf(lowerCaseMatchWith);
			if (index != -1 && numSuggestions++<count) {
				Range matchRange = new Range(index, index+lowerCaseMatchWith.length());
				suggestions.add(new InputSuggestion(branch, matchRange));
			}
		}
		return suggestions;
	}
	
	public static List<InputSuggestion> suggestTag(Depot depot, String matchWith, int count) {
		String lowerCaseMatchWith = matchWith.toLowerCase();
		int numSuggestions = 0;
		List<InputSuggestion> suggestions = new ArrayList<>();
		for (Ref ref: depot.getTagRefs()) {
			String tag = GitUtils.ref2tag(ref.getName());
			int index = tag.toLowerCase().indexOf(lowerCaseMatchWith);
			if (index != -1 && numSuggestions++<count) {
				Range matchRange = new Range(index, index+lowerCaseMatchWith.length());
				suggestions.add(new InputSuggestion(tag, matchRange));
			}
		}
		return suggestions;
	}
	
	public static List<InputSuggestion> suggestPath(Depot depot, String matchWith, int count) {
		String lowerCaseMatchWith = matchWith.toLowerCase();
		List<InputSuggestion> suggestions = new ArrayList<>();
		
		List<WildcardApplied> allApplied = new ArrayList<>();
		Map<String, Range> suggestedInputs = new LinkedHashMap<>();
		AuxiliaryManager auxiliaryManager = GitPlex.getInstance(AuxiliaryManager.class);
		for (String path: auxiliaryManager.getFiles(depot)) {
			WildcardApplied applied = WildcardUtils.applyWildcard(path, lowerCaseMatchWith, false);
			if (applied != null) 
				allApplied.add(applied);
		}
		Collections.sort(allApplied, new Comparator<WildcardApplied>() {

			@Override
			public int compare(WildcardApplied o1, WildcardApplied o2) {
				return o1.getMatchRange().getFrom() - o2.getMatchRange().getFrom();
			}
			
		});

		suggestedInputs = new LinkedHashMap<>();
		for (WildcardApplied applied: allApplied) {
			Range matchRange = applied.getMatchRange();
			String suffix = applied.getText().substring(matchRange.getTo());
			int index = suffix.indexOf('/');
			String suggestedInput = applied.getText().substring(0, matchRange.getTo());
			if (index != -1)
				suggestedInput += suffix.substring(0, index) + "/";
			else
				suggestedInput += suffix;
			suggestedInputs.put(suggestedInput, matchRange);
			if (suggestedInputs.size() == count)
				break;
		}
		
		for (Map.Entry<String, Range> entry: suggestedInputs.entrySet()) { 
			String text = entry.getKey();
			int caret;
			if (text.endsWith("/"))
				caret = text.length();
			else
				caret = -1;
			suggestions.add(new InputSuggestion(text, caret, true, null, entry.getValue()));
		}
		
		return suggestions;		
	}
}
