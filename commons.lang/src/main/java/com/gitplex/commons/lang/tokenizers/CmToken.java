package com.gitplex.commons.lang.tokenizers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.gitplex.commons.lang.diff.DiffMatchPatch.Operation;
import com.gitplex.commons.util.StringUtils;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;

/**
 * Represents a CodeMirror token
 * 
 * @author robin
 *
 */
public class CmToken implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final String type;
	
	private final String text;
	
	public CmToken(String type, String text) {
		this.type = type;
		this.text = text;
	}

	public String getType() {
		return type;
	}

	public String getText() {
		return text;
	}
	
	public boolean isComment() {
		return type.contains("comment");
	}
	
	public boolean isKeyword() {
		return type.contains("keyword");
	}
	
	public boolean isNumber() {
		return type.contains("number");
	}
	
	public boolean isIdentifier() {
		return type.contains("variable");
	}
	
	public boolean isString() {
		return type.contains("string");
	}
	
	public boolean isMeta() {
		return type.contains("meta");
	}
	
	public boolean isTag() {
		return type.contains("tag");
	}
	
	public boolean isBracket() {
		return type.contains("bracket");
	}
	
	public boolean isOperator() {
		return type.contains("operator");
	}
	
	public boolean isAtom() {
		return type.contains("atom");
	}
	
	public boolean isDef() {
		return type.contains("def");
	}
	
	public boolean isQualifier() {
		return type.contains("qualifier");
	}
	
	public boolean isAttribute() {
		return type.contains("attribute");
	}
	
	public boolean isProperty() {
		return type.contains("property");
	}
	
	public boolean isBuiltin() {
		return type.contains("builtin");
	}
	
	public boolean isLink() {
		return type.contains("link");
	}
	
	public boolean isError() {
		return type.contains("error");
	}
	
	public boolean isNotCommentOrString() {
		return !isComment() && !isString();
	}
	
	public boolean isEof() {
		return type.length() == 0 && text.length() == 0;
	}
	
	public boolean isEol() {
		return type.length() == 0 && text.equals("\n");
	}
	
	public boolean isWhitespace() {
		if (type.length() != 0)
			return false;
		for (char ch: text.toCharArray()) {
			if (ch != ' ' && ch != '\n' && ch != '\r' && ch != '\t')
				return false;
		}
		return true;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof CmToken))
			return false;
		if (this == other)
			return true;
		CmToken otherToken = (CmToken) other;
		return Objects.equal(type, otherToken.type) && Objects.equal(text, otherToken.text);
	}

	public int hashCode() {
		return Objects.hashCode(type, text);
	}

	public String toHtml(Operation operation) {
		String escapedText;
		if (text.equals("\r"))
			escapedText = " ";
		else
			escapedText = StringUtils.escapeHtml(text);
		
		if (operation == Operation.EQUAL && StringUtils.isBlank(type)) {
			return escapedText;
		} else {
			StringBuilder builder = new StringBuilder("<span class='");
			for (String each: Splitter.on(" ").trimResults().omitEmptyStrings().split(type)) 
				builder.append("cm-" + each + " ");
			if (operation == Operation.DELETE)
				builder.append("delete");
			else if (operation == Operation.INSERT)
				builder.append("insert");
			builder.append("'>").append(escapedText).append("</span>");
			return builder.toString();
		}
		
	}
	
	@Override
	public String toString() {
		if (StringUtils.isNotBlank(type))
			return text + "[" + type + "]";
		else
			return text;
	}

	/**
	 * CodeMirror returns continuous spaces/tabs as a single token and it makes diff not ideal, 
	 * for instance, if we have three tabs as below:
	 * \t\t\t
	 * And if we add another tab:
	 * \t\t\t\t
	 * The diff will be a deletion of token "\t\t\t" and addition of token "\t\t\t\t", which is 
	 * not optimal. So we split a whitespace token containing multiple characters as multiple 
	 * single-character tokens to make the diff able to figure out that a single "\t" token is 
	 * inserted
	 * 
	 * @return
	 */
	@Nullable
	public List<CmToken> split() {
		if (isWhitespace()) {
			List<CmToken> splitted = new ArrayList<>();
			for (char ch: text.toCharArray()) {
				splitted.add(new CmToken(type, String.valueOf(ch)));
			}
			return splitted;
		} else {
			return null;
		}
	}
	
}
