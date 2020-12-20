package io.github.nuclearfarts.mcap.annotation;

import io.github.nuclearfarts.mcap.TemplateType;

public @interface Template {
	String name();
	String file();
	TemplateType type();
}
