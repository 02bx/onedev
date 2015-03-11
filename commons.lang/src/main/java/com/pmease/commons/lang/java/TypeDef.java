package com.pmease.commons.lang.java;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class TypeDef {

	public enum Kind {
		CLASS, 
		INTERFACE, 
		ANNOTATION {

			@Override
			public String toString() {
				return "@interface";
			}
			
		}, 
		ENUM
	};
	
	public String name;
	
	public Kind kind;

	public List<FieldDef> fieldDefs = new ArrayList<>();
	
	public List<MethodDef> methodDefs = new ArrayList<>();
	
	public List<TypeDef> typeDefs = new ArrayList<>();
	
	public int line;
	
	public List<Modifier> modifiers = new ArrayList<>();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Modifier modifier: modifiers) 
			builder.append(modifier.name().toLowerCase()).append(" ");
		builder.append(kind.toString().toLowerCase()).append(" ");
		builder.append(name).append(" {\n\n");
		
		List<String> enumConstants = new ArrayList<>();
		for (FieldDef fieldDef: fieldDefs) {
			if (fieldDef.type == null)  
				enumConstants.add(fieldDef.name);
		}
		if (!enumConstants.isEmpty())
			builder.append("  ").append(Joiner.on(", ").join(enumConstants)).append(";\n\n");
		else if (kind == Kind.ENUM)
			builder.append("  ;\n\n");
		for (FieldDef fieldDef: fieldDefs) {
			if (fieldDef.type != null)
				builder.append("  ").append(fieldDef).append("\n\n");
		}
		for (MethodDef methodDef: methodDefs) 
			builder.append("  ").append(methodDef).append("\n\n");
		for (TypeDef typeDef: typeDefs) {
			for (String line: Splitter.on('\n').omitEmptyStrings().split(typeDef.toString()))
				builder.append("  ").append(line).append("\n\n");
		}
		
		builder.append("}");
		
		return builder.toString();
	}
	
}
