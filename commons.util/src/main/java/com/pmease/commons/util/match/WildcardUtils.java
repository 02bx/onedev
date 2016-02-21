package com.pmease.commons.util.match;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.pmease.commons.util.Range;

public class WildcardUtils {
	
	private static final PatternSetMatcher stringPatternSetMatcher = new PatternSetMatcher(new WildcardStringMatcher());
	
	private static final PatternSetMatcher pathPatternSetMatcher = new PatternSetMatcher(new WildcardPathMatcher());

	/**
     * Tests whether or not a given path matches a given patterns. 
     * <p>
     * Patterns are separated by comma or line separator, with each pattern using syntax 
     * of <a href="http://ant.apache.org/manual/dirtasks.html">Ant path pattern</a>.
     * <p> 
     * If a pattern is prefixed with <code>-</code>, it is considered as a negative pattern; 
     * if a pattern is prefixed with <code>+</code>, or is not prefixed with both signs, 
     * it is considered as positive pattern.  
     * <p>
     * When determine if a string matches the patterns, the string will be matched against 
     * each pattern in order, until finding a matching negative or positive pattern. If a 
     * negative pattern is matched first, the string is considered not matched for the whole
     * patterns, and if a positive pattern is matched first, the string is considered matched 
     * for the whole patterns. If no any pattern is matched, the string is considered not 
     * matched for the whole patterns.  
     * <p>
     * Some examples:
     * <ul>
     * <li>assert(match("-&#42;&#42;/&#42;.java, &#42;&#42;", "Test.java") == false);
     * <li>assert(match("-&#42;&#42;/&#42;.java, &#42;&#42;", "test.c") == true);
     * </ul> 
	 *
     * @param patterns 
     * 			the patterns to match against. Must not be null
     * @param str     
     * 			the path to match, as a String. Must not be null
     *
     * @return 
     * 			true if the patterns matches against the string, or false otherwise
     * 
     */
    public static boolean matchPath(String patterns, String path) {
    	return pathPatternSetMatcher.matches(patterns, path);
    }
    
    /**
     * Tests whether or not a string matches against specified patterns. 
     * <p>
     * Patterns are separated with comma or line separator, with each pattern may contain 
     * two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     * <p>
     * If a pattern is prefixed with <code>-</code>, it is considered as a negative pattern; 
     * if a pattern is prefixed with <code>+</code>, or is not prefixed with both signs, 
     * it is considered as positive pattern.  
     * <p>
     * When determine if a string matches the patterns, the string will be matched against 
     * each pattern in order, until finding a matching negative or positive pattern. If a 
     * negative pattern is matched first, the string is considered not matched for the whole
     * patterns, and if a positive pattern is matched first, the string is considered matched 
     * for the whole patterns. If no any pattern is matched, the string is considered not 
     * matched for the whole patterns.  
     * <p>
     * Some examples:
     * <ul>
     * <li>assert(wildcardMatch("-Core.java, &#42;.java", "Core.java") == false);
     * <li>assert(wildcardMatch("-Core.java, &#42;.java", "Client.java") == true);
     * </ul> 
     *
     * @param pattern 
     * 			the pattern to match against. Must not be null.
     * @param str     
     * 			the string which must be matched against the pattern. Must not be null.
     * @return 
     * 			true if the string matches against the pattern, or false otherwise.
     */
    public static boolean matchString(String patterns, String str) {
    	return stringPatternSetMatcher.matches(patterns, str);
    }
    
    public static boolean hasWildcards(String input) {
    	return (input.indexOf('*') != -1 || input.indexOf('?') != -1);
    }
    
    /**
     * Check the unit test for explanation.
     * 
     * @param text
     * @param wildcard
     * @param caseSensitive
     * @return
     */
    @Nullable
    public static WildcardApplied applyWildcard(String text, String wildcard, boolean caseSensitive) {
		String normalizedText;
		String normalizedWildcard;
		if (caseSensitive) {
			normalizedText = text;
			normalizedWildcard = wildcard;
		} else {
			normalizedText = text.toLowerCase();
			normalizedWildcard = wildcard.toLowerCase();
		}
		List<Range> literalRanges = new ArrayList<>();
		int pos = 0;
		int index = normalizedWildcard.indexOf('*');
		while (index != -1) {
			if (index>pos)
				literalRanges.add(new Range(pos, index));
			pos = index+1;
			index = normalizedWildcard.indexOf('*', pos);
		}
		if (normalizedWildcard.length()>pos)
			literalRanges.add(new Range(pos, normalizedWildcard.length()));

		String appliedText = wildcard;
		int first = -1;
		int last = 0;
		pos = 0;
		for (Range literalRange: literalRanges) {
			String literal = wildcard.substring(literalRange.getFrom(), literalRange.getTo());
			index = normalizedText.indexOf(literal, pos);
			if (index != -1) {
				if (first == -1)
					first = index;
				pos = index+literal.length();
				last = pos;
				appliedText = replaceLiteral(appliedText, literalRange, text.substring(index, pos));
			} else {
				return null;
			}
		}
		if (first != -1 && wildcard.charAt(0) != '*')
			appliedText = text.substring(0, first) + appliedText;
		else 
			first = 0;
		if (wildcard.length() == 0 || wildcard.charAt(wildcard.length()-1) != '*') {
			String suffix = text.substring(last);
			last = appliedText.length();
			appliedText = appliedText + suffix;
		} else {
			last = appliedText.length();
		}
		return new WildcardApplied(appliedText, new Range(first, last));
    }
	
	private static String replaceLiteral(String text, Range literalRange, String literal) {
		String prefix = text.substring(0, literalRange.getFrom());
		String suffix = text.substring(literalRange.getTo());
		return prefix + literal + suffix;
	}
	
}
