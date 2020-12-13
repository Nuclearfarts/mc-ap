package io.github.nuclearfarts.mcap.annotation;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface FieldRef {
	Class<?> clazz();
	String field();
}
