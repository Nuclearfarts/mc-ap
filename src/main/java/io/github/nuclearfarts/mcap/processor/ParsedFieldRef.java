package io.github.nuclearfarts.mcap.processor;

import javax.lang.model.type.TypeMirror;

public class ParsedFieldRef {
	private final TypeMirror targetType;
	private final String targetName;
	public ParsedFieldRef(TypeMirror targetType, String targetName) {
		this.targetType = targetType;
		this.targetName = targetName;
	}
	public TypeMirror getTargetType() {
		return targetType;
	}
	public String getTargetName() {
		return targetName;
	}
	public boolean inherit() {
		return targetName.equals("#$%INHERIT");
	}
}
