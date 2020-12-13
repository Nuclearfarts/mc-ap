package io.github.nuclearfarts.mcap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.nuclearfarts.mcap.ItemModelAction;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface RegisterItem {
	/**
	 * Item's string ID.
	 */
	String value();
	
	/**
	 * Model generation action
	 */
	ItemModelAction model() default ItemModelAction.INHERIT_MOD;
	
	/**
	 * Data for CUSTOM model action. First argument is json file (use ., not /), later args will be passed into String.format.
	 */
	String[] modelData() default { };
}
