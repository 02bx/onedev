package com.pmease.gitplex.search.query;

import static com.pmease.gitplex.search.FieldConstants.*;
import static com.pmease.gitplex.search.IndexConstants.NGRAM_SIZE;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.pmease.gitplex.search.hit.QueryHit;
import com.pmease.gitplex.search.query.regex.RegexLiterals;

public abstract class BlobQuery {

	private final String fieldName;
	
	private final String searchFor;
	
	private final int count;

	private final boolean wordMatch;
	
	private final boolean regex;
	
	private final boolean caseSensitive;
	
	private final String pathPrefix;
	
	private final String pathSuffix;
	
	public BlobQuery(String fieldName, String searchFor, boolean regex, boolean wordMatch, boolean caseSensitive, 
			@Nullable String pathPrefix, @Nullable String pathSuffix, int count) {
		this.fieldName = fieldName;
		this.searchFor = searchFor;
		this.regex = regex;
		this.wordMatch = wordMatch;
		this.caseSensitive = caseSensitive;
		this.pathPrefix = pathPrefix;
		this.pathSuffix = pathSuffix;
		this.count = count;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getSearchFor() {
		return searchFor;
	}

	public int getCount() {
		return count;
	}

	public boolean isWordMatch() {
		return wordMatch;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public String getPathPrefix() {
		return pathPrefix;
	}

	public String getPathSuffix() {
		return pathSuffix;
	}

	public boolean isRegex() {
		return regex;
	}

	public abstract void collect(TreeWalk treeWalk, List<QueryHit> hits);

	public Query asLuceneQuery() {
		BooleanQuery query = new BooleanQuery(true);
		if (regex) {
			Query literalsQuery = new RegexLiterals(searchFor).asLuceneQuery(fieldName);
			if (literalsQuery != null)
				query.add(literalsQuery, Occur.MUST);
		} else if (searchFor.length() >= NGRAM_SIZE) { 
			query.add(new NGramLuceneQuery(fieldName, searchFor), Occur.MUST);
		}
		
		/*
		if (pathPrefix != null && pathPrefix.length() >= NGRAM_SIZE)
			query.add(new NGramLuceneQuery(fieldName, pathPrefix), Occur.MUST);

		if (pathSuffix != null && pathSuffix.length() >= NGRAM_SIZE)
			query.add(new NGramLuceneQuery(fieldName, pathSuffix), Occur.MUST);
		*/
		
		if (pathPrefix != null)
			query.add(new WildcardQuery(BLOB_PATH.term(pathPrefix + "*")), Occur.MUST);
		if (pathSuffix != null)
			query.add(new WildcardQuery(BLOB_PATH.term("*" + pathSuffix)), Occur.MUST);
		
		if (query.getClauses().length != 0)
			return query;
		else
			return new WildcardQuery(BLOB_PATH.term("*"));
	}
	
}
