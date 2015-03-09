package com.pmease.commons.lang.analyzers.java;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class MethodDef {

	public String name;
	
	@Nullable
	public String returnType; // null for constructor
	
	@Nullable
	public String params;

	public int line;
	
	public List<Modifier> modifiers = new ArrayList<>();
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Modifier modifier: modifiers) 
			builder.append(modifier.name().toLowerCase()).append(" ");
		if (returnType != null)
			builder.append(returnType).append(" ");
		builder.append(name);
		if (params != null)
			builder.append("(").append(params).append(")");
		else
			builder.append("()");
		return builder.toString();
	}
	
}
