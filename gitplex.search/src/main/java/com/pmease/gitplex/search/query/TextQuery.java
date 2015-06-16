package com.pmease.gitplex.search.query;

import static com.pmease.gitplex.search.FieldConstants.BLOB_NAME;
import static com.pmease.gitplex.search.FieldConstants.BLOB_TEXT;
import static com.pmease.gitplex.search.IndexConstants.NGRAM_SIZE;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.lang3.CharUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.pmease.commons.lang.TokenPosition;
import com.pmease.commons.util.Charsets;
import com.pmease.gitplex.search.IndexConstants;
import com.pmease.gitplex.search.hit.QueryHit;
import com.pmease.gitplex.search.hit.TextHit;
import com.pmease.gitplex.search.query.regex.RegexLiterals;

public class TextQuery extends BlobQuery {

	private static int MAX_LINE_LEN = 1024;

	private final String term;
	
	private final boolean regex;
	
	private final boolean caseSensitive;
	
	private final boolean wholeWord;

	private final String directory;
	
	private final String fileNames;
	
	private transient Pattern pattern;
	
	public TextQuery(String term, boolean regex, boolean caseSensitive, boolean wordMatch, 
			@Nullable String directory, @Nullable String fileNames, int count) {
		super(count);
		
		this.term = term;
		this.regex = regex;
		this.caseSensitive = caseSensitive;
		this.wholeWord = wordMatch;
		this.directory = directory;
		this.fileNames = fileNames;
	}

	@Nullable
	private Pattern getPattern() {
		if (regex) {
			if (pattern == null) {
				String expression = term;
				if (wholeWord) {
					if (!expression.startsWith("\\b"))
						expression = "\\b" + expression;
					if (!expression.endsWith("\\b"))
						expression = expression + "\\b";
				}
				if (caseSensitive)
					pattern = Pattern.compile(expression);
				else
					pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
			}
			return pattern;
		} else {
			return null;
		}
	}
	
	@Override
	public void collect(TreeWalk treeWalk, List<QueryHit> hits) {
		ObjectLoader objectLoader;
		try {
			objectLoader = treeWalk.getObjectReader().open(treeWalk.getObjectId(0));
			if (objectLoader.getSize() <= IndexConstants.MAX_INDEXABLE_SIZE) {
				byte[] bytes = objectLoader.getCachedBytes();
				Charset charset = Charsets.detectFrom(bytes);
				if (charset != null) {
					String blobPath = treeWalk.getPathString();
					String content = new String(bytes, charset);

					Pattern pattern = getPattern();
					if (pattern != null) {
						int lineNo = 0;
						for (String line: Splitter.on(CharMatcher.anyOf("\n\r")).split(content)) {
							if (line.length() <= MAX_LINE_LEN) {
								Matcher matcher = pattern.matcher(line);
								while (matcher.find()) {
									TokenPosition.Range range = new TokenPosition.Range(matcher.start(), matcher.end());
									hits.add(new TextHit(blobPath, line, new TokenPosition(lineNo, range)));
									if (hits.size() >= getCount())
										break;
								}
								if (hits.size() >= getCount())
									break;
							}
							lineNo++;
						}
					} else {
						String normalizedTerm;
						if (!caseSensitive)
							normalizedTerm = term.toLowerCase();
						else
							normalizedTerm = term;
						
						int lineNo = 0;
						for (String line: Splitter.on("\n").split(content)) {
							String normalizedLine;
							if (!caseSensitive)
								normalizedLine = line.toLowerCase();
							else
								normalizedLine = line;
							
							int start = normalizedLine.indexOf(normalizedTerm, 0);
							while (start != -1) {
								int end = start + normalizedTerm.length();
								if (wholeWord) {
									char beforeChar;
									if (start == 0)
										beforeChar = ' ';
									else 
										beforeChar = line.charAt(start-1);
									
									char afterChar;
									if (end == line.length())
										afterChar = ' ';
									else
										afterChar = line.charAt(end);
									
									if (!isWordChar(beforeChar) && !isWordChar(afterChar)) {
										TokenPosition.Range range = new TokenPosition.Range(start, end);
										hits.add(new TextHit(blobPath, line, new TokenPosition(lineNo, range)));
										if (hits.size() >= getCount())
											break;
									}
								} else {
									TokenPosition.Range range = new TokenPosition.Range(start, end);
									hits.add(new TextHit(blobPath, line, new TokenPosition(lineNo, range)));
									if (hits.size() >= getCount())
										break;
								}
								start = normalizedLine.indexOf(normalizedTerm, end);
							}
							if (hits.size() >= getCount())
								break;
							lineNo++;
						}
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isWordChar(char ch) {
		return CharUtils.isAsciiAlphanumeric(ch) || ch == '_';
	}

	@Override
	public Query asLuceneQuery() throws TooGeneralQueryException {
		BooleanQuery query = new BooleanQuery(true);

		if (directory != null)
			applyDirectory(query, directory);

		if (fileNames != null) {
			BooleanQuery subQuery = new BooleanQuery(true);
			for (String pattern: Splitter.on(",").omitEmptyStrings().trimResults().split(fileNames.toLowerCase()))
				subQuery.add(new WildcardQuery(new Term(BLOB_NAME.name(), pattern)), Occur.SHOULD);
			if (subQuery.getClauses().length != 0)
				query.add(subQuery, Occur.MUST);
		}

		if (regex) 
			query.add(new RegexLiterals(term).asNGramQuery(BLOB_TEXT.name(), NGRAM_SIZE), Occur.MUST);
		else if (term.length() >= NGRAM_SIZE)
			query.add(new NGramLuceneQuery(BLOB_TEXT.name(), term, NGRAM_SIZE), Occur.MUST);
		else 
			throw new TooGeneralQueryException();

		return query;
	}
	
}
