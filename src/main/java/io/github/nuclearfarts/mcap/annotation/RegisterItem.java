package io.github.nuclearfarts.mcap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface RegisterItem {
	/**
	 * Item's string ID.
	 */
	String value();
	
	/**
	 * Model generation template.
	 * Builtins: basic
	 */
	String model() default "#$%INHERIT";
	String[] modelArgs() default {};
}
